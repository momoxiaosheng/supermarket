package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 搜索建议查询请求DTO
 * 约束：keyword长度[1,50]，userId正整数
 */
@Data
@Schema(description = "搜索建议查询请求参数")
public class AppSearchSuggestQuery {

    @NotBlank(message = "搜索关键词不能为空")
    @Size(min = 1, max = 50, message = "搜索关键词长度必须在1-50个字符之间")
    @Schema(description = "搜索关键词", requiredMode = Schema.RequiredMode.REQUIRED, example = "牛奶")
    private String keyword;

    @NotNull(message = "用户ID不能为空")
    @jakarta.validation.constraints.Min(value = 1, message = "用户ID必须为正整数")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long userId;
}
