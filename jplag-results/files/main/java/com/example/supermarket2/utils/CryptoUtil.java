package com.example.supermarket2.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;

public class CryptoUtil {
    /**
     * HmacSHA256加密，返回十六进制字符串
     */
    public static String hmacSha256(String plainText, String key) throws Exception {
        if (plainText == null || key == null) {
            return null;
        }

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacResult = mac.doFinal(plainText.getBytes());

        // 字节数组转64位十六进制字符串
        return String.format("%064x", new BigInteger(1, hmacResult));
    }
}