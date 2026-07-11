package com.iot.platform.dto;

/**
 * 通知通道枚举 — 商业级多通道降级策略
 *
 * 发送顺序：主通道 → 备用通道 → 人工介入
 * 例如：SMS(主) → PHONE(备) → MANUAL(人工)
 *
 * @author 王恒
 */
public enum NotificationChannel {

    /** 短信（阿里云短信服务） */
    SMS("短信", true),

    /** 邮件（腾讯云邮件推送） */
    EMAIL("邮件", true),

    /** App推送（极光推送） */
    APP_PUSH("App推送", true),

    /** 电话语音（主通道失败后的降级通道） */
    PHONE("电话语音", false),

    /** 钉钉群机器人 */
    DINGTALK("钉钉", true),

    /** 企业微信群机器人 */
    WECOM("企业微信", true),

    /** 人工介入（所有自动通道失败后的兜底） */
    MANUAL("人工处理", false);

    private final String displayName;
    /** 是否为自动通道（false表示需要人工处理） */
    private final boolean automatic;

    NotificationChannel(String displayName, boolean automatic) {
        this.displayName = displayName;
        this.automatic = automatic;
    }

    public String getDisplayName() { return displayName; }
    public boolean isAutomatic() { return automatic; }

    /**
     * 获取降级通道
     * SMS → PHONE → MANUAL
     */
    public static NotificationChannel fallbackOf(NotificationChannel primary) {
        switch (primary) {
            case SMS:    return PHONE;
            case EMAIL:  return SMS;
            case APP_PUSH: return SMS;
            case DINGTALK: return WECOM;
            case WECOM:  return DINGTALK;
            default:     return MANUAL;
        }
    }
}
