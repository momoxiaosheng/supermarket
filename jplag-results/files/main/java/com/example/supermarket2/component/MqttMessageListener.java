package com.example.supermarket2.component;

import com.example.supermarket2.service.DeviceDataService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageListener implements IMqttMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);

    @Autowired
    private DeviceDataService deviceDataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("接收到MQTT消息 - Topic: {}", topic);

            // 1. 接收消息并转换为字符串
            String payload = new String(message.getPayload(), "UTF-8");
            logger.debug("原始消息内容: {}", payload);

            // 2. 解析JSON消息
            JsonNode jsonNode = objectMapper.readTree(payload);

            // 3. 提取设备标识（从Topic中提取）
            String[] topicParts = topic.split("/");
            String deviceIdentifier = extractDeviceIdentifier(topicParts);

            // 4. 处理设备数据
            deviceDataService.processDeviceData(deviceIdentifier, topic, jsonNode);

            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("消息处理完成 - 设备: {}, 处理时间: {}ms", deviceIdentifier, processingTime);

        } catch (Exception e) {
            logger.error("消息处理失败 - Topic: {}, 错误: {}", topic, e.getMessage(), e);
            // 根据业务需求，这里可以决定是否重新抛出异常
            // 如果重新抛出，MQTT客户端可能会尝试重新投递消息
            throw e;
        }
    }

    /**
     * 从Topic中提取设备标识
     */
    private String extractDeviceIdentifier(String[] topicParts) {
        // 根据阿里云IoT平台Topic格式提取设备信息
        // 典型格式: /sys/{productKey}/{deviceName}/thing/event/property/post
        if (topicParts.length >= 3 && !topicParts[1].isEmpty() && !topicParts[2].isEmpty()) {
            String productKey = topicParts[1];
            String deviceName = topicParts[2];
            return productKey + ":" + deviceName;
        }

        // 如果无法从Topic中提取，返回未知设备
        return "unknown:unknown";
    }
}