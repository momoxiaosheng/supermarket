package com.example.supermarket2.service;

import com.example.supermarket2.dto.ws.WebSocketMessage;
import com.example.supermarket2.manager.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;

/**
 * WebSocket 统一推送服务
 * 所有业务模块需要推送消息时，仅需调用此类的方法，无需关心会话管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketPushService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    /**
     * 向指定用户推送消息（核心方法）
     * @param userId 目标用户ID
     * @param message 推送的消息体
     * @return 推送成功的会话数
     */
    public <T> int pushToUser(Long userId, WebSocketMessage<T> message) {
        if (userId == null || message == null) {
            log.warn("推送消息失败：参数为空");
            return 0;
        }

        // 检查用户是否在线
        if (!sessionManager.isUserOnline(userId)) {
            log.debug("用户{}不在线，跳过消息推送，消息类型={}", userId, message.getType());
            return 0;
        }

        // 获取用户所有在线会话
        Set<WebSocketSession> sessions = sessionManager.getUserSessions(userId);
        int successCount = 0;

        // 序列化消息
        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("消息序列化失败，userId={}", userId, e);
            return 0;
        }

        // 向每个会话推送消息
        TextMessage textMessage = new TextMessage(messageJson);
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) { // 线程安全：防止多线程同时向同一个session写数据
                        session.sendMessage(textMessage);
                    }
                    successCount++;
                    log.debug("消息推送成功，userId={}, sessionId={}, 消息类型={}",
                            userId, session.getId(), message.getType());
                }
            } catch (IOException e) {
                log.error("消息推送失败，userId={}, sessionId={}", userId, session.getId(), e);
            }
        }

        return successCount;
    }

    /**
     * 向所有在线用户广播消息
     * @param message 广播消息
     * @return 推送成功的会话数
     */
    public <T> int broadcastToAll(WebSocketMessage<T> message) {
        Set<Long> onlineUserIds = sessionManager.getOnlineUserIds();
        int totalSuccess = 0;
        for (Long userId : onlineUserIds) {
            totalSuccess += pushToUser(userId, message);
        }
        log.info("广播消息完成，在线用户数={}, 推送成功数={}", onlineUserIds.size(), totalSuccess);
        return totalSuccess;
    }

    /**
     * 向指定用户集合批量推送消息
     * @param userIds 用户ID集合
     * @param message 推送消息
     * @return 推送成功的会话数
     */
    public <T> int pushToUsers(Set<Long> userIds, WebSocketMessage<T> message) {
        int totalSuccess = 0;
        for (Long userId : userIds) {
            totalSuccess += pushToUser(userId, message);
        }
        return totalSuccess;
    }

    // ========== 业务场景快捷方法 ==========
    /**
     * 推送购物车更新消息
     */
    public int pushCartUpdate(Long userId, Object cartData) {
        WebSocketMessage<Object> message = WebSocketMessage.success("cart_update", cartData);
        return pushToUser(userId, message);
    }

    /**
     * 推送用户定位更新消息
     */
    public int pushLocationUpdate(Long userId, Object locationData) {
        WebSocketMessage<Object> message = WebSocketMessage.success("location_update", locationData);
        return pushToUser(userId, message);
    }

    /**
     * 推送导航路径更新消息
     */
    public int pushNavigationUpdate(Long userId, Object navigationData) {
        WebSocketMessage<Object> message = WebSocketMessage.success("navigation_update", navigationData);
        return pushToUser(userId, message);
    }

    /**
     * 推送智能设备数据消息
     */
    public int pushDeviceData(Long userId, Object deviceData) {
        WebSocketMessage<Object> message = WebSocketMessage.success("device_data", deviceData);
        return pushToUser(userId, message);
    }
}