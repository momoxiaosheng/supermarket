package com.example.supermarket2.service.impl.location;

import com.example.supermarket2.entity.location.MapGrid;
import com.example.supermarket2.mapper.location.MapGridMapper;
import com.example.supermarket2.service.location.MapGridService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 地图网格服务实现类
 * 核心业务逻辑实现，与接口解耦
 */
@Slf4j
@Service
public class MapGridServiceImpl implements MapGridService {

    @Autowired
    private MapGridMapper mapGridMapper;

    // 地图数据缓存
    private final ConcurrentMap<String, List<MapGrid>> mapCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 10 * 60 * 1000; // 10分钟缓存

    @Override
    public List<MapGrid> getMapGridByMapId(String mapId) {
        try {
            log.debug("查询地图网格数据: mapId={}", mapId);
            if (mapId == null || mapId.trim().isEmpty()) {
                log.warn("地图ID为空");
                return List.of();
            }
            // 检查缓存
            List<MapGrid> cachedData = getCachedMapData(mapId);
            if (cachedData != null) {
                log.debug("地图数据缓存命中: mapId={}", mapId);
                return cachedData;
            }
            // 从数据库查询
            List<MapGrid> gridList = mapGridMapper.getMapGridByMapId(mapId);
            // 缓存结果
            cacheMapData(mapId, gridList);
            log.debug("地图数据查询完成: mapId={}, 网格数量={}", mapId, gridList.size());
            return gridList;
        } catch (Exception e) {
            log.error("查询地图网格数据异常: mapId={}", mapId, e);
            return List.of();
        }
    }

    @Override
    public List<MapGrid> getMapGridByArea(String mapId, int minX, int maxX, int minY, int maxY) {
        try {
            log.debug("查询区域地图数据: mapId={}, 区域=[{}-{}, {}-{}]",
                    mapId, minX, maxX, minY, maxY);
            // 参数验证
            if (mapId == null || mapId.trim().isEmpty()) {
                return List.of();
            }
            if (minX < 0 || maxX < minX || minY < 0 || maxY < minY) {
                log.warn("区域参数无效: minX={}, maxX={}, minY={}, maxY={}", minX, maxX, minY, maxY);
                return List.of();
            }
            // 使用Mapper的扩展方法查询区域数据
            List<MapGrid> allGrids = getMapGridByMapId(mapId);
            // 过滤指定区域
            List<MapGrid> areaGrids = allGrids.stream()
                    .filter(grid -> grid.getX() >= minX && grid.getX() <= maxX &&
                            grid.getY() >= minY && grid.getY() <= maxY)
                    .toList();
            log.debug("区域地图数据查询完成: 网格数量={}", areaGrids.size());
            return areaGrids;
        } catch (Exception e) {
            log.error("查询区域地图数据异常", e);
            return List.of();
        }
    }

    @Override
    public List<MapGrid> getPassableMapGrid(String mapId) {
        try {
            log.debug("查询可通行地图数据: mapId={}", mapId);
            List<MapGrid> allGrids = getMapGridByMapId(mapId);
            List<MapGrid> passableGrids = allGrids.stream()
                    .filter(grid -> grid.getPassable() == 1)
                    .toList();
            log.debug("可通行地图数据查询完成: 可通行网格数量={}", passableGrids.size());
            return passableGrids;
        } catch (Exception e) {
            log.error("查询可通行地图数据异常", e);
            return List.of();
        }
    }

    @Override
    public boolean isPositionPassable(String mapId, int x, int y) {
        try {
            log.debug("检查位置可通行性: mapId={}, position=({}, {})", mapId, x, y);
            List<MapGrid> allGrids = getMapGridByMapId(mapId);
            boolean isPassable = allGrids.stream()
                    .anyMatch(grid -> grid.getX().equals(x) &&
                            grid.getY().equals(y) &&
                            grid.getPassable() == 1);
            log.debug("位置可通行性检查结果: passable={}", isPassable);
            return isPassable;
        } catch (Exception e) {
            log.error("检查位置可通行性异常", e);
            return false;
        }
    }

