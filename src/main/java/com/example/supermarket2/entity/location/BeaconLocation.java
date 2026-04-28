package com.example.supermarket2.entity.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 信标位置配置实体
 * 存储信标UUID和坐标信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BeaconLocation {
    private String uuid;        // 信标UUID
    private Double x;           // X坐标
    private Double y;           // Y坐标
    private String name;        // 信标名称（可选）
    private Integer floor;      // 所在楼层
    private Double rssi0;       // 参考距离RSSI值（默认-55）
    private Double attenuation; // 衰减因子（默认3.2）

    public BeaconLocation(String uuid, Double x, Double y) {
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.rssi0 = -55.0;
        this.attenuation = 3.2;
    }

    /**
     * 获取规范化UUID（去除横杠并转为小写）
     */
    public String getNormalizedUuid() {
        if (uuid == null) return null;
        return uuid.toLowerCase().replace("-", "");
    }

    /**
     * 根据RSSI计算距离
     */
    public Double calculateDistance(Integer rssi) {
        if (rssi == null || rssi >= 0) return null;

        double refRssi = rssi0 != null ? rssi0 : -55.0;
        double n = attenuation != null ? attenuation : 3.2;

        // 使用对数路径损耗模型
        return Math.pow(10, (refRssi - rssi) / (10 * n));
    }
}
