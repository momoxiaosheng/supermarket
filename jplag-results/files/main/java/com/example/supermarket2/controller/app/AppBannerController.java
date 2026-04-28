package com.example.supermarket2.controller.app;

import com.example.supermarket2.dto.app.BannerDto;
import com.example.supermarket2.service.BannerService;
import com.example.supermarket2.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/app/banner")
@Tag(name = "APP轮播图管理", description = "移动端轮播图相关接口")
public class AppBannerController {

    @Autowired
    private BannerService bannerService;

    @GetMapping("/list")
    @Operation(summary = "获取轮播图列表", description = "获取所有激活状态的轮播图")
    public Object list() {
        List<BannerDto> banners = bannerService.getActiveBanners();
        return ResponseUtil.ok(banners);
    }
}