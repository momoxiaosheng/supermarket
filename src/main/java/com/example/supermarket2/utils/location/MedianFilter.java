package com.example.supermarket2.utils.location;

import com.example.supermarket2.entity.location.Position;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 中值滤波器（Median Filter）
 * <p>
 * 维护每个用户最近 windowSize 帧的位置队列。
 * 每次输入新位置后，分别对 X、Y 轴取中位数作为输出。
 * 目的：消除零星的脉冲噪声。
 * </p>
 */
@Slf4j
public class MedianFilter {

    /** 每个用户的位置滑动窗口 */
    private final Map<String, List<Position>> windows = new ConcurrentHashMap<>();

    /** 滑动窗口大小（帧数） */
    private final int windowSize;

    public MedianFilter(int windowSize) {
        this.windowSize = Math.max(1, windowSize);
    }

    /**
     * 对输入位置应用中值滤波
     *
     * @param userId 用户 ID
     * @param pos    限幅滤波后的位置
     * @return 中值滤波后的位置
     */
    public Position apply(String userId, Position pos) {
        if (pos == null) {
            return null;
        }

        List<Position> window = windows.computeIfAbsent(userId, k -> new ArrayList<>());
        window.add(pos);

        // 保持窗口大小
        if (window.size() > windowSize) {
            window.remove(0);
        }

        if (window.size() == 1) {
            return pos;
        }

        // 分别对 X、Y 取中位数
        double medianX = median(window.stream().map(Position::getX).sorted().toList());
        double medianY = median(window.stream().map(Position::getY).sorted().toList());

        Position result = new Position(medianX, medianY,
                pos.getAlgorithm() + "_median", pos.getQuality());
        log.debug("MedianFilter: userId={}, 窗口={}, 中值=({}，{})",
                userId, window.size(), medianX, medianY);
        return result;
    }

    /** 计算已排序列表的中位数 */
    private double median(List<Double> sorted) {
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    /**
     * 重置指定用户的窗口
     */
    public void reset(String userId) {
        windows.remove(userId);
    }
}
