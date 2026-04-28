package com.example.supermarket2.controller.app;

import com.example.supermarket2.common.result.Result;
import com.example.supermarket2.dto.app.request.AppSearchHistoryQuery;
import com.example.supermarket2.dto.app.request.AppSearchSuggestQuery;
import com.example.supermarket2.dto.app.response.SearchResultDto;
import com.example.supermarket2.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/app/search")
@Tag(name = "APP搜索管理", description = "移动端商品搜索、搜索历史相关接口")
@RequiredArgsConstructor
public class AppSearchController {

    private final SearchService searchService;

    /**
     * 搜索建议
     * 改造点：入参封装为DTO，添加keyword长度+userId正整数校验
     */
    @GetMapping("/suggest")
    @Operation(summary = "搜索建议", description = "根据关键词模糊搜索商品并自动保存搜索历史（去重）")
    public Result<List<SearchResultDto>> searchSuggest(@Valid AppSearchSuggestQuery query) {
        log.debug("请求搜索建议: userId={}, keyword={}", query.getUserId(), query.getKeyword());
        List<SearchResultDto> results = searchService.searchProducts(query.getKeyword().trim());
        searchService.saveSearchHistory(query.getUserId(), query.getKeyword().trim());
        log.debug("返回搜索建议，数量={}", results.size());
        return Result.success(results);
    }

    /**
     * 获取热门搜索关键词
     * 原有合规接口，仅补充日志
     */
    @GetMapping("/hot")
    @Operation(summary = "获取热门搜索关键词", description = "获取按搜索次数降序排列的前20个热门搜索关键词")
    public Result<List<String>> getHotKeywords() {
        log.debug("请求获取热门搜索关键词");
        List<String> hotKeywords = searchService.getHotKeywords();
        log.debug("返回热门搜索关键词，数量={}", hotKeywords.size());
        return Result.success(hotKeywords);
    }

    /**
     * 获取搜索历史
     * 改造点：入参封装为DTO，添加userId正整数校验
     */
    @GetMapping("/history")
    @Operation(summary = "获取搜索历史", description = "获取指定用户按搜索时间降序排列的前20条搜索历史（去重）")
    public Result<List<String>> getSearchHistory(@Valid AppSearchHistoryQuery query) {
        log.debug("请求获取搜索历史: userId={}", query.getUserId());
        List<String> history = searchService.getSearchHistory(query.getUserId());
        log.debug("返回搜索历史，数量={}", history.size());
        return Result.success(history);
    }

    /**
     * 清空搜索历史
     * 改造点：入参封装为DTO，添加userId正整数校验
     */
    @GetMapping("/clear")
    @Operation(summary = "清空搜索历史", description = "清空指定用户的所有搜索历史记录")
    public Result<Void> clearSearchHistory(@Valid AppSearchHistoryQuery query) {
        log.debug("请求清空搜索历史: userId={}", query.getUserId());
        searchService.clearSearchHistory(query.getUserId());
        log.debug("清空搜索历史成功: userId={}", query.getUserId());
        return Result.success();
    }
}