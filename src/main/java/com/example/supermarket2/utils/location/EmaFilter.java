package com.example.supermarket2.utils.location;

import com.example.supermarket2.entity.location.Position;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 指数移动平均滤波器（EMA Filter）
 * <p>
 * 公式：smoothed = alpha * current + (1 - alpha) * previous
 * alpha 越小，平滑程度越高（对历史权重更大）；越大，响应越快。
 * 目的：消除小幅抖动，使轨迹平滑自然。
 * </p>
 */
@Slf4j
public class EmaFilter {

    /** 每个用户的上次平滑位置 */
    private final Map<String, Position> smoothedPositions = new ConcurrentHashMap<>();

    /** 平滑因子（0 < alpha ≤ 1） */
    private final double alpha;

    public EmaFilter(double alpha) {
        this.alpha = Math.max(0.01, Math.min(1.0, alpha));
    }

    /**
     * 对输入位置应用 EMA 平滑
     *
     * @param userId 用户 ID
     * @param pos    中值滤波后的位置
     * @return EMA 平滑后的位置
     */
    public Position apply(String userId, Position pos) {
        if (pos == null) {
            return null;
        }

        Position prev = smoothedPositions.get(userId);

        if (prev == null) {
            // 首帧直接采用
            smoothedPositions.put(userId, pos);
            return pos;
        }

        double smoothedX = alpha * pos.getX() + (1.0 - alpha) * prev.getX();
        double smoothedY = alpha * pos.getY() + (1.0 - alpha) * prev.getY();

        Position result = new Position(smoothedX, smoothedY,
                pos.getAlgorithm() + "_ema", pos.getQuality());
        smoothedPositions.put(userId, result);

        log.debug("EmaFilter: userId={}, alpha={}, EMA=({}, {})",
                userId, alpha, smoothedX, smoothedY);
        return result;
    }

    /**
     * 重置指定用户的平滑状态
     */
    public void reset(String userId) {
        smoothedPositions.remove(userId);
    }
}
