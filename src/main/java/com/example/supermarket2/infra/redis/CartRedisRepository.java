package com.example.supermarket2.infra.redis;

import com.example.supermarket2.common.constants.RedisKeyConstants;
import com.example.supermarket2.infra.redis.model.RedisCartItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.supermarket2.common.constants.RedisKeyConstants.CART_USER_KEY;

@Slf4j
@Component
@RequiredArgsConstructor // 构造器注入，替代@Autowired，符合Spring最佳实践
public class CartRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public Map<String, RedisCartItem> getCartItems(Long userId) {
        String key = String.format(CART_USER_KEY, userId);
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
        Map<String, RedisCartItem> cartItemMap = new HashMap<>();

        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            String productId = (String) entry.getKey();
            Object value = entry.getValue();

            // 【修复】类型校验，仅当类型匹配时才强转
            if (value instanceof RedisCartItem redisCartItem) {
                cartItemMap.put(productId, redisCartItem);
            } else {
                log.warn("购物车数据类型异常，userId={}, productId={}, 实际类型={}",
                        userId, productId, value.getClass().getName());
                // 清理异常数据，避免后续持续报错
                redisTemplate.opsForHash().delete(key, productId);
            }
        }
        return cartItemMap;
    }

    public RedisCartItem getCartItem(Long userId, Long productId) {
        String key = String.format(CART_USER_KEY, userId);
        String field = String.valueOf(productId);
        Object value = redisTemplate.opsForHash().get(key, field);

        // 【修复】类型校验，避免强转异常
        if (value instanceof RedisCartItem redisCartItem) {
            return redisCartItem;
        } else if (value != null) {
            log.warn("购物车单项数据类型异常，userId={}, productId={}, 实际类型={}",
                    userId, productId, value.getClass().getName());
            redisTemplate.opsForHash().delete(key, field);
        }
        return null;
    }

    public Integer getCartItemsCount(Long userId) {
        String key = String.format(CART_USER_KEY, userId);
        Long count = redisTemplate.opsForHash().size(key);
        return count != null ? count.intValue() : 0;
    }

    public void addToCart(Long userId, Long productId, RedisCartItem cartItem) {
        String key = String.format(CART_USER_KEY, userId);
        String field = String.valueOf(productId);
        redisTemplate.opsForHash().put(key, field, cartItem);
        // 新增后刷新过期时间
        expireCart(userId);
    }

    public void updateCartItem(Long userId, Long productId, RedisCartItem cartItem) {
        String key = String.format(CART_USER_KEY, userId);
        String field = String.valueOf(productId);
        redisTemplate.opsForHash().put(key, field, cartItem);
        expireCart(userId);
    }

    public void removeCartItem(Long userId, Long productId) {
        String key = String.format(CART_USER_KEY, userId);
        String field = String.valueOf(productId);
        redisTemplate.opsForHash().delete(key, field);
    }

    public void clearCart(Long userId) {
        String key = String.format(CART_USER_KEY, userId);
        redisTemplate.delete(key);
    }

    public void expireCart(Long userId) {
        String key = String.format(CART_USER_KEY, userId);
        redisTemplate.expire(key, RedisKeyConstants.getSecondsUntilEndOfDay(), TimeUnit.SECONDS);
    }
}