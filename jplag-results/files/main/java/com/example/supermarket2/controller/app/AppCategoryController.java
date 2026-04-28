package com.example.supermarket2.controller.app;


import com.example.supermarket2.dto.app.CategoryDto;
import com.example.supermarket2.service.CategoryService;
import com.example.supermarket2.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/app/category")
@Tag(name = "APP分类管理", description = "移动端商品分类相关接口")
public class AppCategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "获取根分类列表", description = "获取所有一级分类（parent_id=0）")
    public Object list() {
        List<CategoryDto> categories = categoryService.getRootCategories();
        return ResponseUtil.ok(categories);
    }
}