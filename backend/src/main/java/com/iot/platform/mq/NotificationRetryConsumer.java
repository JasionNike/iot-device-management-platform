package com.iot.platform.mq;

import com.iot.platform.dto.NotificationMessage;
import com.iot.platform.feign.NotificationClient;
import com.iot.platform.feign.NotificationClientFallback;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知重试消费者 — 商业级MQ延迟重试
 *
 * 从 iot.notification.retry.queue 消费延迟消息，重新调用通知服务。
 *
 * 重试流程：
 * 1. TTL过期 → DLX回流 → 消息到达 retry.queue
 * 2. Consumer 消费 → 调用 NotificationClient.send()
 * 3. 成功 → ACK + UPDATE t_notification_retry SET status='SENT'
 * 4. 失败 → retryCount++ → 检查是否超过上限
 *    - 未超限 → 重新发到 wait.queue，TTL加大（指数退避）
 *    - 已超限 → 发到 DLQ + UPDATE t_notification_retry SET status='FAILED'
 *
 * @author 王恒
 */
@Component
public class NotificationRetryConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryConsumer.class);

    @Autowired(required = false)
    private NotificationClient notificationClient;

    @Resource
    private NotificationClientFallback fallback;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /** 消费重试队列消息 */
    @RabbitListener(queues = "iot.notification.retry.queue")
    public void handleRetry(NotificationMessage msg, Channel channel, Message message) {
        log.info("通知重试消费: {}", msg);

        try {
            // 1. 尝试重新发送通知
            Map<String, Object> request = new HashMap<>();
            request.put("alertId", msg.getAlertId());
            request.put("deviceId", msg.getDeviceId());
            request.put("channel", msg.getPrimaryChannel());
            request.put("content", msg.getContent());
            request.put("recipients", msg.getRecipients());

            notificationClient.send(request);

            // 2. 成功 → ACK + 更新DB状态
            updateRetryStatus(msg.getRetryId(), "SENT", msg.getRetryCount() + 1, "重试成功");
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("通知重试成功: retryId={}, 第{}次重试", msg.getRetryId(), msg.getRetryCount() + 1);

        } catch (Exception e) {
            int newRetryCount = msg.getRetryCount() + 1;
            log.warn("通知重试失败: retryId={}, retryCount={}/{}, error={}",
                msg.getRetryId(), newRetryCount, msg.getMaxRetry(), e.getMessage());

            try {
                if (newRetryCount >= msg.getMaxRetry()) {
                    // 超过最大重试次数 → NACK不重新入队（进入DLQ）
                    updateRetryStatus(msg.getRetryId(), "FAILED", newRetryCount,
                        "[重试耗尽]" + truncate(e.getMessage(), 400));
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
                    log.error("通知已达最大重试次数，进入DLQ: retryId={}", msg.getRetryId());

                } else {
                    // 未超限 → ACK当前消息 + 重新发送到wait队列（TTL加大）
                    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                    msg.setRetryCount(newRetryCount);

                    // 尝试备用通道降级
                    if (newRetryCount >= 3 && msg.getFallbackChannel() != null) {
                        log.info("主通道失败{}次，降级到备用通道: {}→{}",
                            newRetryCount, msg.getPrimaryChannel(), msg.getFallbackChannel());
                        String temp = msg.getPrimaryChannel();
                        msg.setPrimaryChannel(msg.getFallbackChannel());
                        msg.setFallbackChannel(temp);
                    }

                    fallback.sendWithDelay(msg);
                    updateRetryStatus(msg.getRetryId(), "PENDING", newRetryCount,
                        "[重试中]" + truncate(e.getMessage(), 400));
                }
            } catch (IOException ioEx) {
                log.error("ACK/NACK操作失败", ioEx);
            }
        }
    }

    private void updateRetryStatus(Long retryId, String status, int retryCount, String errorMsg) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        jdbcTemplate.update(
            "UPDATE t_notification_retry SET status=?, retry_count=?, error_msg=?, update_time=? WHERE id=?",
            status, retryCount, truncate(errorMsg, 500), now, retryId);
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) return "";
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
}
