package com.example.supermarket2.controller.app;

import com.example.supermarket2.dto.app.ProductDto;
import com.example.supermarket2.service.ProductService;
import com.example.supermarket2.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/app/product")
@Tag(name = "APP商品管理", description = "移动端商品相关接口")
public class AppProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/recommend")
    @Operation(summary = "获取推荐商品", description = "获取推荐商品列表")
    public Object getRecommendProducts(@RequestParam(defaultValue = "5") int limit) {
        List<ProductDto> products = productService.getRecommendProducts(limit);
        return ResponseUtil.ok(products);
    }

    @GetMapping("/list")
    @Operation(summary = "获取所有商品", description = "获取所有上架商品列表")
    public Object getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseUtil.ok(products);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详细信息")
    public Object getProductDetail(@PathVariable Long id) {
        ProductDto product = productService.getProductById(id);
        return ResponseUtil.ok(product);
    }
}
