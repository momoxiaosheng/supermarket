package com.example.supermarket2.service;

import com.example.supermarket2.entity.MapData;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface MapService {

    // 根据ID获取地图及关联的GeoJSON
    MapData getMapWithGeoJson(Long id);

    // 获取所有已发布地图
    List<MapData> getPublishedMaps();

    // 获取所有地图（含未发布）
    List<MapData> getAllMaps();

    // 保存地图GeoJSON数据
    boolean saveMapData(Long id, String geoJson);

    // 创建地图并保存底图
    boolean createMapWithImage(MapData mapData, MultipartFile baseImage);

    // 更新地图基础信息
    boolean updateById(MapData mapData);

    // 发布地图（设置状态为已发布）
    boolean publishMap(Long id);

    // 取消发布地图（设置状态为未发布）
    boolean unpublishMap(Long id);

    // 删除地图及关联数据
    boolean deleteMap(Long id);

    // 更新地图底图
    boolean updateBackgroundImage(Long id, MultipartFile backgroundImage);
}