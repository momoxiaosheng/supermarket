package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品详情查询请求DTO
 * 约束：ID必须为非空正整数，兼容@PathVariable自动绑定
 */
@Data
@Schema(description = "商品详情查询请求参数")
public class AppProductDetailQuery {

    @NotNull(message = "商品ID不能为空")
    @Min(value = 1, message = "商品ID必须为正整数")
    @Schema(description = "商品ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    private Long id;
}
