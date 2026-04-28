package com.example.supermarket2.service.location;

import com.example.supermarket2.entity.location.UserLocation;

import java.util.List;

/**
 * 用户位置服务接口
 * 定义用户位置CRUD、缓存、统计能力
 */
public interface UserLocationService {

    /**
     * 更新用户位置（新增或覆盖）
     * @param location 用户位置数据
     */
    void updateUserLocation(UserLocation location);

    /**
     * 批量更新用户位置
     * @param locations 用户位置列表
     */
    void batchUpdateUserLocations(List<UserLocation> locations);

    /**
     * 查询用户最新位置（带缓存）
     * @param userId 用户ID
     * @param mapId 地图ID
     * @return 用户最新位置
     */
    UserLocation getLatestUserLocation(String userId, String mapId);

    /**
     * 查询用户位置历史
     * @param userId 用户ID
     * @param mapId 地图ID
     * @param limit 最大返回条数
     * @return 位置历史列表
     */
    List<UserLocation> getUserLocationHistory(String userId, String mapId, Integer limit);

    /**
     * 查询地图上的所有用户位置
     * @param mapId 地图ID
     * @return 最近用户位置列表
     */
    List<UserLocation> getRecentUserLocationsByMap(String mapId);

    /**
     * 删除过期的用户位置记录
     * @param expireHours 过期小时数
     * @return 删除条数
     */
    int deleteExpiredLocations(int expireHours);

    /**
     * 获取用户位置更新统计
     * @param userId 用户ID
     * @param mapId 地图ID
     * @return 统计数据
     */
    UserLocationStats getUserLocationStats(String userId, String mapId);

    /**
     * 清理过期缓存
     */
    void cleanupExpiredCache();

    /**
     * 清除指定用户的缓存
     * @param userId 用户ID
     * @param mapId 地图ID
     */
    void clearUserCache(String userId, String mapId);

    /**
     * 获取缓存统计信息
     * @return 缓存统计数据
     */
    CacheStats getCacheStats();

    /**
     * 用户位置统计类
     */
    class UserLocationStats {
        private final int updateCount;
        private final double avgInterval;
        private final double updatesPerMinute;

        public UserLocationStats(int updateCount, double avgInterval, double updatesPerMinute) {
            this.updateCount = updateCount;
            this.avgInterval = avgInterval;
            this.updatesPerMinute = updatesPerMinute;
        }

        public int getUpdateCount() { return updateCount; }
        public double getAvgInterval() { return avgInterval; }
        public double getUpdatesPerMinute() { return updatesPerMinute; }

        @Override
        public String toString() {
            return String.format("UserLocationStats{updates=%d, interval=%.1fs, rate=%.1f/min}",
                    updateCount, avgInterval, updatesPerMinute);
        }
    }

    /**
     * 缓存统计类
     */
    class CacheStats {
        private final int cachedLocations;
        private final int cacheEntries;

        public CacheStats(int cachedLocations, int cacheEntries) {
            this.cachedLocations = cachedLocations;
            this.cacheEntries = cacheEntries;
        }

        public int getCachedLocations() { return cachedLocations; }
        public int getCacheEntries() { return cacheEntries; }

        @Override
        public String toString() {
            return String.format("CacheStats{locations=%d, entries=%d}", cachedLocations, cacheEntries);
        }
    }
}
