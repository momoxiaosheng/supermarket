package com.example.supermarket2.controller.admin;

import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.admin.MapCreateDTO;
import com.example.supermarket2.dto.admin.MapUpdateDTO;
import com.example.supermarket2.entity.MapData;
import com.example.supermarket2.service.MapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/admin/map")
@Tag(name = "ADMIN地图管理", description = "后台地图数据管理接口（创建、更新、发布）")
@RequiredArgsConstructor
public class AdminMapController {

    private final MapService mapService;

    @GetMapping("/{id}")
    @Operation(summary = "查询地图详情", description = "根据ID获取地图数据及关联的GeoJSON信息")
    public Result<MapData> getMap(
            @Parameter(description = "地图ID", required = true)
            @PathVariable @NotNull(message = "地图ID不能为空") Long id) {
        MapData mapData = mapService.getMapWithGeoJson(id);
        return Result.success(mapData);
    }

    @GetMapping("/published")
    @Operation(summary = "查询已发布地图", description = "获取所有已发布状态的地图列表")
    public Result<List<MapData>> getPublishedMaps() {
        List<MapData> list = mapService.getPublishedMaps();
        return Result.success(list);
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有地图", description = "获取所有地图列表（含未发布）")
    public Result<List<MapData>> getAllMaps() {
        List<MapData> list = mapService.getAllMaps();
        return Result.success(list);
    }

    @PostMapping("/{id}/data")
    @Operation(summary = "保存地图GeoJSON数据", description = "更新地图关联的地理信息数据（GeoJSON格式）")
    public Result<Boolean> saveMapData(
            @Parameter(description = "地图ID", required = true)
            @PathVariable @NotNull(message = "地图ID不能为空") Long id,
            @Parameter(description = "GeoJSON格式的地理数据", required = true)
            @RequestBody @NotBlank(message = "GeoJSON数据不能为空") String geoJson) {
        boolean result = mapService.saveMapData(id, geoJson);
        return Result.success(result ? "地图数据保存成功" : "地图数据保存失败", result);
    }

    @PostMapping
    @Operation(summary = "创建地图", description = "初始化一个新地图（含基础信息）")
    public Result<Boolean> createMap(@Valid MapCreateDTO dto) {
        MapData mapData = new MapData();
        mapData.setName(dto.getName());
        mapData.setDescription(dto.getDescription());
        mapData.setFloor(dto.getFloor());
        mapData.setVersion("1.0.0");
        mapData.setStatus(0); // 默认未发布
        boolean result = mapService.createMapWithImage(mapData, dto.getBaseImage());
        return Result.success(result ? "地图创建成功" : "地图创建失败", result);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新地图基础信息", description = "修改地图名称、描述等基础属性")
    public Result<Boolean> updateMap(
            @Parameter(description = "地图ID", required = true)
            @PathVariable @NotNull(message = "地图ID不能为空") Long id,
            @RequestBody @Valid MapUpdateDTO dto) {
        MapData mapData = new MapData();
        mapData.setId(id);
        mapData.setName(dto.getName());
        mapData.setDescription(dto.getDescription());
        mapData.setFloor(dto.getFloor());
        mapData.setVersion(dto.getVersion());
        mapData.setStatus(dto.getStatus());
        boolean result = mapService.updateById(mapData);
        return Result.success(result ? "地图更新成功" : "地图更新失败", result);
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "发布地图", description = "将地图状态设置为已发布")
    public Result<Boolean> publishMap(
            @Parameter(description = "地图ID", required = true)
            @PathVariable @NotNull(message = "地图ID不能为空") Long id) {
        boolean result = mapService.publishMap(id);
        return Result.success(result ? "地图发布成功" : "地图发布失败", result);
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "取消发布地图", description = "将地图状态设置为未发布")
    public Result<Boolean> unpublishMap(
            @Parameter(description = "地图ID", required = true)
            @PathVariable @NotNull(message = "地图ID不能为空") Long id) {
        boolean result = mapService.unpublishMap(id);
        return Result.success(result ? "地图取消发布成功" : "地图取消发布失败", result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除地图", description = "删除指定地图及关联的地理数据")
    public Result<Boolean> deleteMap(
            @Parameter(description = "地图ID", required = true)
            @PathVariable @NotNull(message = "地图ID不能为空") Long id) {
        boolean result = mapService.deleteMap(id);
        return Result.success(result ? "地图删除成功" : "地图删除失败", result);
    }

    @PostMapping("/{id}/background")
    @Operation(summary = "更新地图底图", description = "更新地图的底图图片")
    public Result<Boolean> updateBackground(
            @Parameter(description = "地图ID", required = true)
            @PathVariable @NotNull(message = "地图ID不能为空") Long id,
            @RequestParam("backgroundImage") @NotNull(message = "底图图片不能为空") MultipartFile backgroundImage) {
        boolean result = mapService.updateBackgroundImage(id, backgroundImage);
        return Result.success(result ? "底图更新成功" : "底图更新失败", result);
    }
}