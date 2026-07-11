package com.iot.platform.common;

/**
 * 统一错误码枚举 — 标准化业务异常标识
 * <p>
 * 编码规则：模块前缀(2位) + 错误类型(1位) + 序号(2位)
 * 模块前缀：DE=设备, AL=告警, OT=OTA, PR=产品, AU=认证, SY=系统
 * 错误类型：4=客户端错误(参数/权限), 5=服务端错误(DB/MQ/第三方)
 * <p>
 * 前端根据错误码做差异化处理（弹窗/重定向/静默忽略）
 *
 * @author 王恒
 */
public enum ErrorCode {

    // ========== 认证 (AU) ==========
    AUTH_TOKEN_EXPIRED(40101, "Token已过期，请重新登录"),
    AUTH_TOKEN_INVALID(40102, "Token无效"),
    AUTH_LOGIN_FAILED(40103, "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED(40104, "账户已被禁用"),
    AUTH_IP_LOCKED(40105, "登录过于频繁，IP已临时锁定"),
    AUTH_PERMISSION_DENIED(40301, "权限不足"),

    // ========== 设备 (DE) ==========
    DEVICE_NOT_FOUND(40401, "设备不存在"),
    DEVICE_SN_DUPLICATE(40901, "设备SN已存在"),
    DEVICE_REGISTER_FAILED(50001, "设备注册失败"),
    DEVICE_SN_EMPTY(40001, "设备SN不能为空"),
    DEVICE_PRODUCT_KEY_INVALID(40002, "产品标识无效"),

    // ========== 告警 (AL) ==========
    ALERT_NOT_FOUND(40402, "告警记录不存在"),
    ALERT_RULE_INVALID(40003, "告警规则配置无效"),
    ALERT_HANDLE_STATUS_INVALID(40004, "告警处理状态无效"),

    // ========== OTA (OT) ==========
    OTA_FIRMWARE_NOT_FOUND(40403, "固件不存在"),
    OTA_TASK_NOT_FOUND(40404, "OTA任务不存在"),
    OTA_FIRMWARE_MD5_REQUIRED(40005, "固件MD5校验值不能为空"),

    // ========== 产品 (PR) ==========
    PRODUCT_NOT_FOUND(40405, "产品不存在"),
    PRODUCT_KEY_DUPLICATE(40902, "产品标识已存在"),
    PRODUCT_PROPERTY_INVALID(40006, "产品属性定义无效"),

    // ========== 系统 (SY) ==========
    SYSTEM_ERROR(50000, "系统繁忙，请稍后重试"),
    DB_ERROR(50002, "数据库异常"),
    REDIS_ERROR(50003, "缓存服务异常"),
    MQ_ERROR(50004, "消息队列异常"),
    VALIDATION_ERROR(40000, "参数校验失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
