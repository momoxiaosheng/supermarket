package com.example.supermarket2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("device_weight_record")
public class DeviceWeightRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 设备ID */
    private String deviceId = "unknown";

    /** 关联用户ID（由设备数据中的 deviceId 映射得到） */
    private Long userId;

    /** 商品ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 重量（克） */
    private Double quantityGrams;

    /** 上报时间（来自设备） */
    private Date reportTime;

    /** 记录创建时间（系统时间） */
    private Date createTime;
}
