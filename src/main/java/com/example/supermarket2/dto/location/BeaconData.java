package com.example.supermarket2.dto.location;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 信标数据DTO
 * 封装信标UUID、RSSI、距离等信息
 */
@Data
public class BeaconData {
    @NotNull(message = "信标UUID不能为空")
    @Pattern(regexp = "^[0-9a-fA-F-]{32,36}$", message = "信标UUID格式无效")
    private String uuid;            // 信标UUID

    @NotNull(message = "信号强度不能为空")
    private Integer rssi;           // RSSI信号强度
    private Double distance;        // 计算距离
    private Long timestamp;         // 时间戳
    private String macAddress;      // MAC地址（可选）
    private Integer txPower;        // 发射功率（可选）
    private Boolean isValid = true; // 数据是否有效

    /**
     * 获取规范化UUID
     */
    public String getNormalizedUuid() {
        if (uuid == null) return null;
        return uuid.toLowerCase().replace("-", "");
    }

    /**
     * 验证信标数据的有效性
     */
    public boolean isValidBeacon() {
        return uuid != null && !uuid.trim().isEmpty() &&
                rssi != null && rssi < 0 && rssi >= -95;
    }
}

