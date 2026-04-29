package com.example.supermarket2.utils.location;

import com.example.supermarket2.entity.location.Position;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限幅滤波器（Jump Limiter）
 * <p>
 * 检测两帧之间的位置跳变距离。若跳变超过 maxJumpDistance，
 * 则沿当前方向移动最多 maxJumpDistance，而不是直接跳到新位置。
 * 目的：剔除因 RSSI 瞬间噪声导致的大幅跳变。
 * </p>
 */
@Slf4j
public class JumpLimiter {

    /** 每个用户的上次输出位置 */
    private final Map<String, Position> lastPositions = new ConcurrentHashMap<>();

    /** 最大允许跳变距离（米） */
    private final double maxJumpDistance;

    public JumpLimiter(double maxJumpDistance) {
        this.maxJumpDistance = maxJumpDistance;
    }

    /**
     * 对原始位置应用限幅处理
     *
     * @param userId   用户 ID（维护各用户独立状态）
     * @param rawPos   本帧原始位置
     * @return 限幅后的位置
     */
    public Position apply(String userId, Position rawPos) {
        if (rawPos == null) {
            return null;
        }

        Position last = lastPositions.get(userId);

        if (last == null) {
            // 首帧直接采用
            lastPositions.put(userId, rawPos);
            return rawPos;
        }

        double dx = rawPos.getX() - last.getX();
        double dy = rawPos.getY() - last.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        Position result;
        if (dist > maxJumpDistance && dist > 0) {
            // 限制跳变：沿方向移动最多 maxJumpDistance
            double ratio = maxJumpDistance / dist;
            double limitedX = last.getX() + dx * ratio;
            double limitedY = last.getY() + dy * ratio;
            result = new Position(limitedX, limitedY,
                    rawPos.getAlgorithm() + "_jump_limited", rawPos.getQuality());
            log.debug("JumpLimiter: userId={}, 跳变距离={:.2f}m > 阈值={:.2f}m, 已限幅",
                    userId, dist, maxJumpDistance);
        } else {
            result = rawPos;
        }

        lastPositions.put(userId, result);
        return result;
    }

    /**
     * 重置指定用户的状态（用户退出时调用）
     */
    public void reset(String userId) {
        lastPositions.remove(userId);
    }
}
