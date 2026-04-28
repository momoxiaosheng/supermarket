package com.example.supermarket2.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 地图更新请求DTO
 */
@Data
@Schema(description = "地图更新请求参数")
public class MapUpdateDTO {

    @NotBlank(message = "地图名称不能为空")
    @Schema(description = "地图名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "地图描述")
    private String description;

    @Schema(description = "楼层")
    private Integer floor;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "地图状态 0-未发布 1-已发布")
    private Integer status;
}