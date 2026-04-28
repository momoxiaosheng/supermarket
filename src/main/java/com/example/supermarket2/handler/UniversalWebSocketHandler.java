package com.example.supermarket2.handler;

import com.example.supermarket2.dto.ws.WebSocketMessage;
import com.example.supermarket2.manager.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * 通用全业务WebSocket处理器
 * 统一处理购物车/定位/导航/设备数据全业务场景的消息收发
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UniversalWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("用户身份校验失败"));
            return;
        }

        sessionManager.addSession(userId, session);
        log.info("通用WebSocket连接建立成功，userId={}, sessionId={}", userId, session.getId());

        // 连接成功响应
        sendMessage(session, WebSocketMessage.success("connect_success", "连接成功"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessionManager.removeSession(userId, session.getId());
        }
        log.info("通用WebSocket连接关闭，userId={}, sessionId={}, 原因={}", userId, session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String payload = message.getPayload();

        try {
            WebSocketMessage<?> wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

            // 心跳包处理
            if ("heartbeat".equals(wsMessage.getType())) {
                sendMessage(session, WebSocketMessage.heartbeat());
                return;
            }

            log.debug("收到客户端消息，userId={}, 消息类型={}", userId, wsMessage.getType());

            // 其他业务消息可通过事件机制分发，或直接在业务层通过PushService推送
            // 这里仅做消息接收，业务推送统一由WebSocketPushService处理

        } catch (Exception e) {
            log.error("消息解析失败，userId={}", userId, e);
            sendMessage(session, WebSocketMessage.error("消息格式解析失败"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("通用WebSocket传输异常，userId={}", userId, exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    /**
     * 线程安全的消息发送方法
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage<?> message) throws IOException {
        if (!session.isOpen()) {
            return;
        }
        String json = objectMapper.writeValueAsString(message);
        synchronized (session) {
            session.sendMessage(new TextMessage(json));
        }
    }
}
