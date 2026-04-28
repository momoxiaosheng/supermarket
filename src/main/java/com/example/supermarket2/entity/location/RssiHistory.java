package com.example.supermarket2.entity.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * RSSI信号历史记录
 * 存储信号值和时间戳，用于信号分析和滤波
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RssiHistory {
    private Integer rssi;           // RSSI信号值
    private Long timestamp;         // 时间戳
    private String beaconId;        // 信标ID
    private Boolean isValid = true; // 是否有效（用于野值检测）

    public RssiHistory(Integer rssi, Long timestamp) {
        this.rssi = rssi;
        this.timestamp = timestamp;
    }

    /**
     * 从历史数据列表中计算移动平均
     */
    public static Double calculateMovingAverage(List<RssiHistory> history, int windowSize) {
        if (history == null || history.isEmpty()) return null;

        int size = Math.min(windowSize, history.size());
        List<RssiHistory> recent = history.subList(history.size() - size, history.size());

        return recent.stream()
                .filter(RssiHistory::getIsValid)
                .mapToInt(RssiHistory::getRssi)
                .average()
                .orElse(Double.NaN);
    }

    /**
     * 从历史数据中检测野值
     */
    public static List<RssiHistory> detectOutliers(List<RssiHistory> history, double threshold) {
        if (history == null || history.size() < 3) {
            return new ArrayList<>();
        }

        List<RssiHistory> outliers = new ArrayList<>();
        List<Integer> values = history.stream()
                .map(RssiHistory::getRssi)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // 计算中位数
        values.sort(Integer::compareTo);
        int median = values.get(values.size() / 2);

        // 计算中位数绝对偏差
        double mad = values.stream()
                .mapToInt(v -> Math.abs(v - median))
                .average()
                .orElse(0);

        // 检测野值
        for (RssiHistory record : history) {
            if (Math.abs(record.getRssi() - median) > threshold * mad) {
                outliers.add(record);
            }
        }

        return outliers;
    }

    /**
     * 计算信号稳定性（标准差倒数）
     */
    public static Double calculateStability(List<RssiHistory> history) {
        if (history == null || history.size() < 2) return 1.0;

        double mean = history.stream()
                .filter(RssiHistory::getIsValid)
                .mapToInt(RssiHistory::getRssi)
                .average()
                .orElse(0);

        double variance = history.stream()
                .filter(RssiHistory::getIsValid)
                .mapToDouble(h -> Math.pow(h.getRssi() - mean, 2))
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);

        // 标准差越小，稳定性越高
        return 1.0 / (1.0 + stdDev);
    }
}

