package com.example.supermarket2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "wechat")
public class WeChatConfig {
    private String appId;
    private String appSecret;
}
