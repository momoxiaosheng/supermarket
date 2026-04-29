package com.example.supermarket2.entity.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信号质量评估实体
 * 存储信号等级、稳定性等信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignalQuality {
    public enum Level {
        HIGH,   // 高质量信号
        MEDIUM, // 中等质量信号
        LOW,    // 低质量信号
        POOR    // 差质量信号
    }

    private Level level;            // 信号等级
    private Double avgRSSI;        // 平均RSSI值
    private Double stability;      // 信号稳定性（0-1）
    private Integer validBeacons;  // 有效信标数量
    private Double confidence;     // 置信度（0-1）
    private Double variance;       // 信号方差

    /**
     * 根据信号参数评估信号质量
     */
    public static SignalQuality assess(int beaconCount, double avgRSSI, double stability) {
        Level level;
        double confidence;

        if (beaconCount >= 4 && avgRSSI > -70 && stability > 0.6) {
            // 4个以上信标时，放宽RSSI和稳定性门槛，以支持WLS多点定位
            level = Level.HIGH;
            confidence = 0.85;
        } else if (beaconCount >= 3 && avgRSSI > -65 && stability > 0.7) {
            level = Level.HIGH;
            confidence = 0.8;
        } else if (beaconCount >= 2 && avgRSSI > -70 && stability > 0.5) {
            level = Level.MEDIUM;
            confidence = 0.6;
        } else if (beaconCount >= 1 && avgRSSI > -75) {
            level = Level.LOW;
            confidence = 0.4;
        } else {
            level = Level.POOR;
            confidence = 0.2;
        }

        return new SignalQuality(level, avgRSSI, stability, beaconCount, confidence, 0.0);
    }

    /**
     * 检查是否适合三角定位（WLS多点定位），要求至少4个有效信标
     */
    public boolean isSuitableForTrilateration() {
        return level == Level.HIGH && validBeacons >= 4;
    }

    /**
     * 检查是否适合加权质心定位
     */
    public boolean isSuitableForWeightedCentroid() {
        return level == Level.HIGH || level == Level.MEDIUM;
    }
}
