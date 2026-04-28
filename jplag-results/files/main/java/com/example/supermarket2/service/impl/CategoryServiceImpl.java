package com.example.supermarket2.service.impl;

import com.example.supermarket2.dto.app.CategoryDto;
import com.example.supermarket2.entity.Category;
import com.example.supermarket2.mapper.CategoryMapper;
import com.example.supermarket2.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<CategoryDto> getRootCategories() {
        List<Category> categories = categoryMapper.selectRootCategories();
        return categories.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private CategoryDto convertToDto(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        return dto;
    }
}