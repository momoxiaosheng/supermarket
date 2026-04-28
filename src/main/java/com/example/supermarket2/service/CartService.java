package com.example.supermarket2.service;


import com.example.supermarket2.dto.app.response.CartItemDto;

import java.util.List;

public interface CartService {
    List<CartItemDto> getCartItems(Long userId);
    Integer getCartCount(Long userId);
    void addToCart(Long userId, Long productId, Integer quantity);
    void addToCart(Long userId, Long productId, Double weightInGrams);
    void updateCartItem(Long userId, Long itemId, Integer quantity, Boolean selected);
    void removeCartItem(Long userId, Long itemId);
    void clearCart(Long userId);
}
