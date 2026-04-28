package com.example.supermarket2.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket 全局会话管理器
 * 核心能力：用户会话的增删改查、多端登录管理、连接状态检测、批量消息推送
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    /**
     * 最后消息时间的Session属性Key
     */
    public static final String LAST_MESSAGE_TIME_KEY = "lastMessageTime";

    /**
     * 用户会话存储
     * key: userId, value: 用户的会话集合（支持多端同时登录，多设备同时在线）
     */
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, WebSocketSession>> userSessionMap = new ConcurrentHashMap<>();

    /**
     * 心跳超时时间（30秒无心跳则判定为死连接）
     */
    private static final long HEARTBEAT_TIMEOUT = 30 * 1000L;

    /**
     * 新增用户会话
     * @param userId 用户ID
     * @param session WebSocket会话
     */
    public void addSession(Long userId, WebSocketSession session) {
        // 新增会话时初始化最后消息时间
        session.getAttributes().put(LAST_MESSAGE_TIME_KEY, System.currentTimeMillis());
        // 为用户创建会话集合（如果不存在）
        userSessionMap.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        // 将会话加入用户的集合，key为会话ID
        userSessionMap.get(userId).put(session.getId(), session);
        log.info("用户{}会话新增，会话ID={}，当前在线会话数={}",
                userId, session.getId(), userSessionMap.get(userId).size());
    }

    /**
     * 移除用户会话
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    public void removeSession(Long userId, String sessionId) {
        ConcurrentHashMap<String, WebSocketSession> sessionMap = userSessionMap.get(userId);
        if (sessionMap != null) {
            WebSocketSession session = sessionMap.remove(sessionId);
            // try-with-resources 自动关闭会话
            if (session != null) {
                try (session) {
                    if (session.isOpen()) {
                        log.info("用户{}会话{}已关闭", userId, sessionId);
                    }
                } catch (IOException e) {
                    log.error("关闭用户{}会话{}异常", userId, sessionId, e);
                }
            }
            // 如果用户没有会话了，移除用户key
            if (sessionMap.isEmpty()) {
                userSessionMap.remove(userId);
                log.info("用户{}所有会话已移除，下线", userId);
            } else {
                log.info("用户{}会话{}已移除，剩余会话数={}", userId, sessionId, sessionMap.size());
            }
        }
    }

    /**
     * 获取用户的所有在线会话
     * @param userId 用户ID
     * @return 会话集合
     */
    public Set<WebSocketSession> getUserSessions(Long userId) {
        ConcurrentHashMap<String, WebSocketSession> sessionMap = userSessionMap.get(userId);
        if (sessionMap == null || sessionMap.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(sessionMap.values());
    }

    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(Long userId) {
        ConcurrentHashMap<String, WebSocketSession> sessionMap = userSessionMap.get(userId);
        if (sessionMap == null || sessionMap.isEmpty()) {
            return false;
        }
        // 优化：只要有一个会话是打开的，就判定为在线
        return sessionMap.values().stream().anyMatch(WebSocketSession::isOpen);
    }

    /**
     * 获取所有在线用户ID
     * @return 在线用户ID集合
     */
    public Set<Long> getOnlineUserIds() {
        return Set.copyOf(userSessionMap.keySet());
    }

    /**
     * 获取在线用户总数
     * @return 在线用户数
     */
    public int getOnlineUserCount() {
        return userSessionMap.size();
    }

    /**
     * 获取在线会话总数
     * @return 会话数
     */
    public int getOnlineSessionCount() {
        return userSessionMap.values().stream()
                .mapToInt(ConcurrentHashMap::size)
                .sum();
    }

    /**
     * 关闭指定用户的所有会话
     * @param userId 用户ID
     */
    public void closeUserSessions(Long userId) {
        ConcurrentHashMap<String, WebSocketSession> sessionMap = userSessionMap.get(userId);
        if (sessionMap != null) {
            sessionMap.values().forEach(session -> {
                // try-with-resources 自动关闭
                try (session) {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (IOException e) {
                    log.error("关闭用户{}会话异常", userId, e);
                }
            });
            userSessionMap.remove(userId);
            log.info("用户{}所有会话已强制关闭", userId);
        }
    }

    /**
     * 清理超时死连接
     */
    public void cleanTimeoutSessions() {
        long currentTime = System.currentTimeMillis();
        int cleanCount = 0;

        for (Long userId : userSessionMap.keySet()) {
            ConcurrentHashMap<String, WebSocketSession> sessionMap = userSessionMap.get(userId);
            if (sessionMap == null) continue;

            // 过滤出超时/关闭的会话
            Set<String> timeoutSessionIds = sessionMap.entrySet().stream()
                    .filter(entry -> {
                        WebSocketSession session = entry.getValue();
                        // 从Session属性中获取最后消息时间，修复getLastMessageTime不存在的报错
                        Long lastMessageTime = (Long) session.getAttributes().get(LAST_MESSAGE_TIME_KEY);
                        // 会话已关闭 或 最后消息时间超过超时时间
                        return !session.isOpen() ||
                                (lastMessageTime != null && (currentTime - lastMessageTime) > HEARTBEAT_TIMEOUT);
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // 移除超时会话
            for (String sessionId : timeoutSessionIds) {
                WebSocketSession session = sessionMap.remove(sessionId);
                // try-with-resources 自动关闭
                if (session != null) {
                    try (session) {
                        if (session.isOpen()) {
                            session.close();
                        }
                    } catch (IOException e) {
                        log.error("关闭超时会话异常，sessionId={}", sessionId, e);
                    }
                }
                cleanCount++;
            }

            // 用户无会话则移除key
            if (sessionMap.isEmpty()) {
                userSessionMap.remove(userId);
            }
        }

        if (cleanCount > 0) {
            log.info("清理超时会话完成，共清理{}个死连接", cleanCount);
        }
    }
}