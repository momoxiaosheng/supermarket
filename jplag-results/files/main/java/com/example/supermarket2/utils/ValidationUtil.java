package com.example.supermarket2.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtil {

    private static final Logger logger = LoggerFactory.getLogger(ValidationUtil.class);

    /**
     * 验证设备数据格式是否有效（适配阿里云IoT平台格式）
     * 有效格式：{"params":{"product_id":1,"Weight":275.7},"version":"1.0.0"}
     */
    public static boolean isValidDeviceData(JsonNode data) {
        // 1. 基础检查：数据不能为null
        if (data == null || data.isNull()) {
            logger.warn("设备数据为空");
            return false;
        }

        // 2. 检查是否有params节点，如果没有则使用根节点
        JsonNode paramsNode = data.has("params") ? data.get("params") : data;

        // 3. 检查必要字段是否存在
        boolean hasProductId = paramsNode.has("product_id");
        boolean hasQuantity = paramsNode.has("quantity");
        boolean hasWeight = paramsNode.has("Weight") || paramsNode.has("weight");

        // 4. 验证必须包含产品ID和数量或重量中的一个
        if (!hasProductId) {
            logger.warn("设备数据缺少必要字段: product_id");
            return false;
        }

        if (!hasQuantity && !hasWeight) {
            logger.warn("设备数据必须包含 quantity 或 weight/Weight 字段");
            return false;
        }

        // 5. 验证字段格式有效性
        try {
            // 验证product_id格式（必须是数字）
            if (!paramsNode.get("product_id").isNumber()) {
                logger.warn("product_id必须是数字类型");
                return false;
            }
            long productId = paramsNode.get("product_id").asLong();
            if (productId <= 0) {
                logger.warn("product_id必须是正数");
                return false;
            }

            // 验证quantity格式（如果存在）
            if (hasQuantity) {
                if (!paramsNode.get("quantity").isNumber()) {
                    logger.warn("quantity必须是数字类型");
                    return false;
                }
                int quantity = paramsNode.get("quantity").asInt();
                if (quantity <= 0) {
                    logger.warn("quantity必须是正整数");
                    return false;
                }
            }

            // 验证weight格式（如果存在）
            if (hasWeight) {
                JsonNode weightNode = paramsNode.has("Weight") ?
                        paramsNode.get("Weight") : paramsNode.get("weight");

                if (!weightNode.isNumber()) {
                    logger.warn("weight必须是数字类型");
                    return false;
                }
                double weight = weightNode.asDouble();
                if (weight <= 0) {
                    logger.warn("weight必须是正数");
                    return false;
                }
            }

        } catch (Exception e) {
            logger.warn("设备数据字段解析异常: {}", e.getMessage());
            return false;
        }

        // 所有验证通过
        return true;
    }

    /**
     * 验证数值范围
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * 验证字符串不为空
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
}