package com.example.supermarket2.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/banner")
@Tag(name = "ADMIN轮播图管理", description = "后台轮播图管理接口")
public class AdminBannerController {
    // 这里可以添加管理员相关的轮播图管理接口
}
