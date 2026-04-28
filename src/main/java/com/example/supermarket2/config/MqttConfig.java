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

    // ==================== 修改点1：前缀从 aliyun.iot 改为 huawei.iot.mqtt ====================
    @Value("${huawei.iot.mqtt.username}")
    private String username;

    @Value("${huawei.iot.mqtt.password}")
    private String password;

    @Value("${huawei.iot.mqtt.clientId}")
    private String clientId;

    @Value("${huawei.iot.mqtt.hostUrl}") // 修改点2：配置项名从 mqttHostUrl 改为 hostUrl
    private String mqttHostUrl;

    @Value("${huawei.iot.mqtt.port:8883}")
    private int port;

    @Value("${huawei.iot.mqtt.subscribeTopic}") // 修改点3：配置项名从 topic 改为 subscribeTopic
    private String subscribeTopic;

    @Autowired
    private MqttMessageListener mqttMessageListener;

    private MqttClient mqttClient;

    @Bean
    public MqttClient mqttClient() {
        try {
            logger.info("MQTT连接参数 - Username: {}, ClientId: {}", username, clientId);

            // 华为云IoT MQTT 连接地址（ssl:// 前缀保持不变，符合MQTTS协议要求）
            String broker = "ssl://" + mqttHostUrl + ":" + port;
            logger.info("连接MQTT服务器: {}", broker);

            mqttClient = new MqttClient(broker, clientId, new MemoryPersistence());

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setConnectionTimeout(30);
            connOpts.setKeepAliveInterval(60);
            connOpts.setAutomaticReconnect(true);
            connOpts.setHttpsHostnameVerificationEnabled(false);

            // 1. 先连接
            mqttClient.connect(connOpts);
            logger.info("成功连接到华为云IoT MQTT服务器");

            // 2. 后订阅 (单独 try-catch，订阅失败不影响项目启动)
            try {
                mqttClient.subscribe(subscribeTopic, 0, mqttMessageListener); // 修改点4：变量名从 topic 改为 subscribeTopic
                logger.info("已订阅主题: {}", subscribeTopic);
            } catch (MqttException e) {
                logger.error("订阅Topic失败，请检查Topic配置: topic={}", subscribeTopic, e);
                logger.warn("项目将继续启动，但MQTT消息监听暂不可用。");
            }

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