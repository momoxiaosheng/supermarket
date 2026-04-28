package com.example.supermarket2.service;


import com.example.supermarket2.dto.app.response.ProductDto;

import java.util.List;

public interface ProductService {
    List<ProductDto> getRecommendProducts(int limit);
    List<ProductDto> getAllProducts();
    ProductDto getProductById(Long id);
}