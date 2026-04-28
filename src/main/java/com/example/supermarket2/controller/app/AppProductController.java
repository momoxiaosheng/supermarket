package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.app.request.AppProductDetailQuery;
import com.example.supermarket2.dto.app.request.AppProductRecommendQuery;
import com.example.supermarket2.dto.app.response.ProductDto;
import com.example.supermarket2.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/product")
@Tag(name = "APP商品管理", description = "移动端商品相关接口")
@RequiredArgsConstructor
public class AppProductController {

    private final ProductService productService;

    /**
     * 获取推荐商品
     * 改造点：入参封装为DTO，添加limit区间校验[1,100]
     */
    @GetMapping("/recommend")
    @Operation(summary = "获取推荐商品", description = "获取推荐商品列表（限制数量1-100）")
    public Result<List<ProductDto>> getRecommendProducts(@Valid AppProductRecommendQuery query) {
        List<ProductDto> products = productService.getRecommendProducts(query.getLimit());
        return Result.success(products);
    }

    /**
     * 获取所有商品
     * 保持原有逻辑，无入参无需改造
     */
    @GetMapping("/list")
    @Operation(summary = "获取所有商品", description = "获取所有上架商品列表")
    public Result<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return Result.success(products);
    }

    /**
     * 获取商品详情
     * 改造点：
     * 1. @PathVariable id封装为DTO
     * 2. 添加非空+正整数校验
     * 3. 校验失败由全局异常处理器统一返回
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详细信息")
    public Result<ProductDto> getProductDetail(@Valid AppProductDetailQuery query) {
        ProductDto product = productService.getProductById(query.getId());
        return Result.success(product);
    }
}