package com.example.supermarket2.mapper.location;

import com.example.supermarket2.entity.location.MapGrid;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 地图网格数据访问接口
 * MyBatis映射，查询地图网格信息
 */
@Mapper
public interface MapGridMapper {

    /**
     * 根据地图ID查询网格数据
     */
    @Select("SELECT id, map_id, x, y, color, passable " +
            "FROM map_grid " +
            "WHERE map_id = #{mapId} " +
            "ORDER BY y, x")
    List<MapGrid> getMapGridByMapId(@Param("mapId") String mapId);

    /**
     * 查询指定范围内的网格数据
     */
    @Select("SELECT id, map_id, x, y, color, passable " +
            "FROM map_grid " +
            "WHERE map_id = #{mapId} " +
            "AND x BETWEEN #{minX} AND #{maxX} " +
            "AND y BETWEEN #{minY} AND #{maxY} " +
            "ORDER BY y, x")
    List<MapGrid> getMapGridByArea(@Param("mapId") String mapId,
                                   @Param("minX") Integer minX,
                                   @Param("maxX") Integer maxX,
                                   @Param("minY") Integer minY,
                                   @Param("maxY") Integer maxY);

    /**
     * 查询可通行网格数据
     */
    @Select("SELECT id, map_id, x, y, color, passable " +
            "FROM map_grid " +
            "WHERE map_id = #{mapId} AND passable = 1 " +
            "ORDER BY y, x")
    List<MapGrid> getPassableMapGrid(@Param("mapId") String mapId);

    /**
     * 查询地图边界信息
     */
    @Select("SELECT " +
            "MIN(x) as min_x, MAX(x) as max_x, " +
            "MIN(y) as min_y, MAX(y) as max_y, " +
            "COUNT(*) as total_grids " +
            "FROM map_grid " +
            "WHERE map_id = #{mapId}")
    MapGridBounds getMapBounds(@Param("mapId") String mapId);

    /**
     * 检查指定位置是否可通行
     */
    @Select("SELECT COUNT(*) " +
            "FROM map_grid " +
            "WHERE map_id = #{mapId} AND x = #{x} AND y = #{y} AND passable = 1")
    int isPositionPassable(@Param("mapId") String mapId,
                           @Param("x") Integer x,
                           @Param("y") Integer y);

    /**
     * 地图边界信息类
     */
    class MapGridBounds {
        private Integer minX;
        private Integer maxX;
        private Integer minY;
        private Integer maxY;
        private Integer totalGrids;

        // getters and setters
        public Integer getMinX() { return minX; }
        public void setMinX(Integer minX) { this.minX = minX; }

        public Integer getMaxX() { return maxX; }
        public void setMaxX(Integer maxX) { this.maxX = maxX; }

        public Integer getMinY() { return minY; }
        public void setMinY(Integer minY) { this.minY = minY; }

        public Integer getMaxY() { return maxY; }
        public void setMaxY(Integer maxY) { this.maxY = maxY; }

        public Integer getTotalGrids() { return totalGrids; }
        public void setTotalGrids(Integer totalGrids) { this.totalGrids = totalGrids; }

        /**
         * 获取地图宽度
         */
        public Integer getWidth() {
            return maxX != null && minX != null ? maxX - minX + 1 : 0;
        }

        /**
         * 获取地图高度
         */
        public Integer getHeight() {
            return maxY != null && minY != null ? maxY - minY + 1 : 0;
        }
    }
}