package com.example.supermarket2.controller.admin;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/category")
@Tag(name = "ADMIN分类管理", description = "后台分类管理接口")
public class AdminCategoryController {
    // 这里可以添加管理员相关的分类管理接口
}