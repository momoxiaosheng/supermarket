package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.exception.BusinessException;
import com.example.supermarket2.common.result.ErrorCode;
import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.app.request.AppLocationCurrentQuery;
import com.example.supermarket2.dto.app.request.AppLocationHistoryQuery;
import com.example.supermarket2.dto.location.BeaconRequest;
import com.example.supermarket2.dto.location.LocationResult;
import com.example.supermarket2.entity.location.UserLocation;
import com.example.supermarket2.service.location.BluetoothLocationService;
import com.example.supermarket2.service.location.UserLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 定位接口控制器
 * 整合蓝牙定位和位置查询功能
 */
@Slf4j
@Validated // 类级别校验注解，配合方法参数@Valid使用
@RestController
@RequestMapping("/app/location")
@Tag(name = "APP定位管理", description = "移动端室内蓝牙定位相关接口")
@RequiredArgsConstructor // 替换@Autowired，使用构造器注入（企业级最佳实践）
public class AppLocationController {

    private final BluetoothLocationService bluetoothLocationService;
    private final UserLocationService userLocationService;

    /**
     * 蓝牙定位接口
     * 处理蓝牙信标数据并返回定位结果
     * 【原有合规接口，保留不变】
     */
    @PostMapping("/bluetooth")
    @Operation(summary = "蓝牙定位", description = "上传蓝牙信标数据，计算并返回用户位置")
    public Result<LocationResult> bluetoothLocation(@RequestBody @Valid BeaconRequest request) {
        log.info("收到蓝牙定位请求: userId={}, mapId={}, 信标数量={}",
                request.getUserId(), request.getMapId(),
                request.getBeacons() != null ? request.getBeacons().size() : 0);
        // 1. 处理定位请求（Service层若出错直接抛出BusinessException）
        LocationResult result = bluetoothLocationService.processBeaconData(request);
        // 2. 业务逻辑校验：定位失败则抛出异常
        if(!result.isSuccess()) {
            throw new BusinessException(ErrorCode.LOCATION_FAILED, result.getMessage());
        }
        if(!result.isValidPosition()){
            throw new BusinessException(ErrorCode.LOCATION_FAILED, "定位坐标无效");
        }
        // 3. 更新用户位置到数据库
        updateUserLocation(request, result);
        log.debug("定位成功并更新用户位置: ({}, {})", result.getX(), result.getY());
        return Result.success(result);
    }

    /**
     * 获取用户当前位置
     * 【改造点】：入参封装为DTO，添加userId/mapId长度校验
     */
    @GetMapping("/current")
    @Operation(summary = "获取当前位置", description = "查询用户最新的定位记录")
    public Result<UserLocation> getCurrentLocation(@Valid AppLocationCurrentQuery query) {
        log.debug("查询用户当前位置: userId={}, mapId={}", query.getUserId(), query.getMapId());
        UserLocation location = userLocationService.getLatestUserLocation(query.getUserId(), query.getMapId());
        if (location == null) {
            throw new BusinessException(ErrorCode.USER_LOCATION_NOT_FOUND);
        }
        return Result.success(location);
    }

    /**
     * 更新用户位置（原UserLocationController功能）
     * 【原有合规接口，保留不变】
     */
    @PostMapping("/update")
    @Operation(summary = "更新位置", description = "手动更新用户位置信息")
    public Result<String> updateUserLocation(@RequestBody @Valid UserLocation location) {
        log.debug("更新用户位置: userId={}, mapId={}, position=({}, {})",
                location.getUserId(), location.getMapId(), location.getX(), location.getY());
        // 设置时间戳
        if (location.getTimeStamp() == null) {
            location.setTimeStamp(new Date());
        }
        userLocationService.updateUserLocation(location);
        return Result.success("用户位置更新成功");
    }

    /**
     * 获取用户位置历史
     * 【改造点】：入参封装为DTO，添加userId/mapId长度校验+limit范围校验
     */
    @GetMapping("/history")
    @Operation(summary = "获取位置历史", description = "查询用户的历史定位轨迹")
    public Result<Object> getLocationHistory(@Valid AppLocationHistoryQuery query) {
        log.debug("查询用户位置历史: userId={}, mapId={}, limit={}", query.getUserId(), query.getMapId(), query.getLimit());
        List<UserLocation> history = userLocationService.getUserLocationHistory(
                query.getUserId(), query.getMapId(), query.getLimit());
        return Result.success(history);
    }

    /**
     * 更新用户位置到数据库
     * 【私有方法，保留原有逻辑】
     */
    private void updateUserLocation(BeaconRequest request, LocationResult result) {
        UserLocation userLocation = new UserLocation();
        userLocation.setUserId(request.getUserId());
        userLocation.setMapId(request.getMapId());
        userLocation.setX(result.getX().intValue());
        userLocation.setY(result.getY().intValue());
        userLocation.setTimeStamp(new Date());
        userLocationService.updateUserLocation(userLocation);
    }
}