package com.example.supermarket2.dto.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * WebSocket 统一消息协议
 * 所有前后端交互的消息均使用此结构，保证协议统一
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息类型（核心字段，用于消息分发）
     * 可选值：
     * - heartbeat：心跳包
     * - cart_update：购物车数据更新
     * - location_update：用户定位更新
     * - navigation_update：导航路径更新
     * - device_data：智能设备数据上报
     * - system_notice：系统通知
     * - error：错误消息
     */
    private String type;

    /**
     * 消息体（业务数据）
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 消息唯一ID（用于消息去重、离线补发）
     */
    private String messageId;

    /**
     * 响应码（200成功，非200失败）
     */
    private Integer code;

    /**
     * 响应信息
     */
    private String message;

    // ========== 静态快捷构建方法 ==========
    /**
     * 构建成功消息
     */
    public static <T> WebSocketMessage<T> success(String type, T data) {
        WebSocketMessage<T> message = new WebSocketMessage<>();
        message.setType(type);
        message.setData(data);
        message.setTimestamp(System.currentTimeMillis());
        message.setCode(200);
        message.setMessage("success");
        message.setMessageId(generateMessageId());
        return message;
    }

    /**
     * 构建心跳消息
     */
    public static WebSocketMessage<Void> heartbeat() {
        WebSocketMessage<Void> message = new WebSocketMessage<>();
        message.setType("heartbeat");
        message.setTimestamp(System.currentTimeMillis());
        message.setCode(200);
        message.setMessage("pong");
        return message;
    }

    /**
     * 构建错误消息
     */
    public static WebSocketMessage<Void> error(String message) {
        WebSocketMessage<Void> wsMessage = new WebSocketMessage<>();
        wsMessage.setType("error");
        wsMessage.setTimestamp(System.currentTimeMillis());
        wsMessage.setCode(500);
        wsMessage.setMessage(message);
        wsMessage.setMessageId(generateMessageId());
        return wsMessage;
    }

    /**
     * 生成消息唯一ID
     */
    private static String generateMessageId() {
        return System.currentTimeMillis() + "_" + (int) (Math.random() * 10000);
    }
}