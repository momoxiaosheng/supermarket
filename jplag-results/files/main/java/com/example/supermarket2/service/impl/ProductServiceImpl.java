package com.example.supermarket2.service.impl;

import com.example.supermarket2.dto.app.ProductDto;
import com.example.supermarket2.entity.Product;
import com.example.supermarket2.mapper.ProductMapper;
import com.example.supermarket2.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ProductDto> getRecommendProducts(int limit) {
        List<Product> products = productMapper.selectRecommendProducts(limit);
        return products.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> getAllProducts() {
        List<Product> products = productMapper.selectAllProducts();
        return products.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public ProductDto getProductById(Long id) {
        Product product = productMapper.selectProductById(id);
        return convertToDto(product);
    }

    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());

        // 直接赋值，无需手动解析JSON（MyBatis已处理）
        dto.setImages(product.getImages());
        dto.setDetailImages(product.getDetailImages());
        dto.setTags(product.getTags());

        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setDiscount(product.getDiscount());
        dto.setLocation(product.getLocation());
        dto.setBrand(product.getBrand());
        dto.setStock(product.getStock());
        dto.setExpiryDate(product.getExpiryDate());
        dto.setIngredients(product.getIngredients());

        return dto;
    }
}
