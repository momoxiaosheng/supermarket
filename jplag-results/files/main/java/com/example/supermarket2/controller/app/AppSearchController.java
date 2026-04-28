package com.example.supermarket2.controller.app;

import com.example.supermarket2.service.SearchService;
import com.example.supermarket2.utils.ResponseUtil;
import com.example.supermarket2.dto.app.SearchResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/app/search")
@Tag(name = "APP搜索管理", description = "移动端搜索相关接口")
public class AppSearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/suggest")
    @Operation(summary = "搜索建议", description = "根据关键词搜索商品并保存搜索历史")
    public Object searchSuggest(@RequestParam String keyword, @RequestParam Long userId) {
        List<SearchResultDto> results = searchService.searchProducts(keyword);
        // 保存搜索历史
        if (userId != null && !keyword.trim().isEmpty()) {
            searchService.saveSearchHistory(userId, keyword.trim());
        }
        return ResponseUtil.ok(results);
    }

    @GetMapping("/hot")
    @Operation(summary = "获取热门搜索关键词", description = "获取热门搜索关键词列表")
    public Object getHotKeywords() {
        List<String> hotKeywords = searchService.getHotKeywords();
        return ResponseUtil.ok(hotKeywords);
    }

    @GetMapping("/history")
    @Operation(summary = "获取搜索历史", description = "获取指定用户的搜索历史记录")
    public Object getSearchHistory(@RequestParam Long userId) {
        List<String> history = searchService.getSearchHistory(userId);
        return ResponseUtil.ok(history);
    }

    @GetMapping("/clear")
    @Operation(summary = "清空搜索历史", description = "清空指定用户的搜索历史记录")
    public Object clearSearchHistory(@RequestParam Long userId) {
        searchService.clearSearchHistory(userId);
        return ResponseUtil.ok();
    }
}
