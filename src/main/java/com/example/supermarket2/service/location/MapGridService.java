package com.example.supermarket2.service.location;


import com.example.supermarket2.entity.location.MapGrid;

import java.util.List;

/**
 * 地图网格服务接口
 * 定义地图数据查询、可通行性校验、缓存管理能力
 */
public interface MapGridService {

    /**
     * 查询地图网格数据（带缓存）
     * @param mapId 地图ID
     * @return 地图网格列表
     */
    List<MapGrid> getMapGridByMapId(String mapId);

    /**
     * 查询指定范围内的网格数据
     * @param mapId 地图ID
     * @param minX 最小X坐标
     * @param maxX 最大X坐标
     * @param minY 最小Y坐标
     * @param maxY 最大Y坐标
     * @return 区域内网格列表
     */
    List<MapGrid> getMapGridByArea(String mapId, int minX, int maxX, int minY, int maxY);

    /**
     * 查询可通行网格数据
     * @param mapId 地图ID
     * @return 可通行网格列表
     */
    List<MapGrid> getPassableMapGrid(String mapId);

    /**
     * 检查指定位置是否可通行
     * @param mapId 地图ID
     * @param x X坐标
     * @param y Y坐标
     * @return 是否可通行
     */
    boolean isPositionPassable(String mapId, int x, int y);

    /**
     * 获取地图边界信息
     * @param mapId 地图ID
     * @return 地图边界
     */
    MapBounds getMapBounds(String mapId);

    /**
     * 获取地图统计信息
     * @param mapId 地图ID
     * @return 地图统计数据
     */
    MapStatistics getMapStatistics(String mapId);

    /**
     * 清理过期缓存
     */
    void cleanupExpiredCache();

    /**
     * 清除指定地图的缓存
     * @param mapId 地图ID
     */
    void clearMapCache(String mapId);

    /**
     * 清除所有缓存
     */
    void clearAllCache();

    /**
     * 地图边界类
     */
    class MapBounds {
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;

        public MapBounds(int minX, int maxX, int minY, int maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }

        public int getMinX() { return minX; }
        public int getMaxX() { return maxX; }
        public int getMinY() { return minY; }
        public int getMaxY() { return maxY; }
        public int getWidth() { return maxX - minX; }
        public int getHeight() { return maxY - minY; }

        @Override
        public String toString() {
            return String.format("MapBounds{minX=%d, maxX=%d, minY=%d, maxY=%d, width=%d, height=%d}",
                    minX, maxX, minY, maxY, getWidth(), getHeight());
        }
    }

    /**
     * 地图统计信息类
     */
    class MapStatistics {
        private final long totalGrids;
        private final long passableGrids;
        private final long obstacleGrids;
        private final double passableRatio;
        private final int mapArea;

        public MapStatistics(long totalGrids, long passableGrids, long obstacleGrids, double passableRatio, int mapArea) {
            this.totalGrids = totalGrids;
            this.passableGrids = passableGrids;
            this.obstacleGrids = obstacleGrids;
            this.passableRatio = passableRatio;
            this.mapArea = mapArea;
        }

        public MapStatistics(long totalGrids, long passableGrids, long obstacleGrids, double passableRatio) {
            this(totalGrids, passableGrids, obstacleGrids, passableRatio, 0);
        }

        public long getTotalGrids() { return totalGrids; }
        public long getPassableGrids() { return passableGrids; }
        public long getObstacleGrids() { return obstacleGrids; }
        public double getPassableRatio() { return passableRatio; }
        public int getMapArea() { return mapArea; }

        @Override
        public String toString() {
            return String.format("MapStatistics{total=%d, passable=%d (%.1f%%), obstacles=%d, area=%d}",
                    totalGrids, passableGrids, passableRatio, obstacleGrids, mapArea);
        }
    }
}