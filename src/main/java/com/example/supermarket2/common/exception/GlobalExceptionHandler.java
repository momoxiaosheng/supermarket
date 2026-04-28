package com.example.supermarket2.common.exception;

import com.example.supermarket2.common.result.ErrorCode;
import com.example.supermarket2.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一捕获并处理项目中所有异常，返回标准化的Result响应体
 * 企业级规范：全异常覆盖、日志分级、链路追踪、安全脱敏、环境适配
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // 最高优先级，确保优先拦截异常
public class GlobalExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";

    /**
     * 读取当前环境配置，区分生产/开发环境
     */
    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    // ====================== 核心业务异常处理（已知可预期异常） ======================
    /**
     * 捕获自定义业务异常（核心业务异常处理）
     */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        String traceId = MDC.get(TRACE_ID_KEY);
        // 业务异常：warn级别，只打印业务信息，不打印堆栈（已知异常，无需堆栈定位）
        log.warn("业务异常 [traceId:{}] [{} {}] - 错误码:{}, 错误信息:{}",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                e.getCode(),
                e.getMessage());
        return Result.error(e.getCode(), e.getMessage()).setTraceId(traceId);
    }

    // ====================== 请求参数校验异常处理 ======================
    /**
     * 请求体JSON参数校验失败 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String traceId = MDC.get(TRACE_ID_KEY);
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        // 参数异常：warn级别，打印校验信息，无需完整堆栈
        log.warn("参数校验失败 [traceId:{}]: {}", traceId, errorMsg);
        return Result.error(ErrorCode.PARAM_ERROR, errorMsg).setTraceId(traceId);
    }

    /**
     * GET表单参数绑定失败 / @RequestParam参数缺失
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleParamBindException(Exception e) {
        String traceId = MDC.get(TRACE_ID_KEY);
        String errorMsg;
        if (e instanceof MissingServletRequestParameterException ex) {
            errorMsg = "参数[" + ex.getParameterName() + "]不能为空";
        } else if (e instanceof ConstraintViolationException ex) {
            errorMsg = ex.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
        } else {
            errorMsg = "请求参数绑定失败";
        }
        log.warn("请求参数绑定失败 [traceId:{}]: {}", traceId, errorMsg);
        return Result.error(ErrorCode.PARAM_ERROR, errorMsg).setTraceId(traceId);
    }

    // ====================== HTTP请求系统异常处理 ======================
    /**
     * 404 接口不存在
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<?> handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.warn("请求接口不存在 [traceId:{}] [{} {}]", traceId, request.getMethod(), request.getRequestURI());
        return Result.error(ErrorCode.RESOURCE_NOT_FOUND).setTraceId(traceId);
    }

    /**
     * 请求方法不支持 405
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<?> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.warn("请求方法不支持 [traceId:{}] [{} {}], 支持的方法:{}",
                traceId, request.getMethod(), request.getRequestURI(), e.getSupportedMethods());
        return Result.error(ErrorCode.METHOD_NOT_ALLOWED).setTraceId(traceId);
    }

    /**
     * 请求媒体类型不支持
     */
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class})
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<?> handleMediaTypeException(Exception e) {
        String traceId = MDC.get(TRACE_ID_KEY);
        log.warn("请求媒体类型不支持 [traceId:{}]: {}", traceId, e.getMessage());
        return Result.error(ErrorCode.MEDIA_TYPE_NOT_SUPPORTED).setTraceId(traceId);
    }

    // ====================== 全局兜底异常处理（未知系统异常，禁止删除） ======================
    /**
     * 全局兜底异常处理，捕获所有未被上面拦截的异常
     * 彻底避免异常逃逸导致返回非统一格式、泄露堆栈信息
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> handleGlobalException(Exception e, HttpServletRequest request) {
        String traceId = MDC.get(TRACE_ID_KEY);
        // 系统未知异常：error级别，必须打印完整堆栈+全上下文，用于线上问题定位
        log.error("系统未知异常 [traceId:{}] [{} {}] [客户端IP:{}] [请求参数:{}]",
                traceId,
                request.getMethod(),
                request.getRequestURI(),
                getClientIp(request),
                request.getParameterMap(),
                e); // 必须把异常对象传入log方法，才能打印完整堆栈

        // 环境适配：生产环境只返回通用提示，避免泄露敏感信息；开发环境返回异常信息便于调试
        String errorMsg = "prod".equals(activeProfile)
                ? ErrorCode.SYSTEM_ERROR.getMessage()
                : e.getMessage();

        return Result.error(ErrorCode.SYSTEM_ERROR, errorMsg).setTraceId(traceId);
    }

    // ====================== 工具方法 ======================
    /**
     * 获取客户端真实IP，完善日志上下文
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}