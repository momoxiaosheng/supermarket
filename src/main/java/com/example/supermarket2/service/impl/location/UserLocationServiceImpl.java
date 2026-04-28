package com.example.supermarket2.service.impl.location;

import com.example.supermarket2.entity.location.UserLocation;
import com.example.supermarket2.mapper.location.UserLocationMapper;
import com.example.supermarket2.service.location.UserLocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 用户位置服务实现类
 * 核心业务逻辑实现，与接口解耦
 */
@Slf4j
@Service
public class UserLocationServiceImpl implements UserLocationService {

    @Autowired
    private UserLocationMapper userLocationMapper;

    // 用户位置缓存（内存缓存，提高读取性能）
    private final ConcurrentMap<String, UserLocation> locationCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 2 * 60 * 1000; // 2分钟缓存
    private final ConcurrentMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    @Override
    public void updateUserLocation(UserLocation location) {
        try {
            log.debug("更新用户位置: userId={}, mapId={}, position=({}, {})",
                    location.getUserId(), location.getMapId(), location.getX(), location.getY());
            // 参数验证
            if (!isValidUserLocation(location)) {
                log.warn("用户位置数据无效: {}", location);
                return;
            }
            // 设置时间戳
            if (location.getTimeStamp() == null) {
                location.setTimeStamp(new Date());
            }
            // 更新数据库
            int affectedRows = userLocationMapper.upsertUserLocation(location);
            if (affectedRows > 0) {
                // 更新缓存
                updateLocationCache(location);
                log.debug("用户位置更新成功: userId={}", location.getUserId());
            } else {
                log.warn("用户位置更新失败: 数据库操作返回0行影响");
            }
        } catch (Exception e) {
            log.error("更新用户位置异常: userId={}", location.getUserId(), e);
            throw new RuntimeException("用户位置更新失败", e);
        }
    }

    @Override
    public void batchUpdateUserLocations(List<UserLocation> locations) {
        try {
            log.debug("批量更新用户位置: 数量={}", locations.size());
            if (locations == null || locations.isEmpty()) {
                return;
            }
            // 过滤有效位置数据
            List<UserLocation> validLocations = locations.stream()
                    .filter(this::isValidUserLocation)
                    .peek(location -> {
                        if (location.getTimeStamp() == null) {
                            location.setTimeStamp(new Date());
                        }
                    })
                    .toList();
            if (validLocations.isEmpty()) {
                log.warn("批量更新: 无有效位置数据");
                return;
            }
            // 批量更新数据库
            int affectedRows = userLocationMapper.batchUpsertUserLocations(validLocations);
            // 更新缓存
            for (UserLocation location : validLocations) {
                updateLocationCache(location);
            }
            log.debug("批量更新用户位置完成: 成功更新{}条记录", affectedRows);
        } catch (Exception e) {
            log.error("批量更新用户位置异常", e);
            throw new RuntimeException("批量更新用户位置失败", e);
        }
    }

    @Override
    public UserLocation getLatestUserLocation(String userId, String mapId) {
        try {
            log.debug("查询用户最新位置: userId={}, mapId={}", userId, mapId);
            // 参数验证
            if (userId == null || userId.trim().isEmpty() ||
                    mapId == null || mapId.trim().isEmpty()) {
                log.warn("查询参数无效: userId={}, mapId={}", userId, mapId);
                return null;
            }
            // 检查缓存
            String cacheKey = generateCacheKey(userId, mapId);
            UserLocation cachedLocation = getCachedLocation(cacheKey);
            if (cachedLocation != null) {
                log.debug("用户位置缓存命中: userId={}", userId);
                return cachedLocation;
            }
            // 从数据库查询
            UserLocation location = userLocationMapper.getLatestUserLocation(userId, mapId);
            if (location != null) {
                // 缓存结果
                cacheLocation(cacheKey, location);
                log.debug("用户位置查询完成: 找到位置记录");
            } else {
                log.debug("用户位置查询完成: 未找到位置记录");
            }
            return location;
        } catch (Exception e) {
            log.error("查询用户最新位置异常: userId={}, mapId={}", userId, mapId, e);
            return null;
        }
    }

    @Override
    public List<UserLocation> getUserLocationHistory(String userId, String mapId, Integer limit) {
        try {
            log.debug("查询用户位置历史: userId={}, mapId={}, limit={}", userId, mapId, limit);
            // 参数验证和默认值设置
            if (userId == null || userId.trim().isEmpty() ||
                    mapId == null || mapId.trim().isEmpty()) {
                return List.of();
            }
            int queryLimit = limit != null && limit > 0 ? Math.min(limit, 100) : 10;
            // 查询数据库
            List<UserLocation> history = userLocationMapper.getUserLocationHistory(userId, mapId, queryLimit);
            log.debug("用户位置历史查询完成: 记录数量={}", history.size());
            return history;
        } catch (Exception e) {
            log.error("查询用户位置历史异常", e);
            return List.of();
        }
    }

