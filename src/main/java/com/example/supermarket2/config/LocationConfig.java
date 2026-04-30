package com.example.supermarket2.config;

import com.example.supermarket2.utils.location.KalmanFilter2D;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定位系统配置类
 * 集中管理定位相关的配置参数和Bean创建
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "location")
public class LocationConfig {

    // 定位算法配置
    private AlgorithmConfig algorithm = new AlgorithmConfig();

    // 信号处理配置
    private SignalConfig signal = new SignalConfig();

    // 质量评估配置
    private QualityConfig quality = new QualityConfig();

    // 系统性能配置
    private PerformanceConfig performance = new PerformanceConfig();

    /**
     * 创建卡尔曼滤波器Bean
     */
    @Bean
    public KalmanFilter2D kalmanFilter() {
        log.info("初始化卡尔曼滤波器: 过程噪声={}, 测量噪声={}",
                algorithm.getKalmanProcessNoise(), algorithm.getKalmanMeasurementNoise());

        return new KalmanFilter2D(
                algorithm.getKalmanProcessNoise(),
                algorithm.getKalmanMeasurementNoise()
        );
    }

    /**
     * 算法配置
     */
    @Data
    public static class AlgorithmConfig {
        private Double kalmanProcessNoise = 1.0;
        private Double kalmanMeasurementNoise = 2.0;
        private Double maxMovementDistance = 5.0;
        private Double rssiWeightExponent = 0.2;
        private Integer minBeaconsForTrilateration = 3;
        private Integer minBeaconsForCentroid = 1;
        private Double predictionScale = 1.2;
    }

    /**
     * 信号处理配置
     */
    @Data
    public static class SignalConfig {
        private Double rssi0 = -55.0;
        private Double attenuationFactor = 3.2;
        private Integer rssiMin = -95;
        private Integer rssiMax = -35;
        private Integer historySize = 10;
        private Double outlierThreshold = 3.0;
        private Double stabilityThreshold = 0.6;
        private Integer movingAverageWindow = 5;
    }

    /**
     * 质量评估配置
     */
    @Data
    public static class QualityConfig {
        private Double highQualityRssiThreshold = -65.0;
        private Double mediumQualityRssiThreshold = -70.0;
        private Double lowQualityRssiThreshold = -75.0;
        private Integer highQualityBeaconCount = 3;
        private Integer mediumQualityBeaconCount = 2;
        private Integer minConfidence = 50;
        private Double stabilityWeight = 0.3;
    }

    /**
     * 系统性能配置
     */
    @Data
    public static class PerformanceConfig {
        private Integer maxPositionHistory = 20;
        private Integer locationTimeout = 5000; // 毫秒
        private Boolean enableDetailedLogging = false;
        private Double maxAllowableError = 10.0; // 米
        private Integer cacheTimeout = 300; // 秒
    }

    /**
     * 获取完整的配置信息（用于日志记录）
     */
    public String getConfigSummary() {
        return String.format(
                "定位系统配置 - 算法: Kalman(%.1f,%.1f), 信号: RSSI0=%.1f/n=%.1f, 质量: 高<%.1f/中<%.1f/低<%.1f",
                algorithm.getKalmanProcessNoise(),
                algorithm.getKalmanMeasurementNoise(),
                signal.getRssi0(),
                signal.getAttenuationFactor(),
                quality.getHighQualityRssiThreshold(),
                quality.getMediumQualityRssiThreshold(),
                quality.getLowQualityRssiThreshold()
        );
    }
}

