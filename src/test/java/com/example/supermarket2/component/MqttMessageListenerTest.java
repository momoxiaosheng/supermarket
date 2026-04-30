package com.example.supermarket2.component;

import com.example.supermarket2.service.DeviceDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MqttMessageListenerTest {

    @Mock
    private DeviceDataService deviceDataService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MqttMessageListener mqttMessageListener;

    @Test
    public void testMessageArrived() throws Exception {
        // 准备测试数据
        String topic = "/k2069zW7Drp/gwc_wxx/user/get";
        String payload = "{\"user_id\": \"123\", \"product_id\": 1001, \"quantity\": 1}";
        MqttMessage message = new MqttMessage(payload.getBytes());

        // 执行测试
        mqttMessageListener.messageArrived(topic, message);

        // 验证
        verify(deviceDataService, times(1)).processDeviceData(
                eq(topic), eq(payload), any());
    }

    @Test
    public void testMessageArrivedWithInvalidTopic() throws Exception {
        // 准备测试数据 - 无效的topic格式
        String topic = "/invalid/topic";
        String payload = "{\"user_id\": \"123\", \"product_id\": 1001, \"quantity\": 1}";
        MqttMessage message = new MqttMessage(payload.getBytes());

        // 执行测试
        mqttMessageListener.messageArrived(topic, message);

        // 验证 - 应该使用解析出的设备标识
        verify(deviceDataService, times(1)).processDeviceData(
                eq(topic), eq(payload), any());
    }
}
