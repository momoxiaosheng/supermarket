package com.example.supermarket2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.supermarket2.entity.MapFeature;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MapFeatureMapper extends BaseMapper<MapFeature> {

    /**
     * 根据地图ID删除所有相关要素
     * @param mapId 地图ID
     * @return 删除的记录数
     */
    @Delete("DELETE FROM map_features WHERE map_id = #{mapId}")
    int deleteByMapId(@Param("mapId") Long mapId);

    /**
     * 插入地图要素
     * 由于继承了BaseMapper，insert方法已经存在，无需额外定义
     * 这里为了清晰，可以添加自定义的插入方法（如果需要特殊处理）
     */

    /**
     * 批量插入地图要素
     * @param features 要素列表
     * @return 插入的记录数
     */
    // 如果需要批量插入，可以添加此方法
    // int batchInsert(@Param("list") List<MapFeature> features);
}