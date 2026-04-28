package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.app.response.CategoryDto;
import com.example.supermarket2.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/app/category")
@Tag(name = "APP分类管理", description = "移动端商品分类相关接口")
@RequiredArgsConstructor
public class AppCategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "获取一级根分类列表", description = "获取所有parent_id=0且状态为「启用」、按排序号升序排列的一级分类")
    public Result<List<CategoryDto>> list() {
        log.debug("请求获取一级根分类列表");
        List<CategoryDto> categories = categoryService.getRootCategories();
        log.debug("返回一级根分类列表，数量={}", categories.size());
        return Result.success(categories);
    }
}