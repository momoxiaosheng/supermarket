package com.example.supermarket2.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 超市购物物联网系统 业务错误码枚举
 * 错误码分段规则：
 * 200          通用成功
 * 10000-19999  通用系统错误
 * 20000-29999  用户&购物车模块错误
 * 30000-39999  商品&分类&搜索&轮播图模块错误
 * 40000-49999  地图&定位&导航模块错误
 * 50000-59999  IoT设备&MQTT消息模块错误
 * 60000-69999  微信相关模块错误
 * 70000-79999  权限&安全相关模块错误
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {
    // ===================== 通用成功 =====================
    SUCCESS(200, "操作成功"),
    // ===================== 通用系统错误 10000-19999 =====================
    SYSTEM_ERROR(10001, "系统内部异常，请稍后重试"),
    PARAM_ERROR(10002, "参数校验失败"),
    RESOURCE_NOT_FOUND(10003, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(10004, "不支持的请求方法"),
    MEDIA_TYPE_NOT_SUPPORTED(10005, "不支持的请求媒体类型"), // 新增，修复报错
    REQUEST_FREQUENT(10006, "请求过于频繁，请稍后重试"),
    DATA_PARSE_ERROR(10007, "数据解析失败"),
    FILE_UPLOAD_ERROR(10008, "文件上传失败"),
    FILE_TYPE_NOT_SUPPORT(10009, "不支持的文件类型"),
    TYPE_CONVERT_ERROR(10010, "参数类型转换失败"),
    SQL_EXECUTE_ERROR(10011, "数据库执行异常"),
    MISSING_REQUEST_PARAM(10012, "请求缺少必填参数"),
    MISSING_PATH_VARIABLE(10013, "请求路径缺少必填变量"),
    // ===================== 权限&安全相关 70000-79999 =====================
    UNAUTHORIZED(70001, "未登录，请先进行认证"), // 新增，修复报错
    NO_PERMISSION(70002, "无操作权限，访问被拒绝"),
    FORBIDDEN(70003, "访问被拒绝"), // 新增，修复报错
    TOKEN_INVALID(70004, "Token无效或已过期"),
    TOKEN_MISSING(70005, "请求缺少Token"),
    ACCESS_DENIED(70006, "访问被拒绝"),
    // ===================== 用户&购物车模块 20000-29999 =====================
    USER_NOT_FOUND(20001, "用户不存在"),
    CART_IS_EMPTY(20002, "购物车为空"),
    CART_ITEM_NOT_FOUND(20003, "购物车商品不存在"),
    CART_ADD_FAILED(20004, "商品添加购物车失败"),
    CART_UPDATE_FAILED(20005, "购物车更新失败"),
    CART_CLEAR_FAILED(20006, "购物车清空失败"),
    WEIGHT_DATA_INVALID(20007, "称重商品重量数据无效"),
    // ===================== 商品&分类&搜索模块 30000-39999 =====================
    PRODUCT_NOT_FOUND(30001, "商品不存在"),
    PRODUCT_OFF_SHELF(30002, "商品已下架"),
    PRODUCT_NOT_AVAILABLE(30003, "无效商品，商品不存在或已下架"),
    PRODUCT_STOCK_NOT_ENOUGH(30004, "商品库存不足"),
    CATEGORY_NOT_FOUND(30005, "商品分类不存在"),
    BANNER_NOT_FOUND(30006, "轮播图不存在"),
    SEARCH_KEYWORD_EMPTY(30007, "搜索关键词不能为空"),
    SEARCH_HISTORY_EMPTY(30008, "暂无搜索历史"),
    // ===================== 地图&定位&导航模块 40000-49999 =====================
    MAP_NOT_FOUND(40001, "地图数据不存在"),
    MAP_NOT_PUBLISHED(40002, "地图未发布"),
    MAP_DATA_EMPTY(40003, "地图网格数据为空"),
    BEACON_DATA_INVALID(40004, "蓝牙信标数据无效"),
    LOCATION_FAILED(40005, "定位计算失败"),
    NO_PASSABLE_PATH(40006, "未找到可通行路径"),
    POSITION_OUT_OF_BOUNDS(40007, "坐标超出地图边界"),
    USER_LOCATION_NOT_FOUND(40008, "未找到用户位置信息"),
    PATH_PLAN_FAILED(40009, "路径规划失败"),
    MAP_SAVE_FAILED(40010, "地图数据保存失败"),
    MAP_CREATE_FAILED(40011, "地图创建失败"),
    MAP_UPDATE_FAILED(40012, "地图更新失败"),
    MAP_DELETE_FAILED(40013, "地图删除失败"),
    MAP_PUBLISH_FAILED(40014, "地图发布失败"),
    MAP_UNPUBLISH_FAILED(40015, "地图取消发布失败"),
    // ===================== IoT设备&MQTT模块 50000-59999 =====================
    MQTT_CONNECT_FAILED(50001, "MQTT服务器连接失败"),
    MQTT_SUBSCRIBE_FAILED(50002, "MQTT主题订阅失败"),
    MQTT_MESSAGE_PARSE_ERROR(50003, "MQTT消息解析失败"),
    DEVICE_DATA_INVALID(50004, "设备上报数据格式无效"),
    DEVICE_IDENTIFIER_NOT_FOUND(50005, "设备标识提取失败"),
    DEVICE_DATA_PROCESS_FAILED(50006, "设备数据处理失败"),
    // ===================== 微信相关模块 60000-69999 =====================
    WECHAT_CONFIG_ERROR(60001, "微信配置信息异常"),
    WECHAT_AUTH_FAILED(60002, "微信授权失败"),
    WECHAT_API_CALL_FAILED(60003, "微信接口调用失败"),
    ;
    private final int code;
    private final String message;
}