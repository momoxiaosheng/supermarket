package com.example.supermarket2.dto.location;

import com.example.supermarket2.entity.location.LocationQuality;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定位质量传输对象
 * 用于传输定位质量的详细信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationQualityDTO {
    private String level;           // 质量等级
    private Double score;           // 质量分数
    private Integer beaconCount;    // 信标数量
    private Double avgRSSI;         // 平均RSSI
    private Double stability;       // 稳定性
    private Double confidence;      // 置信度
    private String algorithm;       // 使用算法
    private String description;     // 质量描述

    /**
     * 从实体类转换
     */
    public static LocationQualityDTO fromEntity(LocationQuality quality) {
        if (quality == null) return null;

        return LocationQualityDTO.builder()
                .level(quality.getLevel().name())
                .score(quality.getScore())
                .beaconCount(quality.getBeaconCount())
                .avgRSSI(quality.getAvgRSSI())
                .stability(quality.getStability())
                .confidence(quality.getConfidence())
                .algorithm(quality.getAlgorithm())
                .description(generateDescription(quality))
                .build();
    }

    /**
     * 生成质量描述
     */
    private static String generateDescription(LocationQuality quality) {
        if (quality == null) return "未知质量";

        switch (quality.getLevel()) {
            case HIGH:
                return String.format("高质量定位（%d个信标，平均RSSI: %.1f）",
                        quality.getBeaconCount(), quality.getAvgRSSI());
            case MEDIUM:
                return String.format("中等质量定位（%d个信标，平均RSSI: %.1f）",
                        quality.getBeaconCount(), quality.getAvgRSSI());
            case LOW:
                return String.format("低质量定位（%d个信标，平均RSSI: %.1f）",
                        quality.getBeaconCount(), quality.getAvgRSSI());
            case POOR:
                return "信号质量较差，定位可能不准确";
            case PREDICTED:
                return "基于运动模型的预测位置";
            default:
                return "未知定位质量";
        }
    }

    /**
     * 检查是否为高质量定位
     */
    public boolean isHighQuality() {
        return "HIGH".equals(level) && score != null && score >= 0.8;
    }

    /**
     * 检查是否为可靠定位
     */
    public boolean isReliable() {
        return !"POOR".equals(level) && confidence != null && confidence >= 0.5;
    }
}

