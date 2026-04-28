package com.example.supermarket2.common.constants;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class RedisKeyConstants {
    public static final String CART_USER_KEY = "cart:user:%s";
    public static final String SEARCH_HISTORY_KEY = "search:history:%s";
    public static final String SEARCH_HOT_KEY = "search:hot:keywords";
    public static final String STOCK_LOCK_KEY = "stock:lock:%s";

    /**
     * 获取当前时间到今天 23:59:59 的剩余秒数
     */
    public static final long getSecondsUntilEndOfDay() {
        LocalDateTime now = LocalDateTime.now();
        // 获取当天的最后一刻 (23:59:59.999999999)
        LocalDateTime endOfDay = now.with(LocalTime.MAX);

        // 计算时间差
        long seconds = Duration.between(now, endOfDay).toSeconds();

        // 防止极端情况下出现负数（例如刚好在 23:59:59.999 触发）
        return Math.max(seconds, 1);
    }
}
