package com.example.supermarket2.entity.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapGrid {
    private Long id;
    private String mapId;   // 地图标识（如 mall_1f）
    private Integer x;      // 横坐标
    private Integer y;      // 纵坐标
    private String color;   // 颜色（如 white、black）
    private Integer passable; // 0=不可通行，1=可通行
}