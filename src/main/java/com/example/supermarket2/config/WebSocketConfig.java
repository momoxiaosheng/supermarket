package com.example.supermarket2.config;

import com.example.supermarket2.handler.CartWebSocketHandler;
import com.example.supermarket2.handler.UniversalWebSocketHandler;
import com.example.supermarket2.interceptor.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 全局配置
 * 兼容原有购物车单业务Handler，新增通用全业务Handler
 */
@Configuration
@EnableWebSocket // 开启WebSocket支持
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final CartWebSocketHandler cartWebSocketHandler;
    private final UniversalWebSocketHandler universalWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 1. 原有购物车专属WebSocket端点（兼容老代码）
        registry.addHandler(cartWebSocketHandler, "/ws/cart")
                .addInterceptors(handshakeInterceptor) // 握手拦截器（身份校验）
                .setAllowedOrigins("*"); // 生产环境请配置具体域名，禁止用*

        // 2. 通用全业务WebSocket端点（定位/导航/设备/购物车统一接入）
        registry.addHandler(universalWebSocketHandler, "/ws/universal")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}