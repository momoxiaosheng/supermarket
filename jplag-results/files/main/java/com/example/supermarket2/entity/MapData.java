package com.example.supermarket2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("map_data")
public class MapData {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String thumbnail;
    private Integer floor;
    private String version;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // GeoJSON数据（不存储到数据库，通过关联表获取）
    @TableField(exist = false)
    private String geoJson;
}