package com.example.supermarket2.handler;

import com.example.supermarket2.dto.app.response.CartItemDto;
import com.example.supermarket2.dto.ws.WebSocketMessage;
import com.example.supermarket2.manager.WebSocketSessionManager;
import com.example.supermarket2.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;

/**
 * 购物车专属WebSocket处理器
 * 兼容原有业务逻辑，重构后增加：身份校验、心跳处理、会话统一管理
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CartWebSocketHandler extends TextWebSocketHandler {

    private final CartService cartService;
    private final ObjectMapper objectMapper;
    private final WebSocketSessionManager sessionManager;

    /**
     * 连接建立成功后执行
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从握手拦截器注入的属性中获取userId（安全，不可伪造）
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("用户身份校验失败"));
            return;
        }

        // 将会话加入全局管理器
        sessionManager.addSession(userId, session);

        // 连接建立时立即发送当前购物车数据
        sendCartDataToUser(userId, session);
    }

    /**
     * 连接关闭后执行
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            // 从全局管理器移除会话
            sessionManager.removeSession(userId, session.getId());
        }
        log.info("购物车WebSocket连接关闭，sessionId={}, 关闭原因={}", session.getId(), status);
    }

    /**
     * 收到客户端消息时执行
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 收到消息时，更新最后消息时间
        session.getAttributes().put(WebSocketSessionManager.LAST_MESSAGE_TIME_KEY, System.currentTimeMillis());

        Long userId = (Long) session.getAttributes().get("userId");
        String payload = message.getPayload();
        log.debug("收到客户端消息，userId={}, 内容={}", userId, payload);

        // 解析消息
        WebSocketMessage<?> wsMessage;
        try {
            wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
        } catch (Exception e) {
            sendMessage(session, WebSocketMessage.error("消息格式错误"));
            return;
        }

        // 心跳包处理
        if ("heartbeat".equals(wsMessage.getType())) {
            sendMessage(session, WebSocketMessage.heartbeat());
            return;
        }

        // 客户端主动请求刷新购物车
        if ("cart_refresh".equals(wsMessage.getType())) {
            sendCartDataToUser(userId, session);
            return;
        }

        // 其他消息类型可扩展
        sendMessage(session, WebSocketMessage.error("不支持的消息类型"));
    }

    /**
     * 传输异常处理
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("购物车WebSocket传输异常，userId={}, sessionId={}", userId, session.getId(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason("传输异常"));
        }
    }

    /**
     * 【原有方法重构】向指定用户发送购物车更新（全量推送）
     */
    public void sendCartUpdateToUser(Long userId) {
        if (!sessionManager.isUserOnline(userId)) {
            return;
        }
        // 获取用户所有在线会话
        var sessions = sessionManager.getUserSessions(userId);
        for (WebSocketSession session : sessions) {
            sendCartDataToUser(userId, session);
        }
    }

    /**
     * 向指定会话发送购物车数据
     */
    private void sendCartDataToUser(Long userId, WebSocketSession session) {
        try {
            if (!session.isOpen()) {
                return;
            }
            List<CartItemDto> cartItems = cartService.getCartItems(userId);
            WebSocketMessage<List<CartItemDto>> wsMessage = WebSocketMessage.success("cart_update", cartItems);
            sendMessage(session, wsMessage);
            log.debug("购物车数据推送成功，userId={}", userId);
        } catch (Exception e) {
            log.error("购物车数据推送失败，userId={}", userId, e);
        }
    }

    /**
     * 发送消息的工具方法（线程安全）
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