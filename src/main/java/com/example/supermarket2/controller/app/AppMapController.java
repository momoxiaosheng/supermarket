package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.exception.BusinessException;
import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.app.request.AppMapDataQuery;
import com.example.supermarket2.dto.app.request.AppMapPassableCheckQuery;
import com.example.supermarket2.entity.location.MapGrid;
import com.example.supermarket2.common.result.ErrorCode;
import com.example.supermarket2.service.location.MapGridService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated // 类级别校验注解，配合方法参数@Valid使用
@RestController
@RequestMapping("/app/map")
@Tag(name = "APP地图管理", description = "移动端地图数据相关接口")
@RequiredArgsConstructor
public class AppMapController {

    private final MapGridService mapGridService;

    /**
     * 获取地图网格数据
     * 改造点：入参封装为DTO，添加mapId长度校验
     */
    @GetMapping("/data")
    @Operation(summary = "获取地图网格数据", description = "根据地图ID获取完整地图网格数据")
    public Result<List<MapGrid>> getMapData(@Valid AppMapDataQuery query) {
        log.debug("请求地图数据: mapId={}", query.getMapId());
        List<MapGrid> gridList = mapGridService.getMapGridByMapId(query.getMapId());
        if (gridList.isEmpty()) {
            log.warn("未找到地图数据: mapId={}", query.getMapId());
            throw new BusinessException(ErrorCode.MAP_NOT_FOUND);
        }
        log.debug("返回地图数据: mapId={}, 网格数量={}", query.getMapId(), gridList.size());
        return Result.success(gridList);
    }

    /**
     * 获取可通行地图数据
     * 改造点：入参封装为DTO，添加mapId长度校验
     */
    @GetMapping("/passable")
    @Operation(summary = "获取可通行地图数据", description = "根据地图ID获取可通行的网格数据")
    public Result<List<MapGrid>> getPassableMapData(@Valid AppMapDataQuery query) {
        log.debug("请求可通行地图数据: mapId={}", query.getMapId());
        List<MapGrid> gridList = mapGridService.getMapGridByMapId(query.getMapId());
        // 过滤可通行网格
        List<MapGrid> passableGrids = gridList.stream()
                .filter(grid -> grid.getPassable() == 1)
                .toList();
        if (passableGrids.isEmpty()) {
            log.warn("未找到可通行地图数据: mapId={}", query.getMapId());
            throw new BusinessException(ErrorCode.NO_PASSABLE_PATH);
        }
        log.debug("返回可通行地图数据: mapId={}, 可通行网格数量={}", query.getMapId(), passableGrids.size());
        return Result.success(passableGrids);
    }

    /**
     * 检查位置是否可通行
     * 改造点：入参封装为DTO，添加mapId长度校验+坐标范围校验
     */
    @GetMapping("/check-passable")
    @Operation(summary = "检查位置是否可通行", description = "校验指定坐标在对应地图中是否可通行")
    public Result<Boolean> checkPositionPassable(@Valid AppMapPassableCheckQuery query) {
        log.debug("检查位置可通行性: mapId={}, position=({}, {})", query.getMapId(), query.getX(), query.getY());
        boolean isPassable = mapGridService.isPositionPassable(query.getMapId(), query.getX(), query.getY());
        log.debug("位置可通行性检查结果: mapId={}, position=({}, {}), passable={}",
                query.getMapId(), query.getX(), query.getY(), isPassable);
        return Result.success(isPassable);
    }
}