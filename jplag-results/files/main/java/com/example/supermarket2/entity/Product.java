package com.example.supermarket2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.supermarket2.handler.ListStringTypeHandler;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;

    // 指定字段使用自定义类型处理器
    @TableField(typeHandler = ListStringTypeHandler.class)
    private List<String> images;

    @TableField(typeHandler = ListStringTypeHandler.class)
    private List<String> detailImages;

    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal discount;
    private Long categoryId;
    private String location;
    private String brand;
    private Integer stock;
    private String expiryDate;
    private String ingredients;

    @TableField(typeHandler = ListStringTypeHandler.class)
    private List<String> tags;

    private Integer status;
    private Date createTime;
    private Date updateTime;
}