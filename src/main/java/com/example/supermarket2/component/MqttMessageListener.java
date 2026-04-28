package com.example.supermarket2.component;

import com.example.supermarket2.service.DeviceDataService;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MqttMessageListener implements IMqttMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MqttMessageListener.class);

    // 构造器注入（Spring官方推荐，解决字段注入警告）
    private final DeviceDataService deviceDataService;

    public MqttMessageListener(DeviceDataService deviceDataService) {
        this.deviceDataService = deviceDataService;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            logger.info("收到华为云IoT消息: topic={}, payload={}", topic, payload);

            // 与测试类完全对齐的3个参数调用
            deviceDataService.processDeviceData(topic, payload, null);

        } catch (Exception e) {
            logger.error("处理MQTT消息异常，topic={}", topic, e);
        }
    }
}