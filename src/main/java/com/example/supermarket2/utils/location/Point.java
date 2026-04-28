package com.example.supermarket2.utils.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 坐标点工具类
 * 用于BFS和A*算法中的坐标处理，以及位置计算
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point {
    public int x;
    public int y;
    public int distance; // 用于BFS搜索的距离
    public Double weight; // 用于加权计算的权重

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(int x, int y, int distance) {
        this.x = x;
        this.y = y;
        this.distance = distance;
    }

    /**
     * 计算与另一个点的欧几里得距离
     */
    public double distanceTo(Point other) {
        if (other == null) return Double.MAX_VALUE;
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算与另一个点的曼哈顿距离
     */
    public int manhattanDistanceTo(Point other) {
        if (other == null) return Integer.MAX_VALUE;
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }

    /**
     * 检查点是否相等
     */
    public boolean equals(Point other) {
        if (other == null) return false;
        return this.x == other.x && this.y == other.y;
    }

    /**
     * 转换为字符串表示
     */
    public String toKey() {
        return x + "," + y;
    }

    /**
     * 从字符串解析点
     */
    public static Point fromKey(String key) {
        if (key == null || !key.contains(",")) return null;
        try {
            String[] parts = key.split(",");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            return new Point(x, y);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查点是否在指定范围内
     */
    public boolean isInBounds(int minX, int maxX, int minY, int maxY) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    /**
     * 移动到相邻点
     */
    public Point move(int dx, int dy) {
        return new Point(x + dx, y + dy, distance + 1);
    }

    /**
     * 获取所有相邻点（4方向）
     */
    public Point[] getNeighbors4() {
        return new Point[] {
                new Point(x - 1, y, distance + 1),
                new Point(x + 1, y, distance + 1),
                new Point(x, y - 1, distance + 1),
                new Point(x, y + 1, distance + 1)
        };
    }

    /**
     * 获取所有相邻点（8方向）
     */
    public Point[] getNeighbors8() {
        return new Point[] {
                new Point(x - 1, y, distance + 1),
                new Point(x + 1, y, distance + 1),
                new Point(x, y - 1, distance + 1),
                new Point(x, y + 1, distance + 1),
                new Point(x - 1, y - 1, distance + 1),
                new Point(x + 1, y - 1, distance + 1),
                new Point(x - 1, y + 1, distance + 1),
                new Point(x + 1, y + 1, distance + 1)
        };
    }
}
