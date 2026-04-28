package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.exception.BusinessException;
import com.example.supermarket2.common.result.ErrorCode;
import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.location.NavigationRequest;
import com.example.supermarket2.dto.location.NavigationResponse;
import com.example.supermarket2.service.location.NavigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 导航接口控制器
 * 处理室内路径规划请求
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/app/navigation") // 统一APP前缀
@Tag(name = "APP导航管理", description = "移动端室内路径规划相关接口")
@RequiredArgsConstructor
public class AppNavigationController {

    private final NavigationService navigationService;

    /**
     * 增强路径规划接口
     * 计算从起点到终点的最优路径，支持不可通行目标点自动调整到最近可通行点
     */
    @PostMapping("/path")
    @Operation(summary = "增强路径规划", description = "计算从起点到终点的最优路径，支持不可通行目标点自动调整到最近可通行点")
    public Result<NavigationResponse> calculatePath(@RequestBody @Valid NavigationRequest request) {
        log.info("收到增强路径规划请求: mapId={}, start=({}, {}), end=({}, {})",
                request.getMapId(), request.getStartX(), request.getStartY(),
                request.getEndX(), request.getEndY());

        // 执行增强路径规划
        NavigationResponse response = navigationService.calculateEnhancedPath(request);

        if (response.isSuccess()) {
            log.info("增强路径规划成功：路径长度={}, 调整目标={}",
                    response.getDistance(), response.getTargetAdjusted());
            return Result.success(response);
        } else {
            log.warn("增强路径规划失败：{}", response.getMessage());
            throw new BusinessException(ErrorCode.PATH_PLAN_FAILED, response.getMessage());
        }
    }
}