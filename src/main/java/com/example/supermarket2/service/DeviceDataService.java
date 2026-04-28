package com.example.supermarket2.service;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.supermarket2.entity.DeviceWeightRecord;
import com.example.supermarket2.entity.Product;
import com.example.supermarket2.mapper.DeviceWeightRecordMapper;
import com.example.supermarket2.mapper.ProductMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 华为云IoT设备数据处理服务
 * 负责解析设备上报数据，更新购物车，并推送WebSocket通知
 */
@Slf4j
@Service
public class DeviceDataService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private DeviceWeightRecordMapper deviceWeightRecordMapper;

    @Autowired
    private CartService cartService;

    private static final Map<String, Long> DEFAULT_MAPPING = new ConcurrentHashMap<>();

    static {
        DEFAULT_MAPPING.put("cart_001", 1L);
        DEFAULT_MAPPING.put("69e60e4b7f2e6c302f68f4b6_test", 1L);
    }

    /**
     * 从 MQTT topic 中提取 deviceId
     * topic 格式: $oc/devices/{device_id}/sys/messages/down
     */
    private String extractDeviceId(String topic) {
        if (topic == null || topic.isEmpty()) {
            return null;
        }
        String[] parts = topic.split("/");
        if (parts.length >= 3) {
            return parts[2];
        }
        return null;
    }

    /**
     * 根据 deviceId 获取对应用户ID（默认用户1）
     */
    private Long getUserIdByDeviceId(String deviceId) {
        if (deviceId == null) {
            return 1L;
        }
        Long userId = DEFAULT_MAPPING.get(deviceId);
        if (userId == null) {
            log.warn("设备ID [{}] 未找到映射，使用默认用户1", deviceId);
            return 1L;
        }
        return userId;
    }

    /**
     * 从 AMQP payload 中提取 deviceId（某些 AMQP 消息会把设备信息放在 payload 中）
     */
    private String extractDeviceIdFromPayload(JSONObject json) {
        // 尝试从 content.deviceId 或直接 deviceId 字段提取
        JSONObject content = json.getJSONObject("content");
        if (content != null) {
            String deviceId = content.getString("deviceId");
            if (deviceId != null) return deviceId;
        }
        String deviceId = json.getString("deviceId");
        return deviceId;
    }

    /**
     * 主入口：MQTT 消息触发（3参数）
     */
    public void processDeviceData(String topic, String payload, Object unused) {
        log.info("收到设备上报消息，topic={}", topic);
        doProcess(topic, payload);
    }

    /**
     * AMQP 消息触发（单参数 payload）
     */
    public void processDeviceData(String payload) {
        log.info("AMQP收到设备数据: {}", payload);
        doProcess(null, payload);
    }

    /**
     * 核心处理逻辑
     */
    private void doProcess(String topic, String payload) {
        try {
            JSONObject json = JSONObject.parseObject(payload);

            // 从 topic 中提取 deviceId（MQTT有topic，AMQP从payload中尝试提取）
            String deviceId = null;
            if (topic != null) {
                deviceId = extractDeviceId(topic);
            }
            if (deviceId == null) {
                deviceId = extractDeviceIdFromPayload(json);
            }
            Long userId = getUserIdByDeviceId(deviceId);

            // 提取 content.services 数组
            // 兼容三种结构：
            // 1. content.services（华为云标准服务格式）
            // 2. content.content.services（华为云命令下发多包了一层）
            // 3. content{product_id, quantity}（直接下发，智能购物车专用）
            JSONObject content = json.getJSONObject("content");
            if (content == null) {
                log.warn("消息中无 content 字段，跳过: {}", payload);
                return;
            }

            JSONArray services = content.getJSONArray("services");
            if (services == null || services.isEmpty()) {
                // 尝试从 content.content 中获取（华为云命令下发多包了一层）
                JSONObject innerContent = content.getJSONObject("content");
                if (innerContent != null) {
                    services = innerContent.getJSONArray("services");
                }
                // 如果还是没有 services，直接取 content 中的字段（智能购物车直接下发格式）
                if (services == null || services.isEmpty()) {
                    Integer productId = content.getInteger("product_id");
                    if (productId != null) {
                        // 格式3: content{product_id, quantity} 直接处理
                        handleSmartCartData(deviceId, userId, content);
                        return;
                    }
                    log.warn("消息中无 services 字段，跳过: {}", payload);
                    return;
                }
            }

            for (int i = 0; i < services.size(); i++) {
                JSONObject service = services.getJSONObject(i);
                String serviceId = service.getString("service_id");
                JSONObject properties = service.getJSONObject("properties");

                if (properties == null) {
                    continue;
                }

                if ("SmartCart".equals(serviceId)) {
                    handleSmartCartData(deviceId, userId, properties);
                } else {
                    log.info("未知服务类型: {}，跳过", serviceId);
                }
            }

        } catch (Exception e) {
            log.error("解析设备数据异常，原始payload={}", payload, e);
        }
    }

    /**
     * 处理智能购物车数据：商品称重 -> 入库 -> 更新购物车 -> WebSocket推送
     */
    private void handleSmartCartData(String deviceId, Long userId, JSONObject properties) {
        // 提取字段（华为云使用下划线命名）
        Integer productId = properties.getInteger("product_id");
        Double quantityGrams = properties.getDouble("quantity"); // 单位：克

        if (productId == null || quantityGrams == null) {
            log.warn("SmartCart 数据缺少必要字段，properties={}", properties);
            return;
        }

        log.info("[SmartCart] 设备={}, 用户={}, 商品ID={}, 重量={}g", deviceId, userId, productId, quantityGrams);

        // 查询商品
        Product product = productMapper.selectProductById(productId.longValue());
        if (product == null || product.getStatus() != 1) {
            log.warn("商品ID [{}] 不存在或已下架，跳过", productId);
            return;
        }

        // 1. 记录到数据库
        DeviceWeightRecord record = new DeviceWeightRecord();
        record.setDeviceId(deviceId);
        record.setUserId(userId);
        record.setProductId(productId.longValue());
        record.setProductName(product.getName());
        record.setQuantityGrams(quantityGrams);
        record.setReportTime(new Date());
        record.setCreateTime(new Date());
        deviceWeightRecordMapper.insert(record);
        log.info("[SmartCart] 称重记录已入库，recordId={}", record.getId());

        // 2. 更新购物车（CartServiceImpl 内部已处理 WebSocket 推送）
        cartService.addToCart(userId, productId.longValue(), quantityGrams);
        log.info("[SmartCart] 购物车已更新，用户={}, 商品={}, 重量={}g", userId, product.getName(), quantityGrams);

        log.info("[SmartCart] 数据处理完成");
    }
}
