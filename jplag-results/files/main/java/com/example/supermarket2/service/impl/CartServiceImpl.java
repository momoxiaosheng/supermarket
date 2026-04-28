package com.example.supermarket2.service.impl;

import com.example.supermarket2.dto.app.CartItemDto;
import com.example.supermarket2.entity.Product;
import com.example.supermarket2.mapper.ProductMapper;
import com.example.supermarket2.service.CartService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // Redis键前缀
    private static final String CART_KEY_PREFIX = "cart:user:";
    // 购物车数据过期时间（30天）
    private static final long CART_EXPIRE_DAYS = 30;

    @Override
    public List<CartItemDto> getCartItems(Long userId) {
        String key = CART_KEY_PREFIX + userId;
        Map<Object, Object> cartData = redisTemplate.opsForHash().entries(key);

        List<CartItemDto> cartItems = new ArrayList<>();

        for (Map.Entry<Object, Object> entry : cartData.entrySet()) {
            try {
                String productIdStr = (String) entry.getKey();
                String cartItemJson = (String) entry.getValue();

                // 解析Redis中的购物车项数据
                RedisCartItem redisCartItem = objectMapper.readValue(cartItemJson, RedisCartItem.class);

                // 获取商品详细信息
                Product product = productMapper.selectProductById(redisCartItem.getProductId());
                if (product != null && product.getStatus() == 1) { // 只返回上架商品
                    CartItemDto dto = convertToDto(redisCartItem, product);
                    cartItems.add(dto);
                }
            } catch (Exception e) {
                // 处理异常，跳过无效数据
            }
        }

        return cartItems;
    }

    @Override
    public Integer getCartCount(Long userId) {
        String key = CART_KEY_PREFIX + userId;
        Long size = redisTemplate.opsForHash().size(key);
        return size != null ? size.intValue() : 0;
    }

    @Override
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // 检查商品是否存在且上架
        Product product = productMapper.selectProductById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new RuntimeException("商品不存在或已下架");
        }

        String key = CART_KEY_PREFIX + userId;
        String field = String.valueOf(productId);

        // 检查购物车中是否已有该商品
        Object existingItem = redisTemplate.opsForHash().get(key, field);

        if (existingItem != null) {
            try {
                // 更新数量
                RedisCartItem cartItem = objectMapper.readValue((String) existingItem, RedisCartItem.class);
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
                cartItem.setUpdateTime(System.currentTimeMillis());

                redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(cartItem));
            } catch (Exception e) {
                throw new RuntimeException("更新购物车失败");
            }
        } else {
            // 新增商品
            RedisCartItem newItem = new RedisCartItem();
            newItem.setProductId(productId);
            newItem.setQuantity(quantity);
            newItem.setSelected(true);
            newItem.setCreateTime(System.currentTimeMillis());
            newItem.setUpdateTime(System.currentTimeMillis());

            try {
                redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(newItem));
            } catch (Exception e) {
                throw new RuntimeException("添加商品到购物车失败");
            }
        }

        // 设置或更新过期时间
        redisTemplate.expire(key, CART_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void addToCart(Long userId, Long productId, Double weightInKg) {
        // 检查商品是否存在且上架
        Product product = productMapper.selectProductById(productId);
        if (product == null || product.getStatus() != 1) {
            throw new RuntimeException("商品不存在或已下架");
        }

        String key = CART_KEY_PREFIX + userId;
        String field = String.valueOf(productId);

        // 检查购物车中是否已有该商品
        Object existingItem = redisTemplate.opsForHash().get(key, field);

        if (existingItem != null) {
            try {
                // 更新重量
                RedisCartItem cartItem = objectMapper.readValue((String) existingItem, RedisCartItem.class);
                // 重量商品直接覆盖原有重量（因为每次称重都是最新值）
                cartItem.setWeight(weightInKg);
                cartItem.setUpdateTime(System.currentTimeMillis());

                redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(cartItem));
            } catch (Exception e) {
                throw new RuntimeException("更新购物车失败");
            }
        } else {
            // 新增称重商品
            RedisCartItem newItem = new RedisCartItem();
            newItem.setProductId(productId);
            newItem.setWeight(weightInKg); // 设置重量
            newItem.setSelected(true);
            newItem.setCreateTime(System.currentTimeMillis());
            newItem.setUpdateTime(System.currentTimeMillis());

            try {
                redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(newItem));
            } catch (Exception e) {
                throw new RuntimeException("添加商品到购物车失败");
            }
        }

        // 设置或更新过期时间
        redisTemplate.expire(key, CART_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void updateCartItem(Long userId, Long productId, Integer quantity, Boolean selected) {
        String key = CART_KEY_PREFIX + userId;
        String field = String.valueOf(productId);

        Object existingItem = redisTemplate.opsForHash().get(key, field);
        if (existingItem != null) {
            try {
                RedisCartItem cartItem = objectMapper.readValue((String) existingItem, RedisCartItem.class);

                if (quantity != null) {
                    cartItem.setQuantity(quantity);
                }
                if (selected != null) {
                    cartItem.setSelected(selected);
                }
                cartItem.setUpdateTime(System.currentTimeMillis());

                redisTemplate.opsForHash().put(key, field, objectMapper.writeValueAsString(cartItem));

                // 更新过期时间
                redisTemplate.expire(key, CART_EXPIRE_DAYS, TimeUnit.DAYS);
            } catch (Exception e) {
                throw new RuntimeException("更新购物车项失败");
            }
        }
    }

    @Override
    public void removeCartItem(Long userId, Long productId) {
        String key = CART_KEY_PREFIX + userId;
        String field = String.valueOf(productId);

        redisTemplate.opsForHash().delete(key, field);

        // 如果购物车为空，删除整个key
        Long size = redisTemplate.opsForHash().size(key);
        if (size != null && size == 0) {
            redisTemplate.delete(key);
        }
    }

    @Override
    public void clearCart(Long userId) {
        String key = CART_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /**
     * 将Redis中的购物车项和商品信息转换为CartItemDto
     */
    private CartItemDto convertToDto(RedisCartItem redisCartItem, Product product) {
        CartItemDto dto = new CartItemDto();
        dto.setId(redisCartItem.getProductId()); // 使用productId作为临时ID
        dto.setProductId(redisCartItem.getProductId());
        dto.setQuantity(redisCartItem.getQuantity());
        dto.setWeight(redisCartItem.getWeight());
        dto.setSelected(redisCartItem.getSelected());

        // 设置商品信息
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());

        try {
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                dto.setImage(product.getImages().get(0));
            }
        } catch (Exception e) {
            // 处理JSON解析异常
        }

        return dto;
    }

    /**
     * Redis中存储的购物车项数据结构
     */
    private static class RedisCartItem {
        private Long productId;
        private Integer quantity;  // 计件商品数量
        private Double weight;     // 称重商品重量（千克）
        private Boolean selected;
        private Long createTime;
        private Long updateTime;

        // Getter和Setter方法
        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getWeight() { return weight; }

        public void setWeight(Double weight) { this.weight = weight; }

        public Boolean getSelected() {
            return selected;
        }

        public void setSelected(Boolean selected) {
            this.selected = selected;
        }

        public Long getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }

        public Long getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Long updateTime) {
            this.updateTime = updateTime;
        }
    }
}