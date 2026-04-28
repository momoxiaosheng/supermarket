package com.example.supermarket2.component;

import com.example.supermarket2.service.DeviceDataService;
import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.JmsQueue;
import org.apache.qpid.jms.transports.TransportOptions;
import org.apache.qpid.jms.transports.TransportSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.jms.*;
import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;

@Component
public class HuaweiAmqpListener implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(HuaweiAmqpListener.class);

    @Value("${huawei.iot.amqp.url}")
    private String amqpUrl;
    @Value("${huawei.iot.amqp.accessKey}")
    private String accessKey;
    @Value("${huawei.iot.amqp.accessCode}")
    private String accessCode;
    @Value("${huawei.iot.amqp.queueName}")
    private String queueName;

    @Autowired
    private DeviceDataService deviceDataService;

    @Override
    public void run(String... args) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {}

        try {
            log.info("=== 华为云IoT AMQP 开始连接 ===");
            log.info("AMQP URL: {}", amqpUrl);
            log.info("AccessKey: {}", accessKey);
            log.info("Queue: {}", queueName);

            String host = amqpUrl.replace("amqps://", "");
            String connectionUrl = "failover:(amqps://" + host
                    + "?amqp.vhost=default"
                    + "&amqp.idleTimeout=30000"
                    + "&amqp.saslMechanisms=PLAIN"
                    + ")"
                    + "?jms.clientID=" + java.util.UUID.randomUUID()
                    + "&failover.reconnectDelay=3000"
                    + "&failover.maxReconnectDelay=30000"
                    + "&failover.maxReconnectAttempts=-1";

            log.info("完整连接URL: {}", connectionUrl);

            JmsConnectionFactory factory = new JmsConnectionFactory(connectionUrl);

            // 配置 SSL，信任所有证书
            TransportOptions transportOptions = new TransportOptions();
            transportOptions.setTrustAll(true);
            SSLContext sslContext = TransportSupport.createJdkSslContext(transportOptions);
            factory.setSslContext(sslContext);

            // 设置 USERNAME_OVERRIDE 扩展
            factory.setExtension(JmsConnectionExtensions.USERNAME_OVERRIDE.toString(), (connection, uri) -> {
                // 直接用控制台显示的原始用户名格式（无 timestamp）
                String username = "accessKey=" + accessKey;
                log.info("[USERNAME_OVERRIDE] 认证用户名: {}", username);
                return username;
            });

            log.info("密码长度: {}", accessCode.length());

            // 传 accessKey 作为基础用户名，密码单独传
            // USERNAME_OVERRIDE 会把它替换成完整用户名
            Connection connection = factory.createConnection("accessKey=" + accessKey, accessCode);
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(new JmsQueue(queueName));

            consumer.setMessageListener(message -> {
                try {
                    BytesMessage msg = (BytesMessage) message;
                    byte[] bytes = new byte[(int) msg.getBodyLength()];
                    msg.readBytes(bytes);
                    String payload = new String(bytes, StandardCharsets.UTF_8);
                    log.info("AMQP收到设备数据: {}", payload);
                    deviceDataService.processDeviceData(payload);
                } catch (Exception e) {
                    log.error("AMQP消息处理失败", e);
                }
            });

            log.info("AMQP 连接成功！已开始监听设备数据");

        } catch (Exception e) {
            log.error("AMQP 连接失败", e);
            Throwable cause = e.getCause();
            int depth = 0;
            while (cause != null && depth < 10) {
                log.error("  异常链[{}]: {} - {}", depth, cause.getClass().getName(), cause.getMessage());
                cause = cause.getCause();
                depth++;
            }
        }
    }
}
