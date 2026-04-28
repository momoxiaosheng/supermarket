package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 位置历史查询请求DTO
 * 约束：userId/mapId长度限制+limit范围[1,500]，防止非法参数进入业务层
 */
@Data
@Schema(description = "位置历史查询请求参数")
public class AppLocationHistoryQuery {

    @NotBlank(message = "用户ID不能为空")
    @Size(min = 1, max = 32, message = "用户ID长度必须在1-32个字符之间")
    @Schema(description = "用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private String userId;

    @NotBlank(message = "地图ID不能为空")
    @Size(min = 1, max = 32, message = "地图ID长度必须在1-32个字符之间")
    @Schema(description = "地图标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "mall_1f")
    private String mapId;

    @Min(value = 1, message = "查询条数最小为1")
    @Max(value = 500, message = "查询条数最大为500")
    @Schema(description = "查询条数限制", requiredMode = Schema.RequiredMode.REQUIRED, example = "10", defaultValue = "10")
    private Integer limit = 10;
}