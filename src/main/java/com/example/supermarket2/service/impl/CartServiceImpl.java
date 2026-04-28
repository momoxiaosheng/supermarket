package com.example.supermarket2.service.impl;

import com.example.supermarket2.common.exception.BusinessException;
import com.example.supermarket2.common.result.ErrorCode;
import com.example.supermarket2.domain.cart.CartDomainService;
import com.example.supermarket2.dto.app.response.CartItemDto;
import com.example.supermarket2.entity.Product;
import com.example.supermarket2.infra.redis.CartRedisRepository;
import com.example.supermarket2.infra.redis.model.RedisCartItem;
import com.example.supermarket2.mapper.ProductMapper;
import com.example.supermarket2.service.CartService;
import com.example.supermarket2.service.WebSocketPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartDomainService cartDomainService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CartRedisRepository cartRedisRepository;

    // 注入推送服务
    @Autowired
    private WebSocketPushService webSocketPushService;

    @Override
    public List<CartItemDto> getCartItems(Long userId) {
        Map<String, RedisCartItem> cartItemMap = cartRedisRepository.getCartItems(userId);

        List<CartItemDto> cartItemDtoList = new ArrayList<>();

        for (Map.Entry<String, RedisCartItem> entry : cartItemMap.entrySet()) {
            RedisCartItem redisCartItem = entry.getValue();

            // 获取商品详细信息
            Product product = productMapper.selectProductById(redisCartItem.getProductId());
            if (cartDomainService.isProductAvailable(product)) { // 只返回上架商品
                CartItemDto dto = cartDomainService.convertToDto(redisCartItem, product);
                cartItemDtoList.add(dto);
            }
        }

        return cartItemDtoList;
    }

    @Override
    public Integer getCartCount(Long userId) {
        return cartRedisRepository.getCartItemsCount(userId);
    }

    @Override
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // 检查商品是否存在且上架
        Product product = productMapper.selectProductById(productId);
        if (!cartDomainService.isProductAvailable(product)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        // 检查购物车中是否已有该商品
        RedisCartItem existingItem = cartRedisRepository.getCartItem(userId, productId);

        if (existingItem != null) {
            try {
                existingItem = cartDomainService.updateCartItem(existingItem, quantity);

                cartRedisRepository.addToCart(userId, productId, existingItem);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.CART_UPDATE_FAILED, e.getMessage());
            }
        } else {
            // 新增商品
            RedisCartItem newItem = cartDomainService.addCartItem(productId, quantity);

            cartRedisRepository.addToCart(userId, productId, newItem);
        }

        // 设置或更新过期时间
        cartRedisRepository.expireCart(userId);
        webSocketPushService.pushCartUpdate(userId, this.getCartItems(userId));
    }

    public void addToCart(Long userId, Long productId, Double weightInGrams) {
        // 检查商品是否存在且上架
        Product product = productMapper.selectProductById(productId);
        if (!cartDomainService.isProductAvailable(product)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }

        // 检查购物车中是否已有该商品
        RedisCartItem existingItem = cartRedisRepository.getCartItem(userId, productId);

        if (existingItem != null) {
            try {
                existingItem = cartDomainService.updateCartItem(existingItem, weightInGrams);

                cartRedisRepository.addToCart(userId, productId, existingItem);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.CART_UPDATE_FAILED, e.getMessage());
            }
        } else {
            // 新增商品
            RedisCartItem newItem = cartDomainService.addCartItem(productId, weightInGrams);

            cartRedisRepository.addToCart(userId, productId, newItem);
        }

        // 设置或更新过期时间
        cartRedisRepository.expireCart(userId);
        webSocketPushService.pushCartUpdate(userId, this.getCartItems(userId));
    }

    @Override
    public void updateCartItem(Long userId, Long productId, Integer quantity, Boolean selected) {
        RedisCartItem existingItem = cartRedisRepository.getCartItem(userId, productId);
        if (existingItem != null) {
            if (quantity != null) {
                existingItem.setQuantity(quantity);
            }
            if (selected != null) {
                existingItem.setSelected(selected);
            }
            existingItem.setUpdateTime(System.currentTimeMillis());
            cartRedisRepository.updateCartItem(userId, productId, existingItem);
            webSocketPushService.pushCartUpdate(userId, this.getCartItems(userId));
        }
    }

    @Override
    public void removeCartItem(Long userId, Long productId) {
        cartRedisRepository.removeCartItem(userId, productId);

        // 如果购物车为空，删除整个key
        Integer size = cartRedisRepository.getCartItemsCount(userId);
        if (size != null && size == 0) {
            cartRedisRepository.clearCart(userId);
            webSocketPushService.pushCartUpdate(userId, this.getCartItems(userId));
        }
    }

    @Override
    public void clearCart(Long userId) {
        cartRedisRepository.clearCart(userId);
        webSocketPushService.pushCartUpdate(userId, List.of());
    }

}