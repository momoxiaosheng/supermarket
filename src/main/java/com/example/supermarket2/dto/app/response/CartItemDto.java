package com.example.supermarket2.dto.app.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemDto {
    private Long id;
    private Long productId;
    private String name;
    private String image;
    private BigDecimal price;
    private Integer quantity;
    private Double weight;
    private Boolean selected;
}