    @Override
    public MapBounds getMapBounds(String mapId) {
        try {
            log.debug("获取地图边界信息: mapId={}", mapId);
            List<MapGrid> allGrids = getMapGridByMapId(mapId);
            if (allGrids.isEmpty()) {
                return new MapBounds(0, 0, 0, 0);
            }
            int minX = allGrids.stream().mapToInt(MapGrid::getX).min().orElse(0);
            int maxX = allGrids.stream().mapToInt(MapGrid::getX).max().orElse(0);
            int minY = allGrids.stream().mapToInt(MapGrid::getY).min().orElse(0);
            int maxY = allGrids.stream().mapToInt(MapGrid::getY).max().orElse(0);
            MapBounds bounds = new MapBounds(minX, maxX, minY, maxY);
            log.debug("地图边界信息: {}", bounds);
            return bounds;
        } catch (Exception e) {
            log.error("获取地图边界信息异常", e);
            return new MapBounds(0, 0, 0, 0);
        }
    }

    @Override
    public MapStatistics getMapStatistics(String mapId) {
        try {
            log.debug("获取地图统计信息: mapId={}", mapId);
            List<MapGrid> allGrids = getMapGridByMapId(mapId);
            if (allGrids.isEmpty()) {
                return new MapStatistics(0, 0, 0, 0, 0);
            }
            long totalGrids = allGrids.size();
            long passableGrids = allGrids.stream().filter(grid -> grid.getPassable() == 1).count();
            long obstacleGrids = totalGrids - passableGrids;
            double passableRatio = totalGrids > 0 ? (double) passableGrids / totalGrids * 100 : 0;
            MapBounds bounds = getMapBounds(mapId);
            int area = (bounds.getWidth() + 1) * (bounds.getHeight() + 1);
            MapStatistics stats = new MapStatistics(totalGrids, passableGrids, obstacleGrids, passableRatio, area);
            log.debug("地图统计信息: {}", stats);
            return stats;
        } catch (Exception e) {
            log.error("获取地图统计信息异常", e);
            return new MapGridService.MapStatistics(0, 0, 0, 0, 0);
        }
    }

    @Override
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;
        for (String mapId : cacheTimestamps.keySet()) {
            Long timestamp = cacheTimestamps.get(mapId);
            if (timestamp != null && currentTime - timestamp > CACHE_DURATION) {
                mapCache.remove(mapId);
                cacheTimestamps.remove(mapId);
                removedCount++;
            }
        }
        if (removedCount > 0) {
            log.info("清理过期地图缓存: 移除{}个地图的缓存", removedCount);
        }
    }

    @Override
    public void clearMapCache(String mapId) {
        mapCache.remove(mapId);
        cacheTimestamps.remove(mapId);
        log.info("清除地图缓存: mapId={}", mapId);
    }

    @Override
    public void clearAllCache() {
        int cacheSize = mapCache.size();
        mapCache.clear();
        cacheTimestamps.clear();
        log.info("清除所有地图缓存: 原有缓存数量={}", cacheSize);
    }

    /**
     * 获取缓存的映射数据
     */
    private List<MapGrid> getCachedMapData(String mapId) {
        Long timestamp = cacheTimestamps.get(mapId);
        if (timestamp != null && System.currentTimeMillis() - timestamp < CACHE_DURATION) {
            return mapCache.get(mapId);
        }
        // 清理过期缓存
        if (timestamp != null) {
            mapCache.remove(mapId);
            cacheTimestamps.remove(mapId);
        }
        return null;
    }

    /**
     * 缓存映射数据
     */
    private void cacheMapData(String mapId, List<MapGrid> gridList) {
        mapCache.put(mapId, gridList);
        cacheTimestamps.put(mapId, System.currentTimeMillis());
    }
}
