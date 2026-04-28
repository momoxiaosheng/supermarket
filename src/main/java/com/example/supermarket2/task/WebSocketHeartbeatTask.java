package com.example.supermarket2.task;

import com.example.supermarket2.manager.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * WebSocket 心跳定时任务
 * 定期清理超时死连接，保证会话管理器的内存不泄漏
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHeartbeatTask {

    private final WebSocketSessionManager sessionManager;

    /**
     * 每30秒执行一次，清理超时死连接
     */
    @Scheduled(fixedRate = 30 * 1000)
    public void cleanTimeoutSession() {
        log.debug("开始执行WebSocket死连接清理任务");
        sessionManager.cleanTimeoutSessions();
    }
}