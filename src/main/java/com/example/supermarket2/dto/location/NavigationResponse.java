package com.example.supermarket2.dto.location;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NavigationResponse {
    private boolean success;
    private String message;
    private List<GridPoint> path;
    private Double distance;
    private GridPoint originalTarget;   // 原始目标点
    private GridPoint adjustedTarget;   // 调整后的目标点（如果原始目标不可通行，则调整到最近可通行点）
    private Boolean targetAdjusted;     // 添加这个字段

    // GridPoint 类
    @Data
    @NoArgsConstructor
    public static class GridPoint {
        private int x;
        private int y;

        // 添加两个参数的构造函数
        public GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // 构建成功响应的方法 - 重载版本，支持更多参数
    public static NavigationResponse success(List<GridPoint> path, double distance) {
        return success(path, distance, null, null, false);
    }

    public static NavigationResponse success(List<GridPoint> path, double distance,
                                             GridPoint originalTarget, GridPoint adjustedTarget,
                                             boolean targetAdjusted) {
        NavigationResponse response = new NavigationResponse();
        response.setSuccess(true);
        response.setPath(path);
        response.setDistance(distance);
        response.setOriginalTarget(originalTarget);
        response.setAdjustedTarget(adjustedTarget);
        response.setTargetAdjusted(targetAdjusted);
        return response;
    }

    // 构建失败响应的方法
    public static NavigationResponse error(String message) {
        NavigationResponse response = new NavigationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setPath(List.of());
        response.setDistance(0.0);
        response.setTargetAdjusted(false);
        return response;
    }
}
