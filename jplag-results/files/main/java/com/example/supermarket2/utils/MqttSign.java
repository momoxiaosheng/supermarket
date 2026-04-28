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
     * 生成阿里云IoT MQTT连接参数（用户名、密码、ClientId）
     * 规则：https://help.aliyun.com/document_detail/73742.html
     */
    public void calculate(String productKey, String deviceName, String deviceSecret) {
        if (productKey == null || deviceName == null || deviceSecret == null) {
            return;
        }

        try {
            // 用户名：deviceName&productKey
            username = deviceName + "&" + productKey;

            // 生成时间戳（用于签名和ClientId）
            String timestamp = String.valueOf(System.currentTimeMillis());

            // ClientId格式：productKey.deviceName|securemode=2,signmethod=hmacsha256,timestamp=xxx|
            clientId = productKey + "." + deviceName + "|" +
                    "securemode=3" + "," +
                    "signmethod=hmacsha256" + "," +
                    "timestamp=" + timestamp + "|";

            // 密码明文：clientId${clientId}deviceName${deviceName}productKey${productKey}timestamp${timestamp}
            String plainPasswd = "clientId" + clientId + "deviceName" + deviceName + "productKey" + productKey + "timestamp" + timestamp;

            // HmacSHA256加密明文，密钥为deviceSecret
            password = CryptoUtil.hmacSha256(plainPasswd, deviceSecret);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}