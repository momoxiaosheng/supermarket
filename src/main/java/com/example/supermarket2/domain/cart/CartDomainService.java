package com.example.supermarket2.domain.cart;

import com.example.supermarket2.dto.app.response.CartItemDto;
import com.example.supermarket2.entity.Product;
import com.example.supermarket2.infra.redis.model.RedisCartItem;
import org.springframework.stereotype.Component;

@Component
public class CartDomainService {

    // 判断商品是否存在且上架
    public boolean isProductAvailable(Product product) {
        return product != null && product.getStatus() == 1;
    }

    // 更新购物车计数商品的数量
    public RedisCartItem updateCartItem(RedisCartItem cartItem, Integer quantity) {
        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        cartItem.setUpdateTime(System.currentTimeMillis());
        return cartItem;
    }

    // 新增计数商品到购物车
    public RedisCartItem addCartItem(Long productId, Integer quantity) {
        RedisCartItem newItem = new RedisCartItem();
        newItem.setProductId(productId);
        newItem.setQuantity(quantity);
        newItem.setSelected(true);
        newItem.setCreateTime(System.currentTimeMillis());
        newItem.setUpdateTime(System.currentTimeMillis());
        return newItem;
    }

    // 更新购物车称重商品的数量（克）
    public RedisCartItem updateCartItem(RedisCartItem cartItem, Double weightInGrams) {
        cartItem.setWeight(cartItem.getWeight() + weightInGrams);
        cartItem.setUpdateTime(System.currentTimeMillis());
        return cartItem;
    }

    // 新增称重商品到购物车（克）
    public RedisCartItem addCartItem(Long productId, Double weightInGrams) {
        RedisCartItem newItem = new RedisCartItem();
        newItem.setProductId(productId);
        newItem.setWeight(weightInGrams);
        newItem.setSelected(true);
        newItem.setCreateTime(System.currentTimeMillis());
        newItem.setUpdateTime(System.currentTimeMillis());
        return newItem;
    }

    // 判断购物车是否为空
    public boolean isCartEmpty(Long size) {
        if(size != null && size == 0){
            return true;
        }
        return false;
    }

    /**
     * 将Redis中的购物车项和商品信息转换为CartItemDto
     */
    public CartItemDto convertToDto(RedisCartItem redisCartItem, Product product) {
        CartItemDto dto = new CartItemDto();
        dto.setId(redisCartItem.getProductId()); // 使用productId作为临时ID
        dto.setProductId(redisCartItem.getProductId());
        dto.setQuantity(redisCartItem.getQuantity());
        dto.setWeight(redisCartItem.getWeight());
        dto.setSelected(redisCartItem.getSelected());

        // 设置商品信息
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            dto.setImage(product.getImages().get(0));
        }

        return dto;
    }
}
