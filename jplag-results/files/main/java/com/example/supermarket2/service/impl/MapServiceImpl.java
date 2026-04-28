package com.example.supermarket2.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.supermarket2.entity.MapData;
import com.example.supermarket2.entity.MapFeature;
import com.example.supermarket2.mapper.MapDataMapper;
import com.example.supermarket2.mapper.MapFeatureMapper;
import com.example.supermarket2.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class MapServiceImpl implements MapService {

    @Autowired
    private MapDataMapper mapDataMapper;

    @Autowired
    private MapFeatureMapper mapFeatureMapper;


    @Override
    public MapData getMapWithGeoJson(Long id) {
        return mapDataMapper.selectWithGeoJson(id);
    }

    @Override
    public List<MapData> getPublishedMaps() {
        return mapDataMapper.selectPublishedMaps();
    }

    @Override
    public List<MapData> getAllMaps() {
        // 查询所有地图（MyBatis-Plus自带方法）
        return mapDataMapper.selectList(null);
    }

    @Override
    public boolean saveMapData(Long id, String geoJson) {
        // 实际项目中需要解析geoJson并保存到map_features表
        // 这里仅为示例，需根据实际GeoJSON格式实现解析逻辑
        return true;
    }

    @Override
    public boolean createMapWithImage(MapData mapData, MultipartFile baseImage) {
        try {
            // 保存图片并设置thumbnail路径
            String fileName = saveImage(baseImage);
            mapData.setThumbnail(fileName);
            // 插入地图数据
            return mapDataMapper.insert(mapData) > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean updateById(MapData mapData) {
        return mapDataMapper.updateById(mapData) > 0;
    }

    @Override
    public boolean publishMap(Long id) {
        MapData mapData = new MapData();
        mapData.setId(id);
        mapData.setStatus(1); // 1表示已发布
        return mapDataMapper.updateById(mapData) > 0;
    }

    @Override
    public boolean unpublishMap(Long id) {
        MapData mapData = new MapData();
        mapData.setId(id);
        mapData.setStatus(0); // 0表示未发布
        return mapDataMapper.updateById(mapData) > 0;
    }

    @Override
    public boolean deleteMap(Long id) {
        // 先删除关联的要素
        mapFeatureMapper.deleteByMapId(id);
        // 再删除地图本身
        return mapDataMapper.deleteById(id) > 0;
    }

    @Override
    public boolean updateBackgroundImage(Long id, MultipartFile backgroundImage) {
        try {
            // 保存新图片并更新thumbnail路径
            String fileName = saveImage(backgroundImage);
            MapData mapData = new MapData();
            mapData.setId(id);
            mapData.setThumbnail(fileName);
            return mapDataMapper.updateById(mapData) > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 工具方法：保存图片到本地
    private String saveImage(MultipartFile file) throws IOException {
        // 创建上传目录（如果不存在）
        Path uploadDir = Paths.get(IMAGE_UPLOAD_PATH);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        // 生成唯一文件名（避免重名）
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + extension;
        // 保存文件
        Path filePath = uploadDir.resolve(fileName);
        Files.write(filePath, file.getBytes());
        return fileName;
    }
}