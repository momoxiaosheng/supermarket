package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.BeaconRequest;
import com.example.supermarket2.dto.location.LocationResult;

/**
 * 蓝牙定位服务接口
 * 定义蓝牙定位核心能力，实现与业务解耦
 */
public interface BluetoothLocationService {

    /**
     * 处理蓝牙信标数据并返回定位结果
     * @param request 蓝牙信标请求数据
     * @return 定位结果
     */
    LocationResult processBeaconData(BeaconRequest request);

    /**
     * 获取定位服务统计信息
     * @return 服务统计数据
     */
    LocationServiceStats getServiceStats();

    /**
     * 重置服务统计
     */
    void resetStats();

    /**
     * 定位服务统计类
     */
    class LocationServiceStats {
        private final long totalRequests;
        private final long successfulRequests;
        private final double successRate;
        private final int activeUsers;

        public LocationServiceStats(long totalRequests, long successfulRequests,
                                    double successRate, int activeUsers) {
            this.totalRequests = totalRequests;
            this.successfulRequests = successfulRequests;
            this.successRate = successRate;
            this.activeUsers = activeUsers;
        }

        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public double getSuccessRate() { return successRate; }
        public int getActiveUsers() { return activeUsers; }
    }
}