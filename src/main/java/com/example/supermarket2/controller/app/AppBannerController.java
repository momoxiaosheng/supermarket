package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.app.response.BannerDto;
import com.example.supermarket2.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/app/banner")
@Tag(name = "APP轮播图管理", description = "移动端首页/活动页轮播图相关接口")
@RequiredArgsConstructor
public class AppBannerController {

    private final BannerService bannerService;

    @GetMapping("/list")
    @Operation(summary = "获取激活轮播图列表", description = "获取所有状态为「激活」且按排序号升序排列的轮播图")
    public Result<List<BannerDto>> list() {
        log.debug("请求获取激活轮播图列表");
        List<BannerDto> banners = bannerService.getActiveBanners();
        log.debug("返回激活轮播图列表，数量={}", banners.size());
        return Result.success(banners);
    }
}