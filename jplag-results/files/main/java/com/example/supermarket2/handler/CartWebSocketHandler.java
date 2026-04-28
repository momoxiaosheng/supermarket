package com.example.supermarket2.handler;

import com.example.supermarket2.dto.app.CartItemDto;
import com.example.supermarket2.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CartWebSocketHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从连接参数中获取用户ID
        String query = session.getUri().getQuery();
        Long userId = extractUserIdFromQuery(query);

        if (userId != null) {
            userSessions.put(userId, session);
            System.out.println("用户 " + userId + " 的WebSocket连接已建立");

            // 连接建立时立即发送当前购物车数据
            sendCartDataToUser(userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 移除断开连接的会话
        userSessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
        System.out.println("WebSocket连接关闭: " + status.toString());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 处理客户端消息（如请求刷新等）
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket传输错误: " + exception.getMessage());
    }

    /**
     * 向指定用户发送购物车数据更新
     */
    public void sendCartUpdateToUser(Long userId) {
        try {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                sendCartDataToUser(userId);
            }
        } catch (Exception e) {
            System.err.println("向用户 " + userId + " 发送购物车更新失败: " + e.getMessage());
        }
    }

    /**
     * 发送购物车数据给指定用户
     */
    private void sendCartDataToUser(Long userId) throws IOException {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            List<CartItemDto> cartItems = cartService.getCartItems(userId);

            WebSocketMessage message = new WebSocketMessage();
            message.setType("cart_update");
            message.setData(cartItems);
            message.setTimestamp(System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));

            System.out.println("向用户 " + userId + " 发送购物车数据更新");
        }
    }

    /**
     * 从查询参数中提取用户ID
     */
    private Long extractUserIdFromQuery(String query) {
        if (query != null && query.contains("userId=")) {
            try {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("userId=")) {
                        return Long.parseLong(param.substring(7));
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("用户ID格式错误: " + query);
            }
        }
        return null;
    }

    /**
     * WebSocket消息封装类
     */
    public static class WebSocketMessage {
        private String type;
        private Object data;
        private Long timestamp;

        // Getter和Setter
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}