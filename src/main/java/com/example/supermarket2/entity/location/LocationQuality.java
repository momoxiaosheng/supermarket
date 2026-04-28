package com.example.supermarket2.entity.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定位质量评估结果
 * 存储等级、分数、信标数量等质量信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationQuality {
    public enum QualityLevel {
        HIGH,       // 高质量：≥3个信标，RSSI > -65dBm
        MEDIUM,     // 中等质量：≥2个信标，RSSI > -70dBm
        LOW,        // 低质量：≥1个信标，RSSI > -75dBm
        POOR,       // 差质量：其他情况
        PREDICTED   // 预测位置：信号丢失时使用
    }

    private QualityLevel level;         // 质量等级
    private Double score;               // 质量分数（0-1）
    private Integer beaconCount;        // 有效信标数量
    private Double avgRSSI;            // 平均RSSI值
    private Double stability;           // 信号稳定性（0-1）
    private Double confidence;          // 置信度（0-1）
    private String algorithm;           // 使用的定位算法

    /**
     * 构建高质量定位结果
     */
    public static LocationQuality highQuality(int beaconCount, double avgRSSI, String algorithm) {
        return new LocationQuality(
                QualityLevel.HIGH,
                0.9,
                beaconCount,
                avgRSSI,
                0.8,
                0.85,
                algorithm
        );
    }

    /**
     * 构建中等质量定位结果
     */
    public static LocationQuality mediumQuality(int beaconCount, double avgRSSI, String algorithm) {
        return new LocationQuality(
                QualityLevel.MEDIUM,
                0.7,
                beaconCount,
                avgRSSI,
                0.6,
                0.6,
                algorithm
        );
    }

    /**
     * 构建低质量定位结果
     */
    public static LocationQuality lowQuality(int beaconCount, double avgRSSI, String algorithm) {
        return new LocationQuality(
                QualityLevel.LOW,
                0.5,
                beaconCount,
                avgRSSI,
                0.4,
                0.4,
                algorithm
        );
    }

    /**
     * 构建差质量定位结果
     */
    public static LocationQuality poorQuality() {
        return new LocationQuality(
                QualityLevel.POOR,
                0.2,
                0,
                -100.0,
                0.1,
                0.1,
                "conservative"
        );
    }

    /**
     * 构建预测位置质量结果
     */
    public static LocationQuality predictedQuality() {
        return new LocationQuality(
                QualityLevel.PREDICTED,
                0.3,
                0,
                null,
                0.2,
                0.2,
                "prediction"
        );
    }
}

