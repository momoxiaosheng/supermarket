package com.example.supermarket2.dto.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationRequest {

    @NotBlank(message = "地图ID不能为空")
    private String mapId;   // 地图标识

    @NotNull(message = "起点x坐标不能为空")
    private Integer startX; // 起点横坐标

    @NotNull(message = "起点y坐标不能为空")
    private Integer startY; // 起点纵坐标

    @NotNull(message = "终点x坐标不能为空")
    private Integer endX;   // 终点横坐标

    @NotNull(message = "终点y坐标不能为空")
    private Integer endY;   // 终点纵坐标
    private String userId;  // 用户ID（可选）
}
