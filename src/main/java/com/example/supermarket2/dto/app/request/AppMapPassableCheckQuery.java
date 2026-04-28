package com.example.supermarket2.dto.app.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 坐标可通行性校验请求DTO
 * 约束：mapId长度限制+坐标范围[0,9999]，防止非法参数进入业务层
 */
@Data
@Schema(description = "坐标可通行性校验请求参数")
public class AppMapPassableCheckQuery {

    @NotBlank(message = "地图ID不能为空")
    @Size(min = 1, max = 32, message = "地图ID长度必须在1-32个字符之间")
    @Schema(description = "地图标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "mall_1f")
    private String mapId;

    @NotNull(message = "x坐标不能为空")
    @Min(value = 0, message = "x坐标不能为负数")
    @Max(value = 9999, message = "x坐标超出最大范围")
    @Schema(description = "横坐标", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer x;

    @NotNull(message = "y坐标不能为空")
    @Min(value = 0, message = "y坐标不能为负数")
    @Max(value = 9999, message = "y坐标超出最大范围")
    @Schema(description = "纵坐标", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    private Integer y;
}
