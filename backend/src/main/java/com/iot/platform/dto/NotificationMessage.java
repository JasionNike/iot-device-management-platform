package com.iot.platform.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通知重试消息 DTO
 * 在 MQ 延迟队列中传递，包含重试所需的完整上下文
 *
 * @author 王恒
 */
public class NotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据库补发表记录ID（关联 t_notification_retry.id） */
    private Long retryId;

    /** 告警ID */
    private Long alertId;

    /** 设备ID */
    private String deviceId;

    /** 主通知通道 */
    private String primaryChannel;

    /** 备用通知通道 */
    private String fallbackChannel;

    /** 通知内容 */
    private String content;

    /** 接收人 */
    private String recipients;

    /** 当前重试次数（0-based） */
    private int retryCount;

    /** 最大重试次数 */
    private int maxRetry;

    /** 创建时间 */
    private String createTime;

    public NotificationMessage() {}

    public static NotificationMessage create(Long retryId, Long alertId, String deviceId,
                                              String primaryChannel, String fallbackChannel,
                                              String content, String recipients,
                                              int retryCount, int maxRetry) {
        NotificationMessage msg = new NotificationMessage();
        msg.retryId = retryId;
        msg.alertId = alertId;
        msg.deviceId = deviceId;
        msg.primaryChannel = primaryChannel;
        msg.fallbackChannel = fallbackChannel;
        msg.content = content;
        msg.recipients = recipients;
        msg.retryCount = retryCount;
        msg.maxRetry = maxRetry;
        msg.createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return msg;
    }

    // ========== getters/setters ==========

    public Long getRetryId() { return retryId; }
    public void setRetryId(Long retryId) { this.retryId = retryId; }
    public Long getAlertId() { return alertId; }
    public void setAlertId(Long alertId) { this.alertId = alertId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getPrimaryChannel() { return primaryChannel; }
    public void setPrimaryChannel(String primaryChannel) { this.primaryChannel = primaryChannel; }
    public String getFallbackChannel() { return fallbackChannel; }
    public void setFallbackChannel(String fallbackChannel) { this.fallbackChannel = fallbackChannel; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetry() { return maxRetry; }
    public void setMaxRetry(int maxRetry) { this.maxRetry = maxRetry; }
    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    /**
     * 计算指数退避延迟时间（毫秒）
     * 1分钟 → 2分钟 → 4分钟 → 8分钟 → 16分钟 → 30分钟(封顶)
     */
    public long getBackoffDelayMs() {
        int delaySeconds = (int) Math.min(60 * Math.pow(2, retryCount), 1800);
        return delaySeconds * 1000L;
    }

    @Override
    public String toString() {
        return String.format("NotificationMessage{retryId=%d, alertId=%d, deviceId=%s, channel=%s, retryCount=%d/%d, nextDelay=%ds}",
                retryId, alertId, deviceId, primaryChannel, retryCount, maxRetry, getBackoffDelayMs() / 1000);
    }
}
