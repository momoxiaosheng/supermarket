package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.BeaconData;
import com.example.supermarket2.dto.location.BeaconRequest;
import com.example.supermarket2.entity.location.*;
import com.example.supermarket2.utils.location.KalmanFilter2D;
import com.example.supermarket2.utils.location.PositionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 融合定位引擎
 * 核心定位算法实现，移除指纹算法，增强加权质心定位
 */
@Slf4j
@Component
public class FusionLocationEngine {

    @Autowired
    private SignalProcessor signalProcessor;

    @Autowired
    private LocationQualityService qualityService;

    @Autowired
    private KalmanFilter2D kalmanFilter;

    // 信标位置配置（应从数据库或配置文件中读取）
    private final List<BeaconLocation> beaconLocations = Arrays.asList(
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF1", 1.0, 1.0),
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF2", 28.0, 1.0),
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF3", 28.0, 48.0),
            new BeaconLocation("01122334-4556-6778-899A-ABBCCDDEEFF4", 1.0, 48.0)
    );

    // 用户位置历史
    private final Map<String, List<Position>> userPositionHistory = new ConcurrentHashMap<>();
    private final Map<String, Position> userLastPositions = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_SIZE = 10;

    /**
     * 计算位置 - 主要定位方法
     */
    public Position calculatePosition(List<BeaconData> beacons, BeaconRequest request) {
        try {
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

            // 3. 应用卡尔曼滤波
            Position filteredPosition = applyKalmanFilter(rawPosition, request.getUserId(),
                    signalQuality.getConfidence());

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
        // 根据信号质量选择算法
        if (signalQuality.isSuitableForTrilateration()) {
            log.debug("使用三角定位算法: 高质量信号");
            Position trilaterationResult = calculateTrilateration(beacons);
            if (trilaterationResult != null) {
                return trilaterationResult;
            }
            // 三角定位失败，降级到加权质心
            log.debug("三角定位失败，降级到加权质心");
        }

        if (signalQuality.isSuitableForWeightedCentroid()) {
            log.debug("使用加权质心算法: 中等质量信号");
            return calculateWeightedCentroid(beacons);
        }

        // 低质量信号使用保守定位
        log.debug("使用保守定位算法: 低质量信号");
        return calculateConservativePosition(beacons, request);
    }

    /**
     * 加权质心定位算法
     */
    private Position calculateWeightedCentroid(List<BeaconData> beacons) {
        List<Position> beaconPositions = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        for (BeaconData beacon : beacons) {
            BeaconLocation beaconLoc = findBeaconLocation(beacon.getUuid());
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
     * 三角定位算法
     */
    private Position calculateTrilateration(List<BeaconData> beacons) {
        try {
            if (beacons.size() < 3) {
                return null;
            }

            // 取前3个最强信号的信标
            List<BeaconData> topBeacons = beacons.subList(0, Math.min(3, beacons.size()));

            List<Position> positions = new ArrayList<>();
            List<Double> distances = new ArrayList<>();

            for (BeaconData beacon : topBeacons) {
                BeaconLocation beaconLoc = findBeaconLocation(beacon.getUuid());
                if (beaconLoc != null) {
                    positions.add(new Position(beaconLoc.getX(), beaconLoc.getY()));

                    // 根据RSSI估算距离
                    Double distance = beaconLoc.calculateDistance(beacon.getRssi());
                    distances.add(distance != null ? distance : 5.0); // 默认5米 // 默认5米
                }
            }

            if (positions.size() < 3) {
                return null;
            }

            // 简化三角定位：计算距离加权中心
            return calculateDistanceWeightedCenter(positions, distances);

        } catch (Exception e) {
            log.warn("三角定位计算异常", e);
            return null;
        }
    }

    /**
     * 计算距离加权中心（简化三角定位）
     */
    private Position calculateDistanceWeightedCenter(List<Position> positions, List<Double> distances) {
        double sumX = 0, sumY = 0, totalWeight = 0;

        for (int i = 0; i < positions.size(); i++) {
            Position pos = positions.get(i);
            Double distance = distances.get(i);

            if (pos != null && distance != null && distance > 0) {
                // 距离越近，权重越高
                double weight = 1.0 / distance;
                sumX += pos.getX() * weight;
                sumY += pos.getY() * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) {
            return null;
        }

        Position result = new Position(sumX / totalWeight, sumY / totalWeight);
        result.setAlgorithm("trilateration");
        result.setQuality("high");
        return result;
    }

    /**
     * 保守定位算法
     */
    private Position calculateConservativePosition(List<BeaconData> beacons, BeaconRequest request) {
        // 使用加权质心作为基础
        Position basePosition = calculateWeightedCentroid(beacons);

        if (basePosition == null) {
            return getFallbackPosition(request.getUserId());
        }

        // 获取上次位置
        Position lastPosition = userLastPositions.get(request.getUserId());
        if (lastPosition == null) {
            return basePosition;
        }

        // 限制移动距离（保守策略）
        double distance = PositionUtils.calculateDistance(basePosition, lastPosition);
        double maxMove = 3.0; // 最大移动3米

        if (distance > maxMove) {
            // 限制移动距离
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

    /**
     * 应用卡尔曼滤波
     */
    private Position applyKalmanFilter(Position position, String userId, Double qualityFactor) {
        try {
            long timestamp = System.currentTimeMillis();

            if (!kalmanFilter.isInitialized()) {
                kalmanFilter.initialize(position.getX(), position.getY(), timestamp);
                return position;
            }

            Position filtered = kalmanFilter.filter(
                    position.getX(), position.getY(), timestamp, qualityFactor);

            if (filtered != null && filtered.isValid()) {
                filtered.setAlgorithm(position.getAlgorithm() + "_filtered");
                filtered.setQuality("filtered");
                return filtered;
            }

        } catch (Exception e) {
            log.warn("卡尔曼滤波异常", e);
        }

        return position;
    }

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
     * 查找信标位置
     */
    private BeaconLocation findBeaconLocation(String uuid) {
        if (uuid == null) return null;

        String normalizedUUID = uuid.toLowerCase().replace("-", "");
        return beaconLocations.stream()
                .filter(beacon -> beacon.getNormalizedUuid().equals(normalizedUUID))
                .findFirst()
                .orElse(null);
    }

    /**
     * 更新位置历史
     */
    private void updatePositionHistory(String userId, Position position) {
        userLastPositions.put(userId, position);

        List<Position> history = userPositionHistory.getOrDefault(userId, new ArrayList<>());
        history.add(position);

        // 限制历史记录大小
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
     * 清除用户位置数据
     */
    public void clearUserData(String userId) {
        userLastPositions.remove(userId);
        userPositionHistory.remove(userId);
        log.info("清除用户位置数据: userId={}", userId);
    }
}

