package com.example.supermarket2.utils.location;

import com.example.supermarket2.entity.location.RssiHistory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RSSI信号处理工具
 * 提供RSSI信号的预处理、滤波和分析功能
 */
@Slf4j
public class RssiProcessor {

    private RssiProcessor() {
        // 工具类，防止实例化
    }

    /**
     * 限幅处理RSSI值
     */
    public static int clampRssi(Integer rssi, int min, int max) {
        if (rssi == null) {
            return min;
        }
        return Math.max(min, Math.min(max, rssi));
    }

    /**
     * 过滤有效的信标数据
     */
    public static <T> List<T> filterValidBeacons(List<T> beacons, RssiValidator<T> validator) {
        if (beacons == null) {
            return new ArrayList<>();
        }

        return beacons.stream()
                .filter(validator::isValid)
                .sorted(validator.getComparator())
                .collect(Collectors.toList());
    }

    /**
     * 计算RSSI移动平均
     */
    public static double calculateMovingAverage(List<Integer> rssiValues, int windowSize) {
        if (rssiValues == null || rssiValues.isEmpty()) {
            return Double.NaN;
        }

        int size = Math.min(windowSize, rssiValues.size());
        List<Integer> recent = rssiValues.subList(rssiValues.size() - size, rssiValues.size());

        return recent.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(Double.NaN);
    }

    /**
     * 检测RSSI异常值（基于中位数绝对偏差）
     */
    public static List<Integer> detectOutliers(List<Integer> rssiValues, double threshold) {
        if (rssiValues == null || rssiValues.size() < 3) {
            return new ArrayList<>();
        }

        List<Integer> sorted = new ArrayList<>(rssiValues);
        sorted.sort(Integer::compareTo);

        // 计算中位数
        int median = sorted.get(sorted.size() / 2);

        // 计算中位数绝对偏差
        List<Integer> deviations = sorted.stream()
                .map(rssi -> Math.abs(rssi - median))
                .collect(Collectors.toList());

        deviations.sort(Integer::compareTo);
        int mad = deviations.get(deviations.size() / 2);

        // 检测异常值
        return rssiValues.stream()
                .filter(rssi -> Math.abs(rssi - median) > threshold * mad)
                .collect(Collectors.toList());
    }

    /**
     * 计算信号稳定性
     */
    public static double calculateStability(List<Integer> rssiValues) {
        if (rssiValues == null || rssiValues.size() < 2) {
            return 1.0; // 单个值认为稳定
        }

        double mean = rssiValues.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        double variance = rssiValues.stream()
                .mapToDouble(rssi -> Math.pow(rssi - mean, 2))
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);

        // 标准差越小，稳定性越高
        // 使用sigmoid函数将稳定性映射到0-1范围
        return 1.0 / (1.0 + stdDev / 10.0);
    }

    /**
     * 根据RSSI计算权重
     */
    public static double calculateWeight(Integer rssi, double exponent) {
        if (rssi == null || rssi >= 0) {
            return 0.0;
        }

        // RSSI越强（值越大），权重越高
        return Math.pow(10, rssi * exponent / 10.0);
    }

    /**
     * 计算RSSI变化趋势
     */
    public static double calculateTrend(List<RssiHistory> history) {
        if (history == null || history.size() < 3) {
            return 0.0;
        }

        // 使用简单线性回归计算趋势
        List<RssiHistory> sorted = history.stream()
                .sorted(Comparator.comparing(RssiHistory::getTimestamp))
                .collect(Collectors.toList());

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = sorted.size();

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = sorted.get(i).getRssi();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);

        // 返回趋势斜率（每时间单位的RSSI变化）
        return slope;
    }

    /**
     * 信号质量评估
     */
    public static String assessSignalQuality(double avgRssi, int beaconCount, double stability) {
        if (beaconCount >= 3 && avgRssi > -65 && stability > 0.7) {
            return "HIGH";
        } else if (beaconCount >= 2 && avgRssi > -70 && stability > 0.5) {
            return "MEDIUM";
        } else if (beaconCount >= 1 && avgRssi > -75) {
            return "LOW";
        } else {
            return "POOR";
        }
    }

    /**
     * RSSI验证器接口
     */
    public interface RssiValidator<T> {
        boolean isValid(T beacon);
        Comparator<T> getComparator();
    }

    /**
     * 默认RSSI验证器
     */
    public static class DefaultRssiValidator implements RssiValidator<Object> {
        private final int minRssi;
        private final int maxRssi;

        public DefaultRssiValidator(int minRssi, int maxRssi) {
            this.minRssi = minRssi;
            this.maxRssi = maxRssi;
        }

        @Override
        public boolean isValid(Object beacon) {
            // 需要在具体实现中获取RSSI值
            // 这里只是示例接口
            return true;
        }

        @Override
        public Comparator<Object> getComparator() {
            // 按RSSI降序排序
            return (b1, b2) -> 0; // 具体实现需要比较RSSI
        }
    }
}