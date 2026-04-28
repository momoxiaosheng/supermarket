package com.example.supermarket2.controller.admin;

import com.example.supermarket2.entity.MapData;
import com.example.supermarket2.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/map")
@Tag(name = "ADMIN地图管理", description = "后台地图数据管理接口（创建、更新、发布）")
public class AdminMapController {

    @Autowired
    private MapService mapService;

    @GetMapping("/{id}")
    @Operation(summary = "查询地图详情", description = "根据ID获取地图数据及关联的GeoJSON信息")
    public MapData getMap(
            @Parameter(description = "地图ID", required = true)
            @PathVariable Long id) {
        return mapService.getMapWithGeoJson(id);
    }

    @GetMapping("/published")
    @Operation(summary = "查询已发布地图", description = "获取所有已发布状态的地图列表")
    public List<MapData> getPublishedMaps() {
        return mapService.getPublishedMaps();
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有地图", description = "获取所有地图列表（含未发布）")
    public List<MapData> getAllMaps() {
        return mapService.getAllMaps();
    }

    @PostMapping("/{id}/data")
    @Operation(summary = "保存地图GeoJSON数据", description = "更新地图关联的地理信息数据（GeoJSON格式）")
    public boolean saveMapData(
            @Parameter(description = "地图ID", required = true) @PathVariable Long id,
            @Parameter(description = "GeoJSON格式的地理数据", required = true) @RequestBody String geoJson) {
        return mapService.saveMapData(id, geoJson);
    }

    @PostMapping
    @Operation(summary = "创建地图", description = "初始化一个新地图（含基础信息）")
    public ResponseEntity<?> createMap(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "floor", defaultValue = "1") int floor,
            @RequestParam("baseImage") MultipartFile baseImage) {
        try {
            MapData mapData = new MapData();
            mapData.setName(name);
            mapData.setDescription(description);
            mapData.setFloor(floor);
            mapData.setVersion("1.0.0");
            mapData.setStatus(0); // 默认未发布

            boolean result = mapService.createMapWithImage(mapData, baseImage);
            if (result) {
                return ResponseEntity.ok().body("地图创建成功");
            } else {
                return ResponseEntity.badRequest().body("地图创建失败");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("服务器错误: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新地图基础信息", description = "修改地图名称、描述等基础属性")
    public boolean updateMap(
            @Parameter(description = "地图ID", required = true) @PathVariable Long id,
            @Parameter(description = "更新后的地图信息", required = true) @RequestBody MapData mapData) {
        mapData.setId(id);
        return mapService.updateById(mapData);
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布地图", description = "将地图状态设置为已发布")
    public boolean publishMap(
            @Parameter(description = "地图ID", required = true) @PathVariable Long id) {
        return mapService.publishMap(id);
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "取消发布地图", description = "将地图状态设置为未发布")
    public boolean unpublishMap(
            @Parameter(description = "地图ID", required = true) @PathVariable Long id) {
        return mapService.unpublishMap(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地图", description = "删除指定地图及关联的地理数据")
    public boolean deleteMap(
            @Parameter(description = "地图ID", required = true) @PathVariable Long id) {
        return mapService.deleteMap(id);
    }

    @PostMapping("/{id}/background")
    @Operation(summary = "更新地图底图", description = "更新地图的底图图片")
    public ResponseEntity<?> updateBackground(
            @Parameter(description = "地图ID", required = true) @PathVariable Long id,
            @RequestParam("backgroundImage") MultipartFile backgroundImage) {
        try {
            boolean result = mapService.updateBackgroundImage(id, backgroundImage);
            if (result) {
                return ResponseEntity.ok().body("底图更新成功");
            } else {
                return ResponseEntity.badRequest().body("底图更新失败");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("服务器错误: " + e.getMessage());
        }
    }
}