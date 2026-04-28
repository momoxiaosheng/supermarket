package com.example.supermarket2.config;

import com.example.supermarket2.component.MqttMessageListener;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MqttConfig {

    private static final Logger logger = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${aliyun.iot.mqtt.username}")
    private String username;

    @Value("${aliyun.iot.mqtt.password}")
    private String password;

    @Value("${aliyun.iot.mqtt.clientId}")
    private String clientId;

    @Value("${aliyun.iot.mqttHostUrl}")
    private String mqttHostUrl;

    @Value("${aliyun.iot.port:1883}")
    private int port;

    @Value("${aliyun.iot.topic}")
    private String topic;

    @Autowired
    private MqttMessageListener mqttMessageListener;

    private MqttClient mqttClient;

    @Bean
    public MqttClient mqttClient() {
        try {
            // 打印连接参数（调试用）
            logger.info("MQTT连接参数 - Username: {}, ClientId: {}", username, clientId);

            // 构建MQTT服务器地址
            String broker = "tcp://" + mqttHostUrl + ":" + port;
            logger.info("连接MQTT服务器: {}", broker);

            // 创建MQTT客户端
            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());

            // 配置连接选项
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(30);
            connOpts.setKeepAliveInterval(60);
            connOpts.setAutomaticReconnect(true); // 启用自动重连

            // 连接MQTT服务器
            mqttClient.connect(connOpts);
            logger.info("成功连接到MQTT服务器");

            // 订阅主题
            mqttClient.subscribe(topic, 0, mqttMessageListener);
            logger.info("已订阅主题: {}", topic);

            return mqttClient;
        } catch (MqttException e) {
            logger.error("MQTT客户端初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("MQTT客户端初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                logger.info("MQTT客户端已断开连接");
            } catch (MqttException e) {
                logger.error("断开MQTT连接时出错: {}", e.getMessage(), e);
            }
        }
    }
}