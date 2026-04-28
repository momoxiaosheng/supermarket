package com.example.supermarket2.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.supermarket2.entity.MapData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MapDataMapper extends BaseMapper<MapData> {

    @Select("SELECT md.*, " +
            "CONCAT('{', '\"type\": \"FeatureCollection\", ', '\"features\": [', " +
            "GROUP_CONCAT(CONCAT('{', '\"type\": \"Feature\", ', " +
            "'\"properties\": ', IFNULL(mf.properties, '{}'), ', ', " +
            "'\"geometry\": ', mf.geometry, '}')), ']}') AS geoJson " +
            "FROM map_data md " +
            "LEFT JOIN map_features mf ON md.id = mf.map_id " +
            "WHERE md.id = #{id} " +
            "GROUP BY md.id")
    MapData selectWithGeoJson(Long id);

    @Select("SELECT md.* FROM map_data md WHERE md.status = 1")
    List<MapData> selectPublishedMaps();
}