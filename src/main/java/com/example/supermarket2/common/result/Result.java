package com.example.supermarket2.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局统一响应体
 * 所有Controller接口统一返回该格式，规范前后端交互
 * 企业级标准：强约束实例化入口、全场景静态工厂、序列化兼容、链式调用
 */
@Data
@Accessors(chain = true) // 开启链式调用，所有set方法自动返回this，无需手动写setTraceId
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 私有化无参构造，禁止手动new
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 私有化全参构造
@JsonInclude(JsonInclude.Include.NON_NULL) // 序列化时忽略null值字段，减少传输体积
public class Result<T> implements Serializable {

    // 修复序列化警告，JDK14+专用注解，低版本JDK删掉这行注解即可
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     */
    private int code;
    /**
     * 响应提示信息
     */
    private String message;
    /**
     * 响应数据
     */
    private T data;
    /**
     * 链路追踪ID
     */
    private String traceId;

    // ====================== 成功响应静态工厂方法（全场景覆盖） ======================
    /**
     * 无数据成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null, null);
    }

    /**
     * 带数据成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, null);
    }

    /**
     * 带数据+自定义提示成功响应
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data, null);
    }

    // ====================== 失败响应静态工厂方法（强绑定错误码枚举） ======================
    /**
     * 自定义错误码+提示失败响应（兼容原有写法）
     */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, null);
    }

    /**
     * 错误码枚举失败响应（推荐使用，禁止硬编码）
     */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null, null);
    }

    /**
     * 错误码枚举+自定义提示失败响应
     */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null, null);
    }
}