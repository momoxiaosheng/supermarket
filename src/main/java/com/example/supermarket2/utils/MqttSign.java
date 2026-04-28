package com.example.supermarket2.utils;

public class MqttSign {
    private String username;
    private String password;
    private String clientId;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getClientId() {
        return clientId;
    }

    /**
     * 生成华为云IoT MQTT连接参数（用户名、密码、ClientId）
     * 华为云标准接入规则
     */
    public void calculate(String deviceId, String deviceSecret) {
        if (deviceId == null || deviceSecret == null) {
            return;
        }

        try {
            // ============== 华为云 IoT 固定规则 ==============
            // 用户名 = 设备ID
            username = deviceId;

            // 客户端ID = 设备ID
            clientId = deviceId;

            // 密码 = HmacSHA256(设备ID, 设备密钥) → Base64
            password = CryptoUtil.hmacSha256(deviceId, deviceSecret);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}