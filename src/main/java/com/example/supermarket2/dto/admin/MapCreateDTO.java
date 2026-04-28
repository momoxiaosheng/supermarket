package com.example.supermarket2.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 地图创建请求DTO
 */
@Data
@Schema(description = "地图创建请求参数")
public class MapCreateDTO {

    @NotBlank(message = "地图名称不能为空")
    @Schema(description = "地图名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "地图描述")
    private String description;

    @Schema(description = "楼层", defaultValue = "1")
    private Integer floor = 1;

    @NotNull(message = "地图底图不能为空")
    @Schema(description = "地图底图图片", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile baseImage;
}