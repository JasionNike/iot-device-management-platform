package com.iot.platform.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 消息配置 — 商业级标准
 *
 * 队列体系：
 * ┌─────────────────────────────────────────────────┐
 * │ iot.exchange (TopicExchange)                     │
 * │  ├─ iot.device.event.queue  → AlertConsumer     │
 * │  ├─ iot.alert.queue          → AlertConsumer     │
 * │  └─ iot.ota.queue            → OtaConsumer       │
 * ├─────────────────────────────────────────────────┤
 * │ iot.notification.exchange (DirectExchange)       │
 * │  ├─ iot.notification.wait.queue    (TTL延迟队列) │
 * │  └─ iot.notification.retry.queue   (重试消费队列)│
 * │      └─ iot.notification.dlq        (死信队列)   │
 * └─────────────────────────────────────────────────┘
 *
 * 延迟重试原理（DLX + Per-Message TTL）：
 * 1. 消息发到 wait.queue，设置 per-message TTL(指数退避)
 * 2. TTL过期 → 消息被 dead-letter 到 retry.exchange
 * 3. retry.exchange 路由到 retry.queue
 * 4. NotificationRetryConsumer 消费 → Feign重试
 * 5. 仍失败 → 重新发到 wait.queue，TTL增大 → 循环
 * 6. 超过最大重试次数 → 路由到 dlq (死信队列人工介入)
 *
 * @author 王恒
 */
@Configuration
public class RabbitMQConfig {

    // =============================
    // Exchange 定义
    // =============================

    /** IoT 业务 TopicExchange */
    @Bean
    public TopicExchange iotExchange() {
        return new TopicExchange("iot.exchange", true, false);
    }

    /** 通知重试 DirectExchange（用于DLX回流和消费者绑定） */
    @Bean
    public DirectExchange notificationRetryExchange() {
        return new DirectExchange("iot.notification.retry.exchange", true, false);
    }

    // =============================
    // IoT 业务队列
    // =============================

    @Bean
    public Queue deviceEventQueue() {
        return new Queue("iot.device.event.queue", true);
    }

    @Bean
    public Binding deviceEventBinding() {
        return BindingBuilder.bind(deviceEventQueue())
                .to(iotExchange()).with("iot.device.event");
    }

    @Bean
    public Queue alertQueue() {
        return new Queue("iot.alert.queue", true);
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder.bind(alertQueue())
                .to(iotExchange()).with("iot.alert");
    }

    @Bean
    public Queue otaQueue() {
        return new Queue("iot.ota.queue", true);
    }

    @Bean
    public Binding otaBinding() {
        return BindingBuilder.bind(otaQueue())
                .to(iotExchange()).with("iot.ota");
    }

    // =============================
    // 通知重试延迟队列（商业级DLX方案）
    // =============================

    /**
     * 延迟等待队列 — 消息在此队列等待TTL过期
     *
     * x-dead-letter-exchange: 过期后转发到 retry.exchange
     * x-dead-letter-routing-key: 路由到实际的 retry.queue
     * x-message-ttl: 默认TTL 60s（可通过 per-message TTL 覆盖）
     */
    @Bean
    public Queue notificationWaitQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "iot.notification.retry.exchange");
        args.put("x-dead-letter-routing-key", "iot.notification.retry");
        args.put("x-message-ttl", 60000); // 默认1分钟
        return new Queue("iot.notification.wait.queue", true, false, false, args);
    }

    @Bean
    public Binding notificationWaitBinding() {
        return BindingBuilder.bind(notificationWaitQueue())
                .to(notificationRetryExchange()).with("iot.notification.wait");
    }

    /**
     * 重试消费队列 — TTL过期后消息到达此队列，由 Consumer 消费重试
     *
     * x-dead-letter-exchange: 最终失败后转发到 DLQ
     * x-dead-letter-routing-key: 路由到死信队列
     */
    @Bean
    public Queue notificationRetryQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "iot.notification.retry.exchange");
        args.put("x-dead-letter-routing-key", "iot.notification.dlq");
        return new Queue("iot.notification.retry.queue", true, false, false, args);
    }

    @Bean
    public Binding notificationRetryBinding() {
        return BindingBuilder.bind(notificationRetryQueue())
                .to(notificationRetryExchange()).with("iot.notification.retry");
    }

    /**
     * 死信队列（DLQ）— 超过最大重试次数的消息最终进入此队列，人工介入
     */
    @Bean
    public Queue notificationDlq() {
        return new Queue("iot.notification.dlq", true);
    }

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDlq())
                .to(notificationRetryExchange()).with("iot.notification.dlq");
    }

    // =============================
    // 消息转换器
    // =============================

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        // 发送确认回调（生产环境配合 confirmCallback 保证消息不丢）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack && correlationData != null) {
                org.slf4j.LoggerFactory.getLogger(RabbitMQConfig.class)
                    .warn("消息发送未确认: id={}, cause={}", correlationData.getId(), cause);
            }
        });
        return rabbitTemplate;
    }
}
