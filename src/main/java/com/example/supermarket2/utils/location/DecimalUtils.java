package com.example.supermarket2.utils.location;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 小数处理工具类
 * 处理BigDecimal的精度和舍入，确保数值计算的准确性
 */
@Slf4j
public class DecimalUtils {

    private DecimalUtils() {
        // 工具类，防止实例化
    }

    // 默认精度尺度
    private static final int DEFAULT_SCALE = 6;

    /**
     * 四舍五入到指定精度
     */
    public static double round(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            log.warn("尝试对无效数值进行舍入: {}", value);
            return 0.0;
        }

        try {
            BigDecimal bd = BigDecimal.valueOf(value);
            bd = bd.setScale(scale, RoundingMode.HALF_UP);
            return bd.doubleValue();
        } catch (Exception e) {
            log.error("数值舍入失败: value={}, scale={}", value, scale, e);
            return value;
        }
    }

    /**
     * 四舍五入到默认精度
     */
    public static double round(double value) {
        return round(value, DEFAULT_SCALE);
    }

    /**
     * 安全的加法运算
     */
    public static double add(double a, double b) {
        if (Double.isNaN(a) || Double.isInfinite(a) ||
                Double.isNaN(b) || Double.isInfinite(b)) {
            log.warn("无效的加法操作数: {} + {}", a, b);
            return 0.0;
        }

        try {
            BigDecimal bd1 = BigDecimal.valueOf(a);
            BigDecimal bd2 = BigDecimal.valueOf(b);
            return bd1.add(bd2).doubleValue();
        } catch (Exception e) {
            log.error("加法运算失败: {} + {}", a, b, e);
            return a + b; // 回退到原生加法
        }
    }

    /**
     * 安全的减法运算
     */
    public static double subtract(double a, double b) {
        if (Double.isNaN(a) || Double.isInfinite(a) ||
                Double.isNaN(b) || Double.isInfinite(b)) {
            log.warn("无效的减法操作数: {} - {}", a, b);
            return 0.0;
        }

        try {
            BigDecimal bd1 = BigDecimal.valueOf(a);
            BigDecimal bd2 = BigDecimal.valueOf(b);
            return bd1.subtract(bd2).doubleValue();
        } catch (Exception e) {
            log.error("减法运算失败: {} - {}", a, b, e);
            return a - b; // 回退到原生减法
        }
    }

    /**
     * 安全的乘法运算
     */
    public static double multiply(double a, double b) {
        if (Double.isNaN(a) || Double.isInfinite(a) ||
                Double.isNaN(b) || Double.isInfinite(b)) {
            log.warn("无效的乘法操作数: {} * {}", a, b);
            return 0.0;
        }

        try {
            BigDecimal bd1 = BigDecimal.valueOf(a);
            BigDecimal bd2 = BigDecimal.valueOf(b);
            return bd1.multiply(bd2).doubleValue();
        } catch (Exception e) {
            log.error("乘法运算失败: {} * {}", a, b, e);
            return a * b; // 回退到原生乘法
        }
    }

    /**
     * 安全的除法运算
     */
    public static double divide(double a, double b) {
        return divide(a, b, DEFAULT_SCALE);
    }

    /**
     * 安全的除法运算（指定精度）
     */
    public static double divide(double a, double b, int scale) {
        if (Double.isNaN(a) || Double.isInfinite(a) ||
                Double.isNaN(b) || Double.isInfinite(b)) {
            log.warn("无效的除法操作数: {} / {}", a, b);
            return 0.0;
        }

        if (b == 0) {
            log.warn("除法运算除数为零: {} / {}", a, b);
            return Double.MAX_VALUE * Math.signum(a);
        }

        try {
            BigDecimal bd1 = BigDecimal.valueOf(a);
            BigDecimal bd2 = BigDecimal.valueOf(b);
            return bd1.divide(bd2, scale, RoundingMode.HALF_UP).doubleValue();
        } catch (Exception e) {
            log.error("除法运算失败: {} / {}", a, b, e);
            return a / b; // 回退到原生除法
        }
    }

    /**
     * 比较两个double值（考虑精度误差）
     */
    public static boolean equals(double a, double b, double epsilon) {
        if (Double.isNaN(a) && Double.isNaN(b)) return true;
        if (Double.isNaN(a) || Double.isNaN(b)) return false;

        return Math.abs(a - b) <= epsilon;
    }

    /**
     * 比较两个double值（使用默认精度误差）
     */
    public static boolean equals(double a, double b) {
        return equals(a, b, 1e-10);
    }

    /**
     * 限制数值在指定范围内
     */
    public static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 计算平均值（避免数值溢出）
     */
    public static double average(double[] values) {
        if (values == null || values.length == 0) {
            return Double.NaN;
        }

        try {
            BigDecimal sum = BigDecimal.ZERO;
            for (double value : values) {
                sum = sum.add(BigDecimal.valueOf(value));
            }
            return sum.divide(BigDecimal.valueOf(values.length), DEFAULT_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();
        } catch (Exception e) {
            log.error("平均值计算失败", e);

            // 回退到简单平均
            double simpleSum = 0;
            for (double value : values) {
                simpleSum += value;
            }
            return simpleSum / values.length;
        }
    }

    /**
     * 计算加权平均值
     */
    public static double weightedAverage(double[] values, double[] weights) {
        if (values == null || weights == null ||
                values.length != weights.length || values.length == 0) {
            return Double.NaN;
        }

        try {
            BigDecimal weightedSum = BigDecimal.ZERO;
            BigDecimal weightSum = BigDecimal.ZERO;

            for (int i = 0; i < values.length; i++) {
                BigDecimal value = BigDecimal.valueOf(values[i]);
                BigDecimal weight = BigDecimal.valueOf(weights[i]);
                weightedSum = weightedSum.add(value.multiply(weight));
                weightSum = weightSum.add(weight);
            }

            if (weightSum.compareTo(BigDecimal.ZERO) == 0) {
                return average(values);
            }

            return weightedSum.divide(weightSum, DEFAULT_SCALE, RoundingMode.HALF_UP)
                    .doubleValue();
        } catch (Exception e) {
            log.error("加权平均值计算失败", e);
            return Double.NaN;
        }
    }
}
