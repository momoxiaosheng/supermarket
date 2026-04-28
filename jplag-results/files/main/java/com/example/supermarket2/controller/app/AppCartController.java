package com.example.supermarket2.controller.app;

import com.example.supermarket2.dto.app.CartItemDto;
import com.example.supermarket2.service.CartService;
import com.example.supermarket2.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/app/cart")
@Tag(name = "APP购物车管理", description = "移动端购物车相关接口")
public class AppCartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/list")
    @Operation(summary = "获取购物车项列表", description = "获取指定用户的购物车中的所有商品")
    public Object getCartItems(@RequestParam Long userId) {
        List<CartItemDto> items = cartService.getCartItems(userId);
        return ResponseUtil.ok(items);
    }

    @GetMapping("/count")
    @Operation(summary = "获取购物车商品数量", description = "获取指定用户购物车中的商品总数")
    public Object getCartCount(@RequestParam Long userId) {
        Integer count = cartService.getCartCount(userId);
        return ResponseUtil.ok(count);
    }

    @PostMapping("/add")
    @Operation(summary = "添加商品到购物车", description = "将指定数量的商品添加到用户购物车")
    public Object addToCart(@RequestParam Long userId,
                            @RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.addToCart(userId, productId, quantity);
        return ResponseUtil.ok();
    }

    @PutMapping("/update")
    @Operation(summary = "更新购物车项", description = "更新购物车中商品的数量或选择状态")
    public Object updateCartItem(@RequestParam Long userId,
                                 @RequestParam Long itemId,
                                 @RequestParam(required = false) Integer quantity,
                                 @RequestParam(required = false) Boolean selected) {
        cartService.updateCartItem(userId, itemId, quantity, selected);
        return ResponseUtil.ok();
    }

    @DeleteMapping("/remove")
    @Operation(summary = "移除购物车项", description = "从购物车中移除指定商品")
    public Object removeCartItem(@RequestParam Long userId, @RequestParam Long itemId) {
        cartService.removeCartItem(userId, itemId);
        return ResponseUtil.ok();
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空购物车", description = "清空指定用户的整个购物车")
    public Object clearCart(@RequestParam Long userId) {
        cartService.clearCart(userId);
        return ResponseUtil.ok();
    }
}
