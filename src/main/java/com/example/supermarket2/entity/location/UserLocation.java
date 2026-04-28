package com.example.supermarket2.entity.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLocation {
    private Long id;

    @NotBlank(message = "用户ID不能为空")
    private String userId;  // 用户ID

    @NotBlank(message = "地图标识不能为空")
    private String mapId;   // 地图标识

    @NotNull(message = "x坐标不能为空")
    private Integer x;      // 横坐标

    @NotNull(message = "y坐标不能为空")
    private Integer y;      // 纵坐标
    private Date timeStamp; // 定位时间（建议用 LocalDateTime，示例简化为 String）
}

