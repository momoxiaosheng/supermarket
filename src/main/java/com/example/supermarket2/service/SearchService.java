package com.example.supermarket2.service;

import com.example.supermarket2.dto.app.response.SearchResultDto;

import java.util.List;

public interface SearchService {
    List<SearchResultDto> searchProducts(String keyword);
    List<String> getHotKeywords();
    List<String> getSearchHistory(Long userId);
    void saveSearchHistory(Long userId, String keyword);
    void clearSearchHistory(Long userId);
}