    @Override
    public List<UserLocation> getRecentUserLocationsByMap(String mapId) {
        try {
            log.debug("查询地图上的用户位置: mapId={}", mapId);
            if (mapId == null || mapId.trim().isEmpty()) {
                return List.of();
            }
            List<UserLocation> locations = userLocationMapper.getRecentUserLocationsByMap(mapId);
            log.debug("地图用户位置查询完成: 用户数量={}", locations.size());
            return locations;
        } catch (Exception e) {
            log.error("查询地图用户位置异常", e);
            return List.of();
        }
    }

    @Override
    public int deleteExpiredLocations(int expireHours) {
        try {
            log.debug("删除过期用户位置记录: expireHours={}", expireHours);
            if (expireHours <= 0) {
                log.warn("过期时间参数无效: expireHours={}", expireHours);
                return 0;
            }
            int deletedCount = userLocationMapper.deleteExpiredLocations(expireHours);
            // 清理相关缓存
            cleanupExpiredCache();
            log.info("删除过期用户位置记录完成: 删除{}条记录", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("删除过期用户位置记录异常", e);
            return 0;
        }
    }

    @Override
    public UserLocationStats getUserLocationStats(String userId, String mapId) {
        try {
            log.debug("获取用户位置统计: userId={}, mapId={}", userId, mapId);
            if (userId == null || userId.trim().isEmpty() ||
                    mapId == null || mapId.trim().isEmpty()) {
                return new UserLocationStats(0, 0.0, 0.0);
            }
            var stats = userLocationMapper.getLocationUpdateStats(userId, mapId);
            if (stats != null) {
                UserLocationStats result = new UserLocationStats(
                        stats.getUpdateCount(),
                        stats.getAvgInterval(),
                        stats.getUpdatesPerMinute()
                );
                log.debug("用户位置统计: {}", result);
                return result;
            }
            return new UserLocationStats(0, 0.0, 0.0);
        } catch (Exception e) {
            log.error("获取用户位置统计异常", e);
            return new UserLocationStats(0, 0.0, 0.0);
        }
    }

    @Override
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        for (String cacheKey : cacheTimestamps.keySet()) {
            Long timestamp = cacheTimestamps.get(cacheKey);
            if (timestamp != null && currentTime - timestamp > CACHE_DURATION) {
                locationCache.remove(cacheKey);
                cacheTimestamps.remove(cacheKey);
                removedCount++;
            }
        }
        if (removedCount > 0) {
            log.debug("清理过期用户位置缓存: 移除{}条记录", removedCount);
        }
    }

    @Override
    public void clearUserCache(String userId, String mapId) {
        String cacheKey = generateCacheKey(userId, mapId);
        locationCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
        log.debug("清除用户位置缓存: userId={}, mapId={}", userId, mapId);
    }

    @Override
    public CacheStats getCacheStats() {
        return new CacheStats(
                locationCache.size(),
                cacheTimestamps.size()
        );
    }

    /**
     * 验证用户位置数据有效性
     */
    private boolean isValidUserLocation(UserLocation location) {
        return location != null &&
                location.getUserId() != null && !location.getUserId().trim().isEmpty() &&
                location.getMapId() != null && !location.getMapId().trim().isEmpty() &&
                location.getX() != null && location.getX() >= 0 &&
                location.getY() != null && location.getY() >= 0;
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(String userId, String mapId) {
        return userId + ":" + mapId;
    }

    /**
     * 获取缓存的用户位置
     */
    private UserLocation getCachedLocation(String cacheKey) {
        Long timestamp = cacheTimestamps.get(cacheKey);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            return locationCache.get(cacheKey);
        }
        // 清理过期缓存
        if (timestamp != null) {
            locationCache.remove(cacheKey);
            cacheTimestamps.remove(cacheKey);
        }
        return null;
    }

    /**
     * 缓存用户位置
     */
    private void cacheLocation(String cacheKey, UserLocation location) {
        locationCache.put(cacheKey, location);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
    }

    /**
     * 更新位置缓存
     */
    private void updateLocationCache(UserLocation location) {
        String cacheKey = generateCacheKey(location.getUserId(), location.getMapId());
        cacheLocation(cacheKey, location);
    }
}