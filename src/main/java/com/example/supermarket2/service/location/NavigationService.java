package com.example.supermarket2.service.location;

import com.example.supermarket2.dto.location.NavigationRequest;
import com.example.supermarket2.dto.location.NavigationResponse;

/**
 * 导航服务接口
 * 定义路径规划核心能力，实现与业务解耦
 */
public interface NavigationService {

    /**
     * 增强路径规划（支持不可通行目标点调整）
     * @param request 路径规划请求
     * @return 导航路径结果
     */
    NavigationResponse calculateEnhancedPath(NavigationRequest request);

    /**
     * 清理过期缓存
     */
    void cleanupExpiredCache();

    /**
     * 获取服务统计信息
     * @return 导航服务统计数据
     */
    NavigationStats getServiceStats();

    /**
     * 导航服务统计类
     */
    class NavigationStats {
        private final int totalRequests;
        private final int successfulPaths;
        private final int cacheHits;
        private final int cacheSize;

        public NavigationStats(int totalRequests, int successfulPaths, int cacheHits, int cacheSize) {
            this.totalRequests = totalRequests;
            this.successfulPaths = successfulPaths;
            this.cacheHits = cacheHits;
            this.cacheSize = cacheSize;
        }

        public int getTotalRequests() { return totalRequests; }
        public int getSuccessfulPaths() { return successfulPaths; }
        public int getCacheHits() { return cacheHits; }
        public int getCacheSize() { return cacheSize; }
        public double getSuccessRate() {
            return totalRequests > 0 ? (double) successfulPaths / totalRequests * 100 : 0;
        }
        public double getCacheHitRate() {
            return totalRequests > 0 ? (double) cacheHits / totalRequests * 100 : 0;
        }
    }
}
