package com.example.supermarket2.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("map_features")
public class MapFeature {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long mapId;
    private String featureType;
    private String properties;
    private String geometry;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}