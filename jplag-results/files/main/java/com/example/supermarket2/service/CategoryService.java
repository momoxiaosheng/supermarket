package com.example.supermarket2.service;


import com.example.supermarket2.dto.app.CategoryDto;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getRootCategories();
}
