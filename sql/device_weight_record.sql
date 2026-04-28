-- 设备称重记录表
CREATE TABLE IF NOT EXISTS `device_weight_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `device_id` VARCHAR(64) NOT NULL COMMENT '设备ID',
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(128) DEFAULT NULL COMMENT '商品名称（冗余存储）',
    `quantity_grams` DOUBLE NOT NULL COMMENT '重量（克）',
    `report_time` DATETIME DEFAULT NULL COMMENT '设备上报时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    INDEX `idx_device_id` (`device_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_report_time` (`report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备称重记录表';
