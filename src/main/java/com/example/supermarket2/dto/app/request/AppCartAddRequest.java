package com.example.supermarket2.dto.app.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车新增商品请求DTO
 */
@Data
@Schema(description = "购物车新增商品请求参数")
public class AppCartAddRequest {

    @NotNull(message = "用户ID不能为空")
    @Min(value = 1, message = "用户ID必须为正整数")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    @Min(value = 1, message = "商品ID必须为正整数")
    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long productId;

    @Min(value = 1, message = "添加商品数量最小为1")
    @Max(value = 999, message = "添加商品数量最大为999")
    @Schema(description = "商品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1", defaultValue = "1")
    private Integer quantity = 1;
}