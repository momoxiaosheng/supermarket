package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.BeaconData;
import com.example.supermarket2.entity.location.BeaconConfig;
import com.example.supermarket2.entity.location.RssiHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 信号预处理服务
 * 对原始蓝牙信标数据进行预处理和滤波
 */
@Slf4j
@Service
public class SignalProcessor {

    @Autowired
    private BeaconConfig beaconConfig;

    // RSSI历史记录，用于信号分析和滤波
    private final Map<String, List<RssiHistory>> rssiHistoryMap = new ConcurrentHashMap<>();
    private final int MAX_HISTORY_SIZE = 10;

    /**
     * 预处理信标数据
     */
    public List<BeaconData> preProcessBeacons(List<BeaconData> rawBeacons) {
        if (rawBeacons == null || rawBeacons.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("开始信号预处理: 原始信标数量={}", rawBeacons.size());

        List<BeaconData> processedBeacons = rawBeacons.stream()
                .filter(this::isValidBeacon) // 基础验证
                .map(this::clampRssi) // RSSI限幅
                .map(this::updateHistory) // 更新历史记录
                .filter(this::filterOutliers) // 异常值过滤
                .sorted(Comparator.comparing(BeaconData::getRssi).reversed()) // 按信号强度排序
                .collect(Collectors.toList());

        log.debug("信号预处理完成: 有效信标数量={}", processedBeacons.size());
        return processedBeacons;
    }

    /**
     * 验证信标数据有效性
     */
    private boolean isValidBeacon(BeaconData beacon) {
        if (beacon == null || beacon.getUuid() == null || beacon.getUuid().trim().isEmpty()) {
            return false;
        }

        if (beacon.getRssi() == null || beacon.getRssi() >= 0) {
            return false;
        }

        // 检查UUID格式
        String uuid = beacon.getUuid().toLowerCase().replace("-", "");
        if (uuid.length() != 32) { // 标准的UUID长度（去除横杠）
            log.warn("无效的信标UUID格式: {}", beacon.getUuid());
            return false;
        }

        return true;
    }

    /**
     * RSSI限幅处理
     */
    private BeaconData clampRssi(BeaconData beacon) {
        Integer clampedRssi = beaconConfig.clampRssi(beacon.getRssi());

        if (!clampedRssi.equals(beacon.getRssi())) {
            log.debug("RSSI限幅: {} -> {}", beacon.getRssi(), clampedRssi);
            beacon.setRssi(clampedRssi);
        }

        return beacon;
    }

    /**
     * 更新历史记录并检测异常值
     */
    private BeaconData updateHistory(BeaconData beacon) {
        String beaconKey = beacon.getNormalizedUuid();
        List<RssiHistory> history = rssiHistoryMap.getOrDefault(beaconKey, new ArrayList<>());

        // 创建新的历史记录
        RssiHistory newRecord = new RssiHistory();
        newRecord.setRssi(beacon.getRssi());
        newRecord.setTimestamp(System.currentTimeMillis());
        newRecord.setBeaconId(beaconKey);  // 注意：类中使用的是 beaconId，不是 beaconKey
        newRecord.setIsValid(true);

        // 检测异常值
        if (isOutlier(newRecord, history)) {
            log.debug("检测到异常值: beacon={}, rssi={}", beaconKey.substring(0, 8), beacon.getRssi());
            beacon.setIsValid(false);
            return beacon;
        }

        // 更新历史记录
        history.add(newRecord);
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(history.size() - MAX_HISTORY_SIZE, history.size());
        }
        rssiHistoryMap.put(beaconKey, history);

        beacon.setIsValid(true);
        return beacon;
    }

    /**
     * 异常值过滤
     */
    private boolean filterOutliers(BeaconData beacon) {
        if (!beacon.getIsValid()) {
            return false;
        }

        String beaconKey = beacon.getNormalizedUuid();
        List<RssiHistory> history = rssiHistoryMap.get(beaconKey);

        if (history == null || history.size() < 3) {
            return true; // 历史数据不足，不过滤
        }

        // 检查信号稳定性
        return isSignalStable(history, beacon.getRssi());
    }

