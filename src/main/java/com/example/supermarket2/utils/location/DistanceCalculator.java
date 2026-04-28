package com.example.supermarket2.utils.location;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 距离计算工具
 * 提供基于RSSI的距离计算和相关工具方法
 */
@Slf4j
public class DistanceCalculator {

    private DistanceCalculator() {
        // 工具类，防止实例化
    }

    /**
     * 根据RSSI计算距离（对数路径损耗模型）
     */
    public static double calculateDistance(double rssi, double rssi0, double pathLossExponent) {
        if (rssi >= 0) {
            log.warn("无效的RSSI值: {}", rssi);
            return Double.MAX_VALUE;
        }

        try {
            // 使用对数路径损耗模型: RSSI = RSSI0 - 10 * n * log10(d/d0)
            // 其中d0=1米，所以简化为: d = 10^((RSSI0 - RSSI) / (10 * n))
            return Math.pow(10, (rssi0 - rssi) / (10 * pathLossExponent));
        } catch (Exception e) {
            log.error("距离计算失败: RSSI={}, RSSI0={}, n={}", rssi, rssi0, pathLossExponent, e);
            return Double.MAX_VALUE;
        }
    }

    /**
     * 计算三角定位的权重距离
     */
    public static double calculateWeightedDistance(double measuredRssi, double expectedRssi,
                                                   double maxDistance, double weightFactor) {
        double distance = calculateDistance(measuredRssi, expectedRssi, weightFactor);
        return Math.min(distance, maxDistance);
    }

    /**
     * 计算两点之间的距离（欧几里得距离）
     */
    public static double calculateEuclideanDistance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算信标信号覆盖范围
     */
    public static double calculateCoverageRadius(double txPower, double sensitivity, double pathLossExponent) {
        // 根据发射功率和接收灵敏度计算最大覆盖范围
        return calculateDistance(sensitivity, txPower, pathLossExponent);
    }

    /**
     * 距离有效性检查
     */
    public static boolean isValidDistance(double distance, double minDistance, double maxDistance) {
        return !Double.isNaN(distance) &&
                !Double.isInfinite(distance) &&
                distance >= minDistance &&
                distance <= maxDistance;
    }

    /**
     * 计算定位误差圆半径（CEP）
     */
    public static double calculateCircularErrorProbability(List<Double> errors, double probability) {
        if (errors == null || errors.isEmpty()) {
            return 0.0;
        }

        // 对误差进行排序
        List<Double> sortedErrors = new ArrayList<>(errors);
        sortedErrors.sort(Double::compareTo);

        // 计算指定概率对应的误差值
        int index = (int) (sortedErrors.size() * probability);
        index = Math.min(index, sortedErrors.size() - 1);

        return sortedErrors.get(index);
    }

    /**
     * 计算信标部署密度
     */
    public static double calculateBeaconDensity(int beaconCount, double area) {
        if (area <= 0) {
            return 0.0;
        }
        return beaconCount / area;
    }

    /**
     * 估算定位精度
     */
    public static double estimatePositioningAccuracy(int beaconCount, double avgRssi, double coverageDensity) {
        double accuracy = 5.0; // 基础精度（米）

        // 信标数量影响
        if (beaconCount >= 4) {
            accuracy *= 0.6;
        } else if (beaconCount >= 3) {
            accuracy *= 0.8;
        } else if (beaconCount >= 2) {
            accuracy *= 1.0;
        } else {
            accuracy *= 2.0;
        }

        // 信号强度影响
        if (avgRssi > -60) {
            accuracy *= 0.7;
        } else if (avgRssi > -70) {
            accuracy *= 0.9;
        } else if (avgRssi > -80) {
            accuracy *= 1.2;
        } else {
            accuracy *= 2.0;
        }

        // 部署密度影响
        accuracy *= (1.0 / coverageDensity);

        return Math.max(0.5, accuracy); // 最小精度0.5米
    }
}

