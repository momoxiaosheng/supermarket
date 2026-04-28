package com.example.supermarket2.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 链路追踪拦截器
 * 为每个请求生成唯一traceId，贯穿全请求链路
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 生成traceId，优先取请求头中的traceId，否则生成新的
        String traceId = request.getHeader(TRACE_ID_KEY);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID_KEY, traceId);
        // 响应头返回traceId，方便前端排查问题
        response.setHeader(TRACE_ID_KEY, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除MDC，防止线程复用导致traceId错乱
        MDC.remove(TRACE_ID_KEY);
    }
}