    /**
     * 检测异常值（基于中位数绝对偏差）
     */
    private boolean isOutlier(RssiHistory newRecord, List<RssiHistory> history) {
        if (history.size() < 3) {
            return false; // 数据不足，不判定为异常
        }

        // 创建包含新记录的合并列表进行检测
        List<RssiHistory> combinedHistory = new ArrayList<>(history);
        combinedHistory.add(newRecord);

        List<RssiHistory> outliers = RssiHistory.detectOutliers(combinedHistory, beaconConfig.getOutlierThreshold());

        // 检查新记录是否在异常值列表中
        return outliers.stream()
                .anyMatch(outlier ->
                        outlier.getRssi().equals(newRecord.getRssi()) &&
                                outlier.getTimestamp().equals(newRecord.getTimestamp()));
    }

    /**
     * 检查信号稳定性
     */
    private boolean isSignalStable(List<RssiHistory> history, int currentRssi) {
        if (history.size() < 2) {
            return true;
        }

        // 计算最近信号的稳定性
        List<RssiHistory> recent = history.subList(
                Math.max(0, history.size() - 3), history.size());

        double stability = RssiHistory.calculateStability(recent);

        // 检查当前值与历史平均值的差异
        double recentAvg = recent.stream()
                .mapToInt(RssiHistory::getRssi)
                .average()
                .orElse(currentRssi);

        double diff = Math.abs(currentRssi - recentAvg);
        boolean isStable = stability >= beaconConfig.getStabilityThreshold() && diff < 15;

        log.debug("信号稳定性检查: beacon={}, 稳定性={}, 差异={}, 稳定={}",
                history.get(0).getBeaconId().substring(0, 8),
                stability, diff, isStable);

        return isStable;
    }

    /**
     * 计算信标权重
     */
    public double calculateBeaconWeight(BeaconData beacon) {
        if (beacon == null || !beacon.getIsValid()) {
            return 0.0;
        }

        String beaconKey = beacon.getNormalizedUuid();
        List<RssiHistory> history = rssiHistoryMap.get(beaconKey);

        double baseWeight = beaconConfig.calculateWeight(beacon.getRssi());

        // 根据稳定性调整权重
        if (history != null && history.size() >= 2) {
            double stability = RssiHistory.calculateStability(history);
            baseWeight *= stability;
        }

        return baseWeight;
    }

    /**
     * 获取信号质量评分
     */
    public double calculateSignalQualityScore(BeaconData beacon) {
        if (beacon == null || !beacon.getIsValid()) {
            return 0.0;
        }

        double score = 0.0;

        // RSSI强度评分
        if (beacon.getRssi() > -60) score += 0.4;
        else if (beacon.getRssi() > -70) score += 0.3;
        else if (beacon.getRssi() > -80) score += 0.2;
        else score += 0.1;

        // 稳定性评分（如果有历史数据）
        String beaconKey = beacon.getNormalizedUuid();
        List<RssiHistory> history = rssiHistoryMap.get(beaconKey);
        if (history != null && history.size() >= 3) {
            double stability = RssiHistory.calculateStability(history);
            score += stability * 0.3;
        } else {
            score += 0.15; // 默认稳定性分数
        }

        return Math.min(1.0, score);
    }

    /**
     * 清理过期的历史记录
     */
    public void cleanupExpiredHistory(long expireTimeMillis) {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        for (String beaconKey : rssiHistoryMap.keySet()) {
            List<RssiHistory> history = rssiHistoryMap.get(beaconKey);
            if (history != null) {
                List<RssiHistory> validHistory = history.stream()
                        .filter(record -> currentTime - record.getTimestamp() <= expireTimeMillis)
                        .collect(Collectors.toList());

                if (validHistory.size() != history.size()) {
                    rssiHistoryMap.put(beaconKey, validHistory);
                    removedCount += (history.size() - validHistory.size());
                }
            }
        }

        if (removedCount > 0) {
            log.info("清理过期历史记录: 移除{}条记录", removedCount);
        }
    }

    /**
     * 获取信号处理统计信息
     */
    public SignalProcessingStats getStats() {
        int totalBeacons = rssiHistoryMap.values().stream()
                .mapToInt(List::size)
                .sum();

        int uniqueBeacons = rssiHistoryMap.size();

        return new SignalProcessingStats(totalBeacons, uniqueBeacons);
    }

    /**
     * 信号处理统计类
     */
    public static class SignalProcessingStats {
        private final int totalHistoryRecords;
        private final int uniqueBeacons;

        public SignalProcessingStats(int totalHistoryRecords, int uniqueBeacons) {
            this.totalHistoryRecords = totalHistoryRecords;
            this.uniqueBeacons = uniqueBeacons;
        }

        public int getTotalHistoryRecords() { return totalHistoryRecords; }
        public int getUniqueBeacons() { return uniqueBeacons; }
    }
}

