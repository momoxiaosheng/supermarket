package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.app.request.*;
import com.example.supermarket2.dto.app.response.CartItemDto;
import com.example.supermarket2.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/app/cart")
@Tag(name = "APP购物车管理", description = "移动端购物车相关接口")
@RequiredArgsConstructor
public class AppCartController {

    private final CartService cartService;

    @GetMapping("/items")
    @Operation(summary = "获取购物车项列表", description = "获取指定用户的购物车中的所有商品")
    public Result<List<CartItemDto>> getCartItems(@Valid AppCartListQuery query) {
        List<CartItemDto> items = cartService.getCartItems(query.getUserId());
        return Result.success(items);
    }

    @GetMapping("/count")
    @Operation(summary = "获取购物车商品数量", description = "获取指定用户购物车中的商品总数")
    public Result<Integer> getCartCount(@Valid AppCartCountQuery query) {
        Integer count = cartService.getCartCount(query.getUserId());
        return Result.success(count);
    }

    @PostMapping("/items")
    @Operation(summary = "添加商品到购物车", description = "将指定数量的商品添加到用户购物车")
    public Result<String> addToCart(@Valid @RequestBody AppCartAddRequest request) {
        cartService.addToCart(request.getUserId(), request.getProductId(), request.getQuantity());
        return Result.success("添加购物车成功");
    }

    @PutMapping("/item")
    @Operation(summary = "更新购物车项", description = "更新购物车中商品的数量或选择状态")
    public Result<String> updateCartItem(@Valid @RequestBody AppCartUpdateRequest request) {
        cartService.updateCartItem(request.getUserId(), request.getProductId(), request.getQuantity(), request.getSelected());
        return Result.success("购物车更新成功");
    }

    @DeleteMapping("/item")
    @Operation(summary = "移除购物车项", description = "从购物车中移除指定商品")
    public Result<String> removeCartItem(@Valid @RequestBody AppCartDeleteRequest request) {
        cartService.removeCartItem(request.getUserId(), request.getProductId());
        return Result.success("移除购物车商品成功");
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空购物车", description = "清空指定用户的整个购物车")
    public Result<String> clearCart(@Valid @RequestBody AppCartClearRequest request) {
        cartService.clearCart(request.getUserId());
        return Result.success("购物车清空成功");
    }

    @PostMapping("/checkout")
    @Operation(summary = "购物车结算")
    public Result<Object> checkout(@RequestParam Integer userId) {
        // 这里的 Result<Object> 请替换为你实际的结算返回 DTO
        // BillDto bill = cartService.checkout(userId);
        return Result.success("结算逻辑待实现");
    }

    @GetMapping("/check-updates")
    @Operation(summary = "检查购物车是否有更新")
    public Result<Boolean> checkUpdates(@RequestParam Integer userId, @RequestParam Long lastChecked) {
        // boolean hasUpdates = cartService.checkUpdates(userId, lastChecked);
        return Result.success(false); // 模拟返回
    }
}