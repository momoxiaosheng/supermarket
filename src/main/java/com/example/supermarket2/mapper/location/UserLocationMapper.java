package com.example.supermarket2.mapper.location;

import com.example.supermarket2.entity.location.UserLocation;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户位置数据访问接口
 * MyBatis映射，操作用户位置信息
 */
@Mapper
public interface UserLocationMapper {

    /**
     * 新增或更新用户位置（基于 user_id + map_id 唯一键）
     */
    @Insert("INSERT INTO user_location (user_id, map_id, x, y, timestamp) " +
            "VALUES (#{userId}, #{mapId}, #{x}, #{y}, #{timeStamp}) " +
            "ON DUPLICATE KEY UPDATE " +
            "x = #{x}, " +
            "y = #{y}, " +
            "timestamp = #{timeStamp}")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsertUserLocation(UserLocation location);

    /**
     * 查询用户最新位置
     */
    @Select("SELECT id, user_id as userId, map_id as mapId, x, y, timestamp " +
            "FROM user_location " +
            "WHERE user_id = #{userId} AND map_id = #{mapId} " +
            "ORDER BY timestamp DESC " +
            "LIMIT 1")
    UserLocation getLatestUserLocation(@Param("userId") String userId,
                                       @Param("mapId") String mapId);

    /**
     * 查询用户位置历史
     */
    @Select("SELECT id, user_id as userId, map_id as mapId, x, y, timestamp " +
            "FROM user_location " +
            "WHERE user_id = #{userId} AND map_id = #{mapId} " +
            "ORDER BY timestamp DESC " +
            "LIMIT #{limit}")
    List<UserLocation> getUserLocationHistory(@Param("userId") String userId,
                                              @Param("mapId") String mapId,
                                              @Param("limit") Integer limit);

    /**
     * 删除过期的用户位置记录
     */
    @Delete("DELETE FROM user_location " +
            "WHERE timestamp < DATE_SUB(NOW(), INTERVAL #{expireHours} HOUR)")
    int deleteExpiredLocations(@Param("expireHours") Integer expireHours);

    /**
     * 查询地图上的所有用户位置
     */
    @Select("SELECT id, user_id as userId, map_id as mapId, x, y, timestamp " +
            "FROM user_location " +
            "WHERE map_id = #{mapId} " +
            "AND timestamp > DATE_SUB(NOW(), INTERVAL 5 MINUTE) " +
            "ORDER BY timestamp DESC")
    List<UserLocation> getRecentUserLocationsByMap(@Param("mapId") String mapId);

    /**
     * 统计用户位置更新频率
     */
    @Select("SELECT COUNT(*) as update_count, " +
            "AVG(TIMESTAMPDIFF(SECOND, lag_timestamp, timestamp)) as avg_interval " +
            "FROM ( " +
            "    SELECT timestamp, " +
            "           LAG(timestamp) OVER (PARTITION BY user_id, map_id ORDER BY timestamp) as lag_timestamp " +
            "    FROM user_location " +
            "    WHERE user_id = #{userId} AND map_id = #{mapId} " +
            "    AND timestamp > DATE_SUB(NOW(), INTERVAL 1 HOUR) " +
            ") as t " +
            "WHERE lag_timestamp IS NOT NULL")
    LocationUpdateStats getLocationUpdateStats(@Param("userId") String userId,
                                               @Param("mapId") String mapId);

    /**
     * 位置更新统计类
     */
    class LocationUpdateStats {
        private Integer updateCount;
        private Double avgInterval;

        // getters and setters
        public Integer getUpdateCount() { return updateCount; }
        public void setUpdateCount(Integer updateCount) { this.updateCount = updateCount; }

        public Double getAvgInterval() { return avgInterval; }
        public void setAvgInterval(Double avgInterval) { this.avgInterval = avgInterval; }

        /**
         * 获取更新频率（次/分钟）
         */
        public Double getUpdatesPerMinute() {
            return avgInterval != null && avgInterval > 0 ? 60.0 / avgInterval : 0.0;
        }
    }

    /**
     * 批量插入用户位置（性能优化）
     */
    @Insert("<script>" +
            "INSERT INTO user_location (user_id, map_id, x, y, timestamp) VALUES " +
            "<foreach collection='locations' item='location' separator=','>" +
            "(#{location.userId}, #{location.mapId}, #{location.x}, #{location.y}, #{location.timeStamp})" +
            "</foreach>" +
            "ON DUPLICATE KEY UPDATE " +
            "x = VALUES(x), y = VALUES(y), timestamp = VALUES(timestamp)" +
            "</script>")
    int batchUpsertUserLocations(@Param("locations") List<UserLocation> locations);
}
