package com.example.supermarket2.entity.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 信标配置参数实体
 * 存储信标定位系统的配置参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "beacon.config")
public class BeaconConfig {
    // RSSI相关配置
    private Double rssi0 = -55.0;                   // 参考距离RSSI值
    private Double attenuationFactor = 3.2;         // 路径损耗指数
    private Integer rssiMin = -95;                  // 最小有效RSSI
    private Integer rssiMax = -35;                  // 最大有效RSSI

    // 滤波配置
    private Integer historySize = 10;               // 历史数据大小
    private Double outlierThreshold = 3.0;          // 野值检测阈值
    private Double stabilityThreshold = 0.6;        // 稳定性阈值

    // 定位算法配置
    private Double kalmanProcessNoise = 1.0;        // 卡尔曼过程噪声
    private Double kalmanMeasurementNoise = 2.0;    // 卡尔曼测量噪声
    private Double maxMovementDistance = 5.0;       // 最大移动距离（米）

    // 质量评估配置
    private Double highQualityRssiThreshold = -65.0;    // 高质量RSSI阈值
    private Double mediumQualityRssiThreshold = -70.0;  // 中等质量RSSI阈值
    private Double lowQualityRssiThreshold = -75.0;     // 低质量RSSI阈值

    // 权重配置
    private Double rssiWeightExponent = 0.2;        // RSSI权重指数

    // 滤波链路配置
    private Double maxJumpDistance = 5.0;           // 限幅滤波：最大允许跳变距离（米）
    private Integer medianWindowSize = 5;           // 中值滤波：窗口大小（帧数）
    private Double emaAlpha = 0.3;                  // EMA滤波：平滑因子（0~1，越小越平滑）

    // WLS多点定位配置
    private Integer wlsMinBeacons = 4;              // WLS定位最少信标数
    private Integer wlsMaxBeacons = 5;              // WLS定位最多信标数
    private Double wlsResidualThreshold = 2.0;      // 残差阈值：超过则降级到4点定位
    private Double weightThreshold = 0.1;           // 信标权重阈值：低于此值视为弱信标

    /**
     * 验证配置参数的有效性
     */
    public boolean isValid() {
        return rssi0 != null && attenuationFactor != null &&
                rssiMin != null && rssiMax != null &&
                rssiMin < rssiMax && attenuationFactor > 0;
    }

    /**
     * 根据RSSI计算权重
     */
    public Double calculateWeight(Integer rssi) {
        if (rssi == null || rssi >= rssiMin) return 0.0;

        // 使用指数权重，信号越强权重越高
        return Math.pow(10, rssi * rssiWeightExponent / 10.0);
    }

    /**
     * 限幅处理RSSI值
     */
    public Integer clampRssi(Integer rssi) {
        if (rssi == null) return rssiMin;
        return Math.max(rssiMin, Math.min(rssiMax, rssi));
    }
}

