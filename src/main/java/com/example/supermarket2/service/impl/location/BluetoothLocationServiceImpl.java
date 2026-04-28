package com.example.supermarket2.service.impl.location;

import com.example.supermarket2.dto.location.BeaconRequest;
import com.example.supermarket2.dto.location.LocationResult;
import com.example.supermarket2.entity.location.LocationQuality;
import com.example.supermarket2.entity.location.Position;
import com.example.supermarket2.service.WebSocketPushService;
import com.example.supermarket2.service.location.BluetoothLocationService;
import com.example.supermarket2.service.location.FusionLocationEngine;
import com.example.supermarket2.service.location.LocationQualityService;
import com.example.supermarket2.service.location.SignalProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 蓝牙定位服务实现类
 * 核心业务逻辑实现，与接口解耦
 */
@Slf4j
@Service
public class BluetoothLocationServiceImpl implements BluetoothLocationService {

    @Autowired
    private FusionLocationEngine fusionLocationEngine;
    @Autowired
    private SignalProcessor signalProcessor;
    @Autowired
    private LocationQualityService locationQualityService;
    @Autowired
    private WebSocketPushService webSocketPushService;

    // 性能监控
    private final AtomicLong requestCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, Long> userLastRequestTime = new ConcurrentHashMap<>();

    @Override
    public LocationResult processBeaconData(BeaconRequest request) {
        long startTime = System.currentTimeMillis();
        requestCount.incrementAndGet();
        try {
            log.debug("开始处理蓝牙定位请求: userId={}, 信标数量={}",
                    request.getUserId(), request.getBeacons().size());
            // 更新用户请求时间
            userLastRequestTime.put(request.getUserId(), System.currentTimeMillis());
            // 1. 信号预处理
            var processedBeacons = signalProcessor.preProcessBeacons(request.getBeacons());
            if (processedBeacons.isEmpty()) {
                log.warn("信号预处理后无有效信标: userId={}", request.getUserId());
                return LocationResult.error("无有效信标数据");
            }
            // 2. 使用融合引擎计算位置
            Position position = fusionLocationEngine.calculatePosition(processedBeacons, request);
            if (position == null || !position.isValid()) {
                log.warn("融合定位引擎返回无效位置: userId={}", request.getUserId());
                return handlePositioningFailure(request);
            }
            // 3. 评估定位质量
            LocationQuality quality = locationQualityService.assessLocationQuality(
                    processedBeacons, position, request);
            // 4. 构建成功响应
            LocationResult result = buildSuccessResult(position, quality, processedBeacons.size());
            successCount.incrementAndGet();
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("蓝牙定位完成: userId={}, 位置=({}, {}), 质量={}, 耗时={}ms",
                    request.getUserId(), position.getX(), position.getY(),
                    quality.getLevel(), processingTime);
            if (result.isSuccess()) {
                Long userId = Long.parseLong(request.getUserId());
                webSocketPushService.pushLocationUpdate(userId, result);
            }
            return result;
        } catch (Exception e) {
            log.error("蓝牙定位处理异常: userId={}", request.getUserId(), e);
            return LocationResult.error("定位处理异常: " + e.getMessage());
        }
    }

    @Override
    public LocationServiceStats getServiceStats() {
        long totalRequests = requestCount.get();
        long successfulRequests = successCount.get();
        double successRate = totalRequests > 0 ? (double) successfulRequests / totalRequests * 100 : 0;
        return new LocationServiceStats(
                totalRequests,
                successfulRequests,
                successRate,
                userLastRequestTime.size()
        );
    }

    @Override
    public void resetStats() {
        requestCount.set(0);
        successCount.set(0);
        userLastRequestTime.clear();
        log.info("定位服务统计已重置");
    }

    /**
     * 处理定位失败情况
     */
    private LocationResult handlePositioningFailure(BeaconRequest request) {
        // 尝试使用上次已知位置
        Position lastKnownPosition = fusionLocationEngine.getLastKnownPosition(request.getUserId());
        if (lastKnownPosition != null) {
            log.info("使用上次已知位置: userId={}, position=({}, {})",
                    request.getUserId(), lastKnownPosition.getX(), lastKnownPosition.getY());
            LocationQuality quality = LocationQuality.predictedQuality();
            return buildSuccessResult(lastKnownPosition, quality, 0);
        }
        return LocationResult.error("定位失败且无历史位置");
    }

    /**
     * 构建成功定位结果
     */
    private LocationResult buildSuccessResult(Position position, LocationQuality quality, int beaconCount) {
        return LocationResult.success(
                position.getX(),
                position.getY(),
                quality.getLevel().name().toLowerCase(),
                beaconCount,
                position.getAlgorithm(),
                quality.getConfidence(),
                quality
        );
    }
}