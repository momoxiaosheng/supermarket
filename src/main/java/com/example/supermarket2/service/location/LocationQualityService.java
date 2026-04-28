package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.BeaconData;
import com.example.supermarket2.dto.location.BeaconRequest;
import com.example.supermarket2.entity.location.LocationQuality;
import com.example.supermarket2.entity.location.Position;
import com.example.supermarket2.entity.location.SignalQuality;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 定位质量评估服务
 * 评估定位结果的质量和可信度
 */
@Slf4j
@Service
public class LocationQualityService {

    /**
     * 评估定位质量
     */
    public LocationQuality assessLocationQuality(List<BeaconData> beacons,
                                                 Position position,
                                                 BeaconRequest request) {
        try {
            // 1. 评估信号质量
            SignalQuality signalQuality = assessSignalQuality(beacons);

            // 2. 评估位置一致性
            double consistencyScore = assessPositionConsistency(position, request.getUserId());

            // 3. 综合质量评估
            return calculateOverallQuality(signalQuality, consistencyScore, beacons.size(), position);

        } catch (Exception e) {
            log.error("定位质量评估异常", e);
            return LocationQuality.poorQuality();
        }
    }

    /**
     * 评估信号质量
     */
    public SignalQuality assessSignalQuality(List<BeaconData> beacons) {
        if (beacons == null || beacons.isEmpty()) {
            return new SignalQuality(SignalQuality.Level.POOR, -100.0, 0.0, 0, 0.1, 0.0);
        }

        // 计算平均RSSI
        double avgRSSI = beacons.stream()
                .mapToInt(BeaconData::getRssi)
                .average()
                .orElse(-100.0);

        // 计算信号稳定性
        double stability = calculateSignalStability(beacons);

        // 有效信标数量
        int validBeacons = (int) beacons.stream()
                .filter(beacon -> beacon.getRssi() > -80)
                .count();

        // 综合评估
        return SignalQuality.assess(validBeacons, avgRSSI, stability);
    }

    /**
     * 计算信号稳定性
     */
    private double calculateSignalStability(List<BeaconData> beacons) {
        if (beacons.size() < 2) {
            return 1.0; // 单个信标认为稳定
        }

        // 计算RSSI方差
        double mean = beacons.stream()
                .mapToInt(BeaconData::getRssi)
                .average()
                .orElse(0);

        double variance = beacons.stream()
                .mapToDouble(beacon -> Math.pow(beacon.getRssi() - mean, 2))
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);

        // 标准差越小，稳定性越高
        // 使用sigmoid函数映射到0-1范围
        return 1.0 / (1.0 + stdDev / 10.0);
    }

    /**
     * 评估位置一致性
     */
    private double assessPositionConsistency(Position currentPosition, String userId) {
        // 这里可以集成历史位置数据来评估一致性
        // 暂时返回中等一致性
        return 0.7;
    }

    /**
     * 计算综合质量
     */
    private LocationQuality calculateOverallQuality(SignalQuality signalQuality,
                                                    double consistencyScore,
                                                    int beaconCount,
                                                    Position position) {
        // 基础质量基于信号质量
        LocationQuality baseQuality;

        switch (signalQuality.getLevel()) {
            case HIGH:
                baseQuality = LocationQuality.highQuality(beaconCount,
                        signalQuality.getAvgRSSI(), position.getAlgorithm());
                break;
            case MEDIUM:
                baseQuality = LocationQuality.mediumQuality(beaconCount,
                        signalQuality.getAvgRSSI(), position.getAlgorithm());
                break;
            case LOW:
                baseQuality = LocationQuality.lowQuality(beaconCount,
                        signalQuality.getAvgRSSI(), position.getAlgorithm());
                break;
            default:
                baseQuality = LocationQuality.poorQuality();
        }

        // 应用一致性调整
        return adjustQualityWithConsistency(baseQuality, consistencyScore);
    }

    /**
     * 根据一致性调整质量
     */
    private LocationQuality adjustQualityWithConsistency(LocationQuality quality, double consistency) {
        // 一致性影响置信度
        double adjustedConfidence = quality.getConfidence() * consistency;
        double adjustedScore = quality.getScore() * (0.7 + 0.3 * consistency);

        // 创建调整后的质量对象
        LocationQuality adjusted = new LocationQuality(
                quality.getLevel(),
                adjustedScore,
                quality.getBeaconCount(),
                quality.getAvgRSSI(),
                quality.getStability(),
                adjustedConfidence,
                quality.getAlgorithm()
        );

        log.debug("质量调整: 原置信度={}, 一致性={}, 调整后置信度={}",
                quality.getConfidence(), consistency, adjustedConfidence);

        return adjusted;
    }

    /**
     * 评估定位是否可靠
     */
    public boolean isLocationReliable(LocationQuality quality) {
        return quality != null &&
                quality.getConfidence() >= 0.5 &&
                quality.getScore() >= 0.4;
    }

    /**
     * 获取质量描述
     */
    public String getQualityDescription(LocationQuality quality) {
        if (quality == null) {
            return "未知质量";
        }

        switch (quality.getLevel()) {
            case HIGH:
                return String.format("高质量定位 (置信度: %.1f%%)", quality.getConfidence() * 100);
            case MEDIUM:
                return String.format("中等质量定位 (置信度: %.1f%%)", quality.getConfidence() * 100);
            case LOW:
                return String.format("低质量定位 (置信度: %.1f%%)", quality.getConfidence() * 100);
            case POOR:
                return "信号质量较差";
            case PREDICTED:
                return "基于历史数据的预测位置";
            default:
                return "未知定位质量";
        }
    }
}

