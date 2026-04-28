package com.example.supermarket2.utils.location;

import com.example.supermarket2.entity.location.Position;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 位置计算工具类
 * 提供位置相关的计算和转换方法
 */
@Slf4j
public class PositionUtils {

    private PositionUtils() {
        // 工具类，防止实例化
    }

    /**
     * 计算两点之间的欧几里得距离
     */
    public static double calculateDistance(Position p1, Position p2) {
        if (p1 == null || p2 == null) {
            return Double.MAX_VALUE;
        }

        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 计算两点之间的曼哈顿距离
     */
    public static double calculateManhattanDistance(Position p1, Position p2) {
        if (p1 == null || p2 == null) {
            return Double.MAX_VALUE;
        }

        return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
    }

    /**
     * 计算多个位置的中心点
     */
    public static Position calculateCentroid(List<Position> positions) {
        if (positions == null || positions.isEmpty()) {
            return null;
        }

        double sumX = 0;
        double sumY = 0;
        int count = 0;

        for (Position pos : positions) {
            if (pos != null && pos.isValid()) {
                sumX += pos.getX();
                sumY += pos.getY();
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return new Position(sumX / count, sumY / count, "centroid");
    }

    /**
     * 计算加权中心点
     */
    public static Position calculateWeightedCentroid(List<Position> positions, List<Double> weights) {
        if (positions == null || weights == null ||
                positions.size() != weights.size() || positions.isEmpty()) {
            return null;
        }

        double sumX = 0;
        double sumY = 0;
        double totalWeight = 0;

        for (int i = 0; i < positions.size(); i++) {
            Position pos = positions.get(i);
            Double weight = weights.get(i);

            if (pos != null && pos.isValid() && weight != null && weight > 0) {
                sumX += pos.getX() * weight;
                sumY += pos.getY() * weight;
                totalWeight += weight;
            }
        }

        if (totalWeight == 0) {
            return calculateCentroid(positions);
        }

        return new Position(sumX / totalWeight, sumY / totalWeight, "weighted_centroid");
    }

    /**
     * 检查位置是否在地图边界内
     */
    public static boolean isWithinBounds(Position position, double minX, double maxX, double minY, double maxY) {
        if (position == null || !position.isValid()) {
            return false;
        }

        return position.getX() >= minX && position.getX() <= maxX &&
                position.getY() >= minY && position.getY() <= maxY;
    }

    /**
     * 约束位置到地图边界内
     */
    public static Position constrainToBounds(Position position, double minX, double maxX, double minY, double maxY) {
        if (position == null) {
            return null;
        }

        double constrainedX = Math.max(minX, Math.min(maxX, position.getX()));
        double constrainedY = Math.max(minY, Math.min(maxY, position.getY()));

        if (constrainedX != position.getX() || constrainedY != position.getY()) {
            log.debug("位置约束: ({}, {}) -> ({}, {})",
                    position.getX(), position.getY(), constrainedX, constrainedY);
        }

        return new Position(constrainedX, constrainedY, position.getAlgorithm(), "constrained");
    }

    /**
     * 计算移动速度（单位：格/秒）
     */
    public static double calculateSpeed(Position start, Position end, long timeMillis) {
        if (start == null || end == null || timeMillis <= 0) {
            return 0.0;
        }

        double distance = calculateDistance(start, end);
        double timeSeconds = timeMillis / 1000.0;

        return timeSeconds > 0 ? distance / timeSeconds : 0.0;
    }

    /**
     * 插值计算中间位置
     */
    public static Position interpolate(Position start, Position end, double ratio) {
        if (start == null || end == null) {
            return null;
        }

        ratio = Math.max(0, Math.min(1, ratio));

        double x = start.getX() + (end.getX() - start.getX()) * ratio;
        double y = start.getY() + (end.getY() - start.getY()) * ratio;

        return new Position(x, y, "interpolated");
    }

    /**
     * 平滑位置轨迹（移动平均）
     */
    public static Position smoothTrajectory(List<Position> recentPositions) {
        if (recentPositions == null || recentPositions.isEmpty()) {
            return null;
        }

        return calculateCentroid(recentPositions);
    }

    /**
     * 预测下一个位置（简单线性预测）
     */
    public static Position predictNextPosition(Position current, Position previous, double scale) {
        if (current == null || previous == null) {
            return current;
        }

        double dx = current.getX() - previous.getX();
        double dy = current.getY() - previous.getY();

        double predictedX = current.getX() + dx * scale;
        double predictedY = current.getY() + dy * scale;

        return new Position(predictedX, predictedY, "predicted");
    }
}

