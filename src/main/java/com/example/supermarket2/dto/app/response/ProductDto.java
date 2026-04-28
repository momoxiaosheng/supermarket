package com.example.supermarket2.dto.app.response;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private List<String> images;
    private List<String> detailImages;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discount;
    private String location;
    private String brand;
    private Integer stock;
    private String expiryDate;
    private String ingredients;
    private List<String> tags;
}