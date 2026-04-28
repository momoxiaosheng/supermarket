package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 推荐商品查询请求DTO
 * 约束：查询数量限制在[1,100]区间，防止恶意全表扫描
 */
@Data
@Schema(description = "推荐商品查询请求参数")
public class AppProductRecommendQuery {

    @Min(value = 1, message = "查询数量最小为1")
    @Max(value = 100, message = "查询数量最大为100")
    @Schema(description = "查询数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5", defaultValue = "5")
    private int limit = 5;
}
