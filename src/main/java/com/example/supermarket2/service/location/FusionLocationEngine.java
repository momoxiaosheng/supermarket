package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.BeaconData;
import com.example.supermarket2.dto.location.BeaconRequest;
import com.example.supermarket2.entity.location.*;
import com.example.supermarket2.utils.location.EmaFilter;
import com.example.supermarket2.utils.location.JumpLimiter;
import com.example.supermarket2.utils.location.MedianFilter;
import com.example.supermarket2.utils.location.PositionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 融合定位引擎
 * <p>
 * 核心定位算法实现，支持：
 * <ul>
 *   <li>动态信标列表：通过 {@link BeaconRegistryService} 加载，默认列表作为兜底</li>
 *   <li>WLS（加权最小二乘）4/5 点多点定位，带残差驱动的降级切换</li>
 *   <li>滤波链路：限幅滤波 → 中值滤波 → EMA 滤波</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class FusionLocationEngine {

    @Autowired
    private SignalProcessor signalProcessor;

    @Autowired
    private LocationQualityService qualityService;

    @Autowired
    private BeaconRegistryService beaconRegistry;

    @Autowired
    private BeaconConfig beaconConfig;

    // 滤波器（每个实例内部按 userId 维护独立状态）
    private JumpLimiter jumpLimiter;
    private MedianFilter medianFilter;
    private EmaFilter emaFilter;

    // 用户位置历史
    private final Map<String, List<Position>> userPositionHistory = new ConcurrentHashMap<>();
    private final Map<String, Position> userLastPositions = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_SIZE = 10;

    /** 默认距离估计值（当 RSSI 模型无法给出有效距离时使用，单位：米） */
    private static final double DEFAULT_DISTANCE_METERS = 5.0;

    /** WLS 矩阵奇异性判断阈值 */
    private static final double MATRIX_SINGULARITY_THRESHOLD = 1e-10;

    /**
     * 懒初始化滤波器（使用 BeaconConfig 中的参数）
     */
    private void ensureFiltersInitialized() {
        if (jumpLimiter == null) {
            jumpLimiter = new JumpLimiter(beaconConfig.getMaxJumpDistance());
        }
        if (medianFilter == null) {
            medianFilter = new MedianFilter(beaconConfig.getMedianWindowSize());
        }
        if (emaFilter == null) {
            emaFilter = new EmaFilter(beaconConfig.getEmaAlpha());
        }
    }

    /**
     * 计算位置 - 主要定位方法
     */
    public Position calculatePosition(List<BeaconData> beacons, BeaconRequest request) {
        try {
            ensureFiltersInitialized();

            log.debug("开始位置计算: userId={}, 有效信标数={}",
                    request.getUserId(), beacons.size());

            // 1. 评估信号质量
            SignalQuality signalQuality = qualityService.assessSignalQuality(beacons);

            // 2. 根据信号质量选择定位算法
            Position rawPosition = selectPositioningAlgorithm(beacons, signalQuality, request);

            if (rawPosition == null || !rawPosition.isValid()) {
                log.warn("原始位置计算失败，尝试使用历史位置");
                return getFallbackPosition(request.getUserId());
            }

            // 3. 应用滤波链路（限幅 → 中值 → EMA）
            Position filteredPosition = applyFilterPipeline(rawPosition, request.getUserId());

            // 4. 边界约束
            Position constrainedPosition = PositionUtils.constrainToBounds(
                    filteredPosition, 0, 50, 0, 50);

            // 5. 更新位置历史
            updatePositionHistory(request.getUserId(), constrainedPosition);

            log.debug("位置计算完成: userId={}, 原始位置=({}, {}), 滤波位置=({}, {})",
                    request.getUserId(), rawPosition.getX(), rawPosition.getY(),
                    constrainedPosition.getX(), constrainedPosition.getY());

            return constrainedPosition;

        } catch (Exception e) {
            log.error("位置计算异常: userId={}", request.getUserId(), e);
            return getFallbackPosition(request.getUserId());
        }
    }

    /**
     * 根据信号质量选择定位算法
     */
    private Position selectPositioningAlgorithm(List<BeaconData> beacons,
                                                SignalQuality signalQuality,
                                                BeaconRequest request) {
        if (signalQuality.isSuitableForTrilateration()) {
            log.debug("使用 WLS 多点定位算法: 高质量信号");
            Position wlsResult = calculateMultilaterationWLS(beacons);
            if (wlsResult != null) {
                return wlsResult;
            }
            log.debug("WLS 多点定位失败，降级到加权质心");
        }

        if (signalQuality.isSuitableForWeightedCentroid()) {
            log.debug("使用加权质心算法: 中等质量信号");
            return calculateWeightedCentroid(beacons);
        }

        // 低质量信号使用保守定位
        log.debug("使用保守定位算法: 低质量信号");
        return calculateConservativePosition(beacons, request);
    }

    // -------------------------------------------------------------------------
    // WLS 加权最小二乘多点定位（4/5 点，带残差驱动降级）
    // -------------------------------------------------------------------------

    /**
     * WLS 多点定位入口。
     * <ol>
     *   <li>先尝试用最多 {@code wlsMaxBeacons} 个信标计算</li>
     *   <li>若残差 > 阈值或最弱信标权重 &lt; weightThreshold，则剔除最差信标后
     *       用最多 {@code wlsMinBeacons} 个重算</li>
     * </ol>
     */
    private Position calculateMultilaterationWLS(List<BeaconData> beacons) {
        int maxBeacons = beaconConfig.getWlsMaxBeacons();
        int minBeacons = beaconConfig.getWlsMinBeacons();
        double residualThreshold = beaconConfig.getWlsResidualThreshold();
        double weightThresh = beaconConfig.getWeightThreshold();

        // 从注册表中选取 top maxBeacons 个有位置信息的信标
        List<BeaconData> candidates = beaconRegistry.selectTopBeaconsByQuality(beacons, maxBeacons);

        if (candidates.size() < minBeacons) {
            log.debug("WLS: 可用信标({})不足最低要求({})", candidates.size(), minBeacons);
            return null;
        }

        // 第一次尝试：使用全部候选信标（最多 maxBeacons 个）
        WlsResult result = solveWLS(candidates);

        if (result == null) {
            return null;
        }

        log.debug("WLS {}点结果: ({}, {}), 残差={}",
                candidates.size(), result.x, result.y, result.residual);

        // 判断是否需要降级到 minBeacons 点
        boolean residualTooHigh = result.residual > residualThreshold;
        boolean weakBeaconPresent = hasWeakBeacon(candidates, weightThresh);

        if (candidates.size() > minBeacons && (residualTooHigh || weakBeaconPresent)) {
            log.debug("WLS 降级触发: 残差过高={}, 弱信标存在={}", residualTooHigh, weakBeaconPresent);

            // 剔除距离最远（权重最低）的信标
            List<BeaconData> reducedCandidates = dropWorstBeacon(candidates);

            WlsResult reducedResult = solveWLS(reducedCandidates);
            if (reducedResult != null) {
                log.debug("WLS 降级到{}点结果: ({}, {}), 残差={}",
                        reducedCandidates.size(), reducedResult.x, reducedResult.y, reducedResult.residual);
                result = reducedResult;
            }
        }

        return new Position(result.x, result.y, "wls_multilateration", "high");
    }

    /**
     * 求解 WLS 线性方程组，返回估计位置和残差。
     * <p>
     * 以最后一个信标为参考，构造线性方程组 A·p = b，
     * 权重 wi = 1 / di²（距离越近权重越高）。
     * 求解：p = (Aᵀ W A)⁻¹ Aᵀ W b
     * </p>
     *
     * @param beaconList 信标列表（已按 RSSI 降序排序）
     * @return WLS 结果（含 x, y, residual），失败返回 null
     */
    private WlsResult solveWLS(List<BeaconData> beaconList) {
        int n = beaconList.size();
        if (n < 2) return null;

        // 收集信标位置和距离
        double[] bx = new double[n];
        double[] by = new double[n];
        double[] dist = new double[n];
        double[] weight = new double[n];

        for (int i = 0; i < n; i++) {
            BeaconData bd = beaconList.get(i);
            BeaconLocation loc = beaconRegistry.findBeaconLocation(bd.getUuid());
            if (loc == null) return null;

            bx[i] = loc.getX();
            by[i] = loc.getY();
            Double d = loc.calculateDistance(bd.getRssi());
            dist[i] = (d != null && d > 0) ? d : DEFAULT_DISTANCE_METERS;

            // 权重 = 1 / d²
            weight[i] = 1.0 / (dist[i] * dist[i]);
        }

        // 使用最后一个信标作为参考（消去非线性项）
        int ref = n - 1;
        int rows = n - 1;

        // 构造矩阵 A（rows × 2）和向量 b（rows × 1）
        double[][] A = new double[rows][2];
        double[] b = new double[rows];

        for (int i = 0; i < rows; i++) {
            A[i][0] = 2.0 * (bx[ref] - bx[i]);
            A[i][1] = 2.0 * (by[ref] - by[i]);
            b[i] = dist[i] * dist[i] - dist[ref] * dist[ref]
                    - bx[i] * bx[i] + bx[ref] * bx[ref]
                    - by[i] * by[i] + by[ref] * by[ref];
        }

        // 行权重：取该行两个信标权重的调和平均
        double[] w = new double[rows];
        for (int i = 0; i < rows; i++) {
            w[i] = 2.0 / (1.0 / weight[i] + 1.0 / weight[ref]);
        }

        // 计算 Aᵀ W A（2×2）和 Aᵀ W b（2×1）
        double[][] AtWA = new double[2][2];
        double[] AtWb = new double[2];

        for (int i = 0; i < rows; i++) {
            AtWA[0][0] += w[i] * A[i][0] * A[i][0];
            AtWA[0][1] += w[i] * A[i][0] * A[i][1];
            AtWA[1][0] += w[i] * A[i][1] * A[i][0];
            AtWA[1][1] += w[i] * A[i][1] * A[i][1];
            AtWb[0] += w[i] * A[i][0] * b[i];
            AtWb[1] += w[i] * A[i][1] * b[i];
        }

        // 求 2×2 逆矩阵
        double det = AtWA[0][0] * AtWA[1][1] - AtWA[0][1] * AtWA[1][0];
        if (Math.abs(det) < MATRIX_SINGULARITY_THRESHOLD) {
            log.warn("WLS: 矩阵奇异，无法求解");
            return null;
        }

        double invDet = 1.0 / det;
        double solX = invDet * (AtWA[1][1] * AtWb[0] - AtWA[0][1] * AtWb[1]);
        double solY = invDet * (AtWA[0][0] * AtWb[1] - AtWA[1][0] * AtWb[0]);

        // 计算加权残差：sum_i( wi * (||p - bi|| - di)² )
        double residual = 0;
        for (int i = 0; i < n; i++) {
            double dx = solX - bx[i];
            double dy = solY - by[i];
            double estDist = Math.sqrt(dx * dx + dy * dy);
            double diff = estDist - dist[i];
            residual += weight[i] * diff * diff;
        }

        return new WlsResult(solX, solY, residual);
    }

    /**
     * 判断候选信标列表中是否存在权重低于阈值的"弱信标"。
     */
    private boolean hasWeakBeacon(List<BeaconData> candidates, double weightThresh) {
        for (BeaconData bd : candidates) {
            BeaconLocation loc = beaconRegistry.findBeaconLocation(bd.getUuid());
            if (loc == null) continue;
            Double d = loc.calculateDistance(bd.getRssi());
            double distance = (d != null && d > 0) ? d : DEFAULT_DISTANCE_METERS;
            double w = 1.0 / (distance * distance);
            if (w < weightThresh) {
                return true;
            }
        }
        return false;
    }

    /**
     * 剔除距离最远（权重最低）的信标，返回缩减后的列表。
     */
    private List<BeaconData> dropWorstBeacon(List<BeaconData> candidates) {
        int worstIdx = -1;
        double maxDist = -1;

        for (int i = 0; i < candidates.size(); i++) {
            BeaconData bd = candidates.get(i);
            BeaconLocation loc = beaconRegistry.findBeaconLocation(bd.getUuid());
            if (loc == null) continue;
            Double d = loc.calculateDistance(bd.getRssi());
            double distance = (d != null && d > 0) ? d : DEFAULT_DISTANCE_METERS;
            if (distance > maxDist) {
                maxDist = distance;
                worstIdx = i;
            }
        }

        List<BeaconData> reduced = new ArrayList<>(candidates);
        if (worstIdx >= 0) {
            reduced.remove(worstIdx);
        }
        return reduced;
    }

    /** WLS 求解结果（内部数据类） */
    private static class WlsResult {
        final double x;
        final double y;
        final double residual;

        WlsResult(double x, double y, double residual) {
            this.x = x;
            this.y = y;
            this.residual = residual;
        }
    }

    // -------------------------------------------------------------------------
    // 其他定位算法（降级使用）
    // -------------------------------------------------------------------------

    /**
     * 加权质心定位算法
     */
    private Position calculateWeightedCentroid(List<BeaconData> beacons) {
        List<Position> beaconPositions = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        for (BeaconData beacon : beacons) {
            BeaconLocation beaconLoc = beaconRegistry.findBeaconLocation(beacon.getUuid());
            if (beaconLoc != null) {
                beaconPositions.add(new Position(beaconLoc.getX(), beaconLoc.getY()));

                // 根据RSSI计算权重（信号越强权重越高）
                double weight = Math.pow(10, beacon.getRssi() / 20.0);
                weights.add(weight);
            }
        }

        if (beaconPositions.isEmpty()) {
            return null;
        }

        Position centroid = PositionUtils.calculateWeightedCentroid(beaconPositions, weights);
        if (centroid != null) {
            centroid.setAlgorithm("weighted_centroid");
            centroid.setQuality("calculated");
        }

        return centroid;
    }

    /**
     * 保守定位算法
     */
    private Position calculateConservativePosition(List<BeaconData> beacons, BeaconRequest request) {
        Position basePosition = calculateWeightedCentroid(beacons);

        if (basePosition == null) {
            return getFallbackPosition(request.getUserId());
        }

        Position lastPosition = userLastPositions.get(request.getUserId());
        if (lastPosition == null) {
            return basePosition;
        }

        double distance = PositionUtils.calculateDistance(basePosition, lastPosition);
        double maxMove = 3.0; // 最大移动3米

        if (distance > maxMove) {
            double ratio = maxMove / distance;
            double newX = lastPosition.getX() + (basePosition.getX() - lastPosition.getX()) * ratio;
            double newY = lastPosition.getY() + (basePosition.getY() - lastPosition.getY()) * ratio;

            Position constrained = new Position(newX, newY);
            constrained.setAlgorithm("conservative");
            constrained.setQuality("constrained");

            log.debug("保守定位: 限制移动距离 {} -> {}", distance, maxMove);
            return constrained;
        }

        basePosition.setAlgorithm("conservative");
        basePosition.setQuality("normal");
        return basePosition;
    }

    // -------------------------------------------------------------------------
    // 滤波链路：限幅 → 中值 → EMA
    // -------------------------------------------------------------------------

    /**
     * 对原始定位结果依次应用三级滤波：
     * <ol>
     *   <li>限幅滤波（JumpLimiter）：消除大跳变</li>
     *   <li>中值滤波（MedianFilter）：消除脉冲噪声</li>
     *   <li>EMA 滤波（EmaFilter）：平滑小抖动</li>
     * </ol>
     */
    private Position applyFilterPipeline(Position rawPosition, String userId) {
        try {
            // Step 1: 限幅滤波
            Position jumpLimited = jumpLimiter.apply(userId, rawPosition);

            // Step 2: 中值滤波
            Position medianFiltered = medianFilter.apply(userId, jumpLimited);

            // Step 3: EMA 平滑
            Position emaSmoothed = emaFilter.apply(userId, medianFiltered);

            if (emaSmoothed != null && emaSmoothed.isValid()) {
                return emaSmoothed;
            }
        } catch (Exception e) {
            log.warn("滤波链路异常", e);
        }
        return rawPosition;
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    /**
     * 获取备用位置（历史位置或默认位置）
     */
    private Position getFallbackPosition(String userId) {
        Position lastPosition = userLastPositions.get(userId);
        if (lastPosition != null) {
            lastPosition.setAlgorithm("fallback");
            lastPosition.setQuality("historical");
            return lastPosition;
        }

        // 返回地图中心作为默认位置
        return new Position(25.0, 25.0, "default", "estimated");
    }

    /**
     * 更新位置历史
     */
    private void updatePositionHistory(String userId, Position position) {
        userLastPositions.put(userId, position);

        List<Position> history = userPositionHistory.getOrDefault(userId, new ArrayList<>());
        history.add(position);

        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
        }

        userPositionHistory.put(userId, history);
    }

    /**
     * 获取用户上次已知位置
     */
    public Position getLastKnownPosition(String userId) {
        return userLastPositions.get(userId);
    }

    /**
     * 获取用户位置历史
     */
    public List<Position> getPositionHistory(String userId) {
        return userPositionHistory.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 清除用户位置数据（用户退出时调用）
     */
    public void clearUserData(String userId) {
        userLastPositions.remove(userId);
        userPositionHistory.remove(userId);

        // 同步重置滤波器中的用户状态
        if (jumpLimiter != null) jumpLimiter.reset(userId);
        if (medianFilter != null) medianFilter.reset(userId);
        if (emaFilter != null) emaFilter.reset(userId);

        log.info("清除用户位置数据: userId={}", userId);
    }
}
