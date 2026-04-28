package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 搜索历史操作请求DTO
 * 约束：userId正整数
 */
@Data
@Schema(description = "搜索历史操作请求参数")
public class AppSearchHistoryQuery {

    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正整数")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long userId;
}
