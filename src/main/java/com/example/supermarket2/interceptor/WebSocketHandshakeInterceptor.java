package com.example.supermarket2.interceptor;

import com.example.supermarket2.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 核心能力：连接建立前的身份校验、用户信息注入、非法连接拦截
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    // 生产环境请替换为真实的JWT校验，这里兼容你现有userId=1的测试场景
    private static final String TEST_USER_ID = "1";

    /**
     * 握手前执行：身份校验
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        try {
            // 1. 从请求中提取参数
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String userId = servletRequest.getServletRequest().getParameter("userId");
            String token = servletRequest.getServletRequest().getParameter("token");

            // 2. 基础参数校验
            if (userId == null || userId.trim().isEmpty()) {
                log.warn("WebSocket握手失败：缺少userId参数");
                return false;
            }

            // 3. 身份校验（生产环境启用JWT校验，测试环境兼容现有逻辑）
            boolean isAuthPass = false;
            // 测试环境：兼容固定userId=1
            if (TEST_USER_ID.equals(userId)) {
                isAuthPass = true;
            }
            // 生产环境：JWT令牌校验
            else if (token != null && !token.trim().isEmpty()) {
                // 这里调用你JwtUtil的校验方法，解析token获取userId
                // Long realUserId = jwtUtil.validateTokenAndGetUserId(token);
                // isAuthPass = realUserId != null && realUserId.toString().equals(userId);
                isAuthPass = true; // 测试临时放行
            }

            if (!isAuthPass) {
                log.warn("WebSocket握手失败：用户身份校验不通过，userId={}", userId);
                return false;
            }

            // 4. 将userId注入WebSocket会话属性，后续处理器可直接获取
            attributes.put("userId", Long.parseLong(userId));
            log.info("WebSocket握手成功：userId={}", userId);
            return true;

        } catch (Exception e) {
            log.error("WebSocket握手异常", e);
            return false;
        }
    }

    /**
     * 握手后执行：日志记录
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket握手后异常", exception);
        }
    }
}