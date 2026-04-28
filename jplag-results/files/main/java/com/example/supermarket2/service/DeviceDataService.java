package com.example.supermarket2.service;

import com.example.supermarket2.handler.CartWebSocketHandler;
import com.example.supermarket2.utils.ValidationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeviceDataService {

    private static final Logger logger = LoggerFactory.getLogger(DeviceDataService.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private CartWebSocketHandler cartWebSocketHandler;

    // 从配置文件中读取固定值，如果没有配置则使用默认值
    @Value("${device.fixed.userId:1}")
    private Long fixedUserId;

    @Value("${device.fixed.cartId:fixed_cart}")
    private String fixedCartId;

    /**
     * 处理设备数据
     */
    public void processDeviceData(String deviceIdentifier, String topic, JsonNode data) {
        try {
            // 1. 验证数据基本格式
            if (!ValidationUtil.isValidDeviceData(data)) {
                logger.warn("设备数据格式无效 - 设备: {}, 数据: {}", deviceIdentifier, data);
                return;
            }

            // 2. 解析业务字段
            DeviceData deviceData = parseDeviceData(deviceIdentifier, data);

            // 3. 根据设备类型和数据类型执行不同的业务逻辑
            if (isShoppingCartData(deviceData)) {
                processShoppingCartData(deviceData);
            } else {
                logger.info("未知数据类型 - 设备: {}, 数据: {}", deviceIdentifier, data);
            }

        } catch (Exception e) {
            logger.error("处理设备数据失败 - 设备: {}, 错误: {}", deviceIdentifier, e.getMessage(), e);
        }
    }

    /**
     * 解析设备数据 - 适配阿里云IoT平台格式
     */
    private DeviceData parseDeviceData(String deviceIdentifier, JsonNode data) {
        DeviceData deviceData = new DeviceData();
        deviceData.setDeviceIdentifier(deviceIdentifier);

        try {
            // 解析时间戳
            if (data.has("timestamp")) {
                deviceData.setTimestamp(data.get("timestamp").asLong());
            } else {
                deviceData.setTimestamp(System.currentTimeMillis());
            }

            // 使用固定值设置用户ID和购物车ID
            deviceData.setUserId(String.valueOf(fixedUserId));
            deviceData.setCartId(fixedCartId);

            // 阿里云IoT平台数据通常包含params节点
            JsonNode paramsNode = data.has("params") ? data.get("params") : data;

            // 解析商品ID
            if (paramsNode.has("product_id")) {
                deviceData.setProductId(paramsNode.get("product_id").asLong());
            }

            // 解析数量
            if (paramsNode.has("quantity")) {
                deviceData.setQuantity(paramsNode.get("quantity").asInt());
            }

            // 解析重量 - 注意字段名可能是"Weight"（首字母大写）
            if (paramsNode.has("Weight")) {
                deviceData.setWeight(paramsNode.get("Weight").asDouble());
            } else if (paramsNode.has("weight")) {
                deviceData.setWeight(paramsNode.get("weight").asDouble());
            }

        } catch (Exception e) {
            logger.warn("解析设备数据时发生异常 - 设备: {}, 数据: {}", deviceIdentifier, data, e);
        }

        return deviceData;
    }

    /**
     * 判断是否为购物车数据
     */
    private boolean isShoppingCartData(DeviceData deviceData) {
        return deviceData.getUserId() != null &&
                deviceData.getProductId() != null &&
                (deviceData.getQuantity() != null || deviceData.getWeight() != null);
    }

    /**
     * 处理购物车数据
     */
    private void processShoppingCartData(DeviceData deviceData) {
        try {
            String userIdStr = deviceData.getUserId();
            Long productId = deviceData.getProductId();

            // 1. 转换用户ID（保留原逻辑，确保格式正确）
            Long userId = null;
            try {
                userId = Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                logger.error("用户ID格式错误: {}", userIdStr);
                return;
            }

            // 定义变量：区分计件商品（quantity）和称重商品（weightInKg）
            Integer quantity = null; // 仅用于普通计件商品
            Double weightInKg = null; // 仅用于称重商品（已转千克单位）

            // 2. 处理普通计件商品（保留原逻辑，使用quantity）
            if (deviceData.getQuantity() != null) {
                quantity = deviceData.getQuantity();
                logger.info("处理普通计件商品 - 用户: {}, 商品: {}, 数量: {}", userId, productId, quantity);
            }
            // 3. 处理称重商品（核心修改：仅克→千克转换，不转数量）
            else if (deviceData.getWeight() != null) {
                Double weightInGram = deviceData.getWeight(); // 假设原weight单位为「克」
                // 克转千克：除以1000.0（保留小数精度，避免整数除法丢失数据）
                weightInKg = weightInGram / 1000.0;
                // 日志打印转换前后的重量，便于溯源
                logger.info("处理称重商品 - 用户: {}, 商品: {}, 原重量: {}g, 转换后重量: {}kg",
                        userId, productId, weightInGram, String.format("%.4f", weightInKg)); // 保留4位小数，适配实际业务精度
            }

            // 4. 有效性校验：数量（计件）或重量（称重）需非空且大于0
            boolean isDataValid = false;
            if (quantity != null && quantity > 0) {
                isDataValid = true; // 计件商品：数量有效
            } else if (weightInKg != null && weightInKg > 0) {
                isDataValid = true; // 称重商品：重量有效（排除0或负数）
            }

            // 5. 调用购物车服务存入数据库（修复逻辑：分别调用对应的方法）
            if (isDataValid && productId != null && productId > 0) {
                if (quantity != null && quantity > 0) {
                    // 调用购物车服务添加计件商品
                    cartService.addToCart(userId, productId, quantity);
                    logger.info("成功添加计件商品到购物车 - 用户: {}, 商品: {}, 数量: {}", userId, productId, quantity);
                } else if (weightInKg != null && weightInKg > 0) {
                    // 调用购物车服务添加称重商品
                    cartService.addToCart(userId, productId, weightInKg);
                    logger.info("成功添加称重商品到购物车 - 用户: {}, 商品: {}, 重量: {}kg", userId, productId, weightInKg);
                }

                // 6. 保留WebSocket推送逻辑（原功能不变）
                try {
                    cartWebSocketHandler.sendCartUpdateToUser(userId);
                    logger.info("已发送WebSocket更新通知 - 用户: {}", userId);
                } catch (Exception e) {
                    logger.error("发送WebSocket更新失败 - 用户: {}, 错误: {}", userId, e.getMessage());
                }
            } else {
                // 无效数据日志：明确标注是数量还是重量问题
                logger.warn("无效的商品数据 - 用户: {}, 商品: {}, 计件数量: {}, 称重重量: {}kg（需非空且大于0）",
                        userId, productId, quantity, weightInKg);
            }

        } catch (Exception e) {
            logger.error("处理购物车数据失败 - 设备: {}, 错误: {}",
                    deviceData.getDeviceIdentifier(), e.getMessage(), e);
        }
    }

    /**
     * 设备数据内部类
     */
    private static class DeviceData {
        private String deviceIdentifier;
        private Long timestamp;
        private String userId;
        private String cartId;
        private Long productId;
        private Integer quantity;
        private Double weight;

        // Getter和Setter方法
        public String getDeviceIdentifier() { return deviceIdentifier; }
        public void setDeviceIdentifier(String deviceIdentifier) { this.deviceIdentifier = deviceIdentifier; }

        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getCartId() { return cartId; }
        public void setCartId(String cartId) { this.cartId = cartId; }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
    }
}