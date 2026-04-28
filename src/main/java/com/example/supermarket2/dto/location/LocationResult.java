package com.example.supermarket2.dto.location;

import com.example.supermarket2.entity.location.LocationQuality;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 定位结果DTO
 * 封装定位坐标、质量、状态等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResult {
    private Double x;                           // X坐标
    private Double y;                           // Y坐标
    private String quality;                     // 质量等级
    private Integer beaconCount;                // 信标数量
    private Long timestamp;                     // 时间戳
    private Boolean success;                    // 是否成功
    private String message;                     // 消息
    private String algorithm;                   // 使用的算法
    private Double confidence;                  // 置信度
    private LocationQuality detailedQuality;    // 详细质量信息
    private Map<String, Object> metadata;       // 元数据

    /**
     * 构建成功定位结果
     */
    public static LocationResult success(Double x, Double y, String quality,
                                         Integer beaconCount, String algorithm,
                                         Double confidence, LocationQuality detailedQuality) {
        return LocationResult.builder()
                .x(x)
                .y(y)
                .quality(quality)
                .beaconCount(beaconCount)
                .timestamp(System.currentTimeMillis())
                .success(true)
                .algorithm(algorithm)
                .confidence(confidence)
                .detailedQuality(detailedQuality)
                .build();
    }

    /**
     * 构建失败定位结果
     */
    public static LocationResult error(String message) {
        return LocationResult.builder()
                .success(false)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 构建预测定位结果
     */
    public static LocationResult predicted(Double x, Double y, Double confidence) {
        return LocationResult.builder()
                .x(x)
                .y(y)
                .quality("predicted")
                .timestamp(System.currentTimeMillis())
                .success(true)
                .algorithm("prediction")
                .confidence(confidence)
                .build();
    }

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }

    /**
     * 检查位置是否有效
     */
    public boolean isValidPosition() {
        return success && x != null && y != null &&
                !x.isNaN() && !y.isNaN() &&
                x >= 0 && x <= 100 &&
                y >= 0 && y <= 100;
    }
}
