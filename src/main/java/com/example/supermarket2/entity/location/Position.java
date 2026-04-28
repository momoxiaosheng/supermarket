package com.example.supermarket2.entity.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置坐标实体
 * 存储坐标、算法类型、质量等信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Position {
    private Double x;
    private Double y;
    private String algorithm;  // 使用的定位算法：weighted_centroid, trilateration, conservative
    private String quality;    // 定位质量：high, medium, low, poor, predicted

    // 简化构造函数
    public Position(Double x, Double y) {
        this.x = x;
        this.y = y;
    }

    public Position(Double x, Double y, String algorithm) {
        this.x = x;
        this.y = y;
        this.algorithm = algorithm;
    }

    /**
     * 检查位置是否有效
     */
    public boolean isValid() {
        return x != null && y != null &&
                !x.isNaN() && !y.isNaN() &&
                x >= 0 && x <= 100 &&
                y >= 0 && y <= 100;
    }

    /**
     * 计算与另一个位置的距离
     */
    public double distanceTo(Position other) {
        if (other == null) return Double.MAX_VALUE;
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

