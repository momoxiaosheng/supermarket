-- 信标位置表
-- 存储超市内所有 BLE 信标的位置信息，供定位引擎动态加载
CREATE TABLE IF NOT EXISTS `beacon_location` (
    `id`         BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `uuid`       VARCHAR(36)  NOT NULL                   COMMENT '信标 UUID',
    `x`          DOUBLE       NOT NULL                   COMMENT 'X 坐标（米）',
    `y`          DOUBLE       NOT NULL                   COMMENT 'Y 坐标（米）',
    `enabled`    TINYINT      NOT NULL DEFAULT 1         COMMENT '是否启用（1=启用，0=禁用）',
    `created_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_uuid` (`uuid`),
    INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='信标位置表';

-- 示例数据（四角默认信标，与代码中的兜底列表对应）
INSERT INTO `beacon_location` (`uuid`, `x`, `y`, `enabled`) VALUES
('01122334-4556-6778-899A-ABBCCDDEEFF1',  1.0,  1.0, 1),
('01122334-4556-6778-899A-ABBCCDDEEFF2', 28.0,  1.0, 1),
('01122334-4556-6778-899A-ABBCCDDEEFF3', 28.0, 48.0, 1),
('01122334-4556-6778-899A-ABBCCDDEEFF4',  1.0, 48.0, 1);
