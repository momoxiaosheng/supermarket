package com.example.supermarket2.infra.redis.model;

import lombok.Data;

@Data
public class RedisCartItem {
    /**
     * Redis中存储的购物车项数据结构
     */
    private Long productId;
    private Integer quantity;  // 计件商品数量
    private Double weight;     // 称重商品重量（克）
    private Boolean selected;
    private Long createTime;
    private Long updateTime;
}
