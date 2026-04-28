package com.example.supermarket2.utils;

import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 华为云IoT AMQP 认证签名工具类
 * 严格遵循华为云AMQP接入规范
 */
@Slf4j
public class HuaweiAmqpSignUtil {

    /**
     * 生成AMQP连接的用户名（华为云标准格式）
     * @param projectId 区域项目ID（如 cn-north-4）
     * @param instanceId 实例ID
     * @param accessKey 访问密钥AK
     * @return 合规的用户名：{project_id}|{instance_id}|{access_key}
     */
    public static String generateUserName(String projectId, String instanceId, String accessKey) {
        StringBuilder sb = new StringBuilder();
        if (projectId != null && !projectId.trim().isEmpty()) {
            sb.append(projectId.trim()).append("|");
        }
        if (instanceId != null && !instanceId.trim().isEmpty()) {
            sb.append(instanceId.trim()).append("|");
        }
        sb.append(accessKey);
        return sb.toString();
    }

    /**
     * 生成AMQP连接的密码（签名）
     * @param accessCode 华为云SK
     * @param queueName 队列名称
     * @return 合规的密码
     */
    public static String generatePassword(String accessCode, String queueName) {
        return generatePasswordWithTimestamp(accessCode, queueName, System.currentTimeMillis());
    }

    /**
     * 生成AMQP连接的密码（签名），支持外部指定时间戳
     * @param accessCode 华为云SK
     * @param queueName 队列名称
     * @param timestamp 指定的时间戳（必须和用户名中的timestamp一致）
     * @return 合规的密码
     */
    public static String generatePasswordWithTimestamp(String accessCode, String queueName, long timestamp) {
        try {
            // 资源名称固定格式：consumer|{队列名}
            String resourceName = "consumer|" + queueName;
            // 签名原文：{timestamp}\n{resourceName}
            String signContent = timestamp + "\n" + resourceName;

            // HmacSHA256签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(accessCode.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signResult = mac.doFinal(signContent.getBytes(StandardCharsets.UTF_8));

            // Base64编码
            String password = Base64.getEncoder().encodeToString(signResult);
            log.debug("AMQP签名生成成功，timestamp={}", timestamp);
            return password;
        } catch (Exception e) {
            log.error("AMQP签名生成失败", e);
            throw new RuntimeException("AMQP认证签名生成失败", e);
        }
    }
}