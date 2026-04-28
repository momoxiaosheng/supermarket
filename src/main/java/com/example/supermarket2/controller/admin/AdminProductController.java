package com.example.supermarket2.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/product")
@Tag(name = "ADMIN商品管理", description = "后台商品管理接口")
public class AdminProductController {
    // 这里可以添加管理员相关的商品管理接口
}
