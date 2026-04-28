package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车清空请求DTO
 */
@Data
@Schema(description = "购物车清空请求参数")
public class AppCartClearRequest {

    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正整数")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;
}