package com.example.supermarket2.service.impl;

import com.example.supermarket2.dto.app.SearchResultDto;
import com.example.supermarket2.entity.Product;
import com.example.supermarket2.mapper.ProductMapper;
import com.example.supermarket2.service.SearchService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired  // 注入ObjectMapper
    private ObjectMapper objectMapper;

    private static final String SEARCH_HISTORY_KEY_PREFIX = "search:history:";
    private static final String HOT_KEYWORDS_KEY = "hot:keywords";
    private static final long SEARCH_HISTORY_EXPIRE_DAYS = 30;

    @Override
    public List<SearchResultDto> searchProducts(String keyword) {
        List<Product> products = productMapper.searchProducts(keyword);
        return products.stream().map(this::convertToSearchResultDto).collect(Collectors.toList());
    }

    @Override
    public List<String> getHotKeywords() {
        // 从Redis获取热门关键词
        Set<Object> keywords = redisTemplate.opsForZSet().reverseRange(HOT_KEYWORDS_KEY, 0, 9);
        if (keywords != null) {
            return keywords.stream().map(Object::toString).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

//    @Override
//    public List<String> getSearchHistory(Long userId) {
//        String key = SEARCH_HISTORY_KEY_PREFIX + userId;
//        Set<Object> history = redisTemplate.opsForZSet().reverseRange(key, 0, 9);
//        if (history != null) {
//            return history.stream().map(Object::toString).collect(Collectors.toList());
//        }
//        return new ArrayList<>();
//    }

//    @Override
//    public void saveSearchHistory(Long userId, String keyword) {
//        String key = SEARCH_HISTORY_KEY_PREFIX + userId;
//        double score = System.currentTimeMillis();
//        redisTemplate.opsForZSet().add(key, keyword, score);
//        redisTemplate.expire(key, SEARCH_HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);
//
//        // 更新热门关键词
//        redisTemplate.opsForZSet().incrementScore(HOT_KEYWORDS_KEY, keyword, 1);
//    }

    @Override
    public List<String> getSearchHistory(Long userId) {
        String key = SEARCH_HISTORY_KEY_PREFIX + userId;
        Set<Object> history = redisTemplate.opsForZSet().reverseRange(key, 0, 9);
        return history.stream().map(Object::toString).collect(Collectors.toList());
    }

    @Override
    public void saveSearchHistory(Long userId, String keyword) {
        String key = SEARCH_HISTORY_KEY_PREFIX + userId;
        double score = System.currentTimeMillis();

        // 添加搜索历史
        redisTemplate.opsForZSet().add(key, keyword, score);

        // 设置过期时间
        redisTemplate.expire(key, 30, TimeUnit.DAYS);

        // 更新热门关键词
        redisTemplate.opsForZSet().incrementScore(HOT_KEYWORDS_KEY, keyword, 1);
    }

    @Override
    public void clearSearchHistory(Long userId) {
        String key = SEARCH_HISTORY_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    private SearchResultDto convertToSearchResultDto(Product product) {
        SearchResultDto dto = new SearchResultDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());

        try {
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                dto.setImage(product.getImages().get(0));
            }
        } catch (Exception e) {
            // 处理JSON解析异常
        }

        dto.setPrice(product.getPrice());
        dto.setLocation(product.getLocation());
        return dto;
    }
}