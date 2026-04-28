package com.example.supermarket2.common.exception;

import com.example.supermarket2.common.result.ErrorCode;
import lombok.Getter;

/**
 * 统一业务异常
 * 所有业务逻辑异常均抛出此异常，禁止抛出RuntimeException/自定义其他异常
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;

    /**
     * 推荐使用：直接传入错误码枚举，全项目统一规范
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    /**
     * 次推荐：支持动态替换message（比如需要携带参数、拼接业务信息）
     */
    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }

    /**
     * 不推荐：兼容旧方式，仅特殊场景使用，禁止日常开发使用
     */
    @Deprecated
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 重写fillInStackTrace，业务异常无需打印堆栈，提升性能
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}