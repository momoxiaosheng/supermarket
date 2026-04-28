package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 地图数据查询请求DTO
 * 约束：mapId长度限制在[1,32]，防止非法字符串穿透
 */
@Data
@Schema(description = "地图数据查询请求参数")
public class AppMapDataQuery {

    @NotBlank(message = "地图ID不能为空")
    @Size(min = 1, max = 32, message = "地图ID长度必须在1-32个字符之间")
    @Schema(description = "地图标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "mall_1f")
    private String mapId;
}