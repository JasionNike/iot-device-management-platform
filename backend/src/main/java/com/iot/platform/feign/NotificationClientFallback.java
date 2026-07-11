package com.iot.platform.feign;

import com.iot.platform.common.Result;
import com.iot.platform.dto.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 通知服务 Sentinel 熔断降级处理 — 商业级实现
 *
 * 降级策略：
 * 1. DB审计：INSERT t_notification_retry（永久记录，可追溯）
 * 2. MQ延迟重试：发送到 iot.notification.wait.queue
 *    利用 RabbitMQ DLX + Per-Message TTL 实现指数退避延迟重试
 * 3. 不阻塞告警主流程
 *
 * @author 王恒
 */
@Component
public class NotificationClientFallback implements FallbackFactory<NotificationClient> {

    private static final Logger log = LoggerFactory.getLogger(NotificationClientFallback.class);

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private RabbitTemplate rabbitTemplate;

    /** 最大重试次数 */
    private static final int MAX_RETRY = 5;

    @Override
    public NotificationClient create(Throwable cause) {
        log.error("通知服务熔断降级触发, cause={}", cause.getMessage());

        return request -> {
            try {
                Long alertId = request.get("alertId") != null
                    ? Long.valueOf(request.get("alertId").toString()) : null;
                String deviceId = (String) request.getOrDefault("deviceId", "");
                String channel = (String) request.getOrDefault("channel", "SMS");
                String content = (String) request.getOrDefault("content", "");
                String recipients = (String) request.getOrDefault("recipients", "");

                // 1. DB 审计 — 永久记录，可追溯
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                jdbcTemplate.update(
                    "INSERT INTO t_notification_retry (alert_id, device_id, channel, content, recipients, " +
                    "status, retry_count, max_retry, error_msg, next_retry_time, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, 'PENDING', 0, ?, ?, ?, ?, ?)",
                    alertId, deviceId, channel, content, recipients,
                    MAX_RETRY,
                    truncate(cause.getMessage(), 500),
                    now,  // next_retry_time = now（立即进入MQ延迟队列）
                    now, now
                );

                // 2. 获取自增ID（MySQL LAST_INSERT_ID）
                Long retryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

                // 3. MQ 延迟重试 — 指数退避
                NotificationMessage msg = NotificationMessage.create(
                    retryId, alertId, deviceId, channel, "PHONE",
                    content, recipients, 0, MAX_RETRY
                );

                // 发送到延迟等待队列，TTL=1分钟（首次重试）
                sendWithDelay(msg);
                log.info("通知已入MQ延迟队列, retryId={}, delay={}s", retryId, msg.getBackoffDelayMs() / 1000);

            } catch (Exception e) {
                log.error("通知降级处理失败（DB+MQ双重写入异常）", e);
            }

            return Result.fail("通知服务暂不可用，已自动进入延迟重试队列");
        };
    }

    /**
     * 发送延迟消息到 wait 队列
     * TTL = 指数退避计算值（per-message TTL 覆盖队列默认 TTL）
     */
    public void sendWithDelay(NotificationMessage msg) {
        long ttlMs = msg.getBackoffDelayMs();
        MessagePostProcessor ttlProcessor = message -> {
            message.getMessageProperties().setExpiration(String.valueOf(ttlMs));
            return message;
        };

        rabbitTemplate.convertAndSend(
            "iot.notification.retry.exchange",
            "iot.notification.wait",
            msg,
            ttlProcessor
        );

        log.info("通知延迟重试入队: retryId={}, retryCount={}/{}, ttl={}s",
            msg.getRetryId(), msg.getRetryCount() + 1, msg.getMaxRetry(), ttlMs / 1000);
    }

    private String truncate(String msg, int maxLen) {
        if (msg == null) return "";
        return msg.length() > maxLen ? msg.substring(0, maxLen) : msg;
    }
}
