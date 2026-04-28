package com.example.supermarket2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("banner")
public class Banner {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String image;
    private String url;
    private Integer sortOrder;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
