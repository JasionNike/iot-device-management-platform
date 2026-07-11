package com.iot.platform.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 事件生产者 — 简历核心亮点
 *
 * 将设备遥测数据和事件发送到RabbitMQ，由消费者异步处理，
 * 实现设备事件异步处理、失败重试和削峰。
 *
 * @author 王恒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventProducer {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "iot.exchange";

    /** 发送遥测数据事件 */
    public void sendTelemetryEvent(String deviceId, Map<String, Object> data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("deviceId", deviceId);
        msg.put("type", "telemetry");
        msg.put("data", data);
        msg.put("timestamp", System.currentTimeMillis());
        rabbitTemplate.convertAndSend(EXCHANGE, "iot.device.event", msg);
    }

    /** 发送设备事件（告警触发） */
    public void sendDeviceEvent(String deviceId, String eventType, Map<String, Object> data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("deviceId", deviceId);
        msg.put("eventType", eventType);
        msg.put("data", data);
        msg.put("timestamp", System.currentTimeMillis());
        rabbitTemplate.convertAndSend(EXCHANGE, "iot.device.event", msg);
    }

    /** 发送告警通知 */
    public void sendAlert(String alertId, String deviceId, String alertLevel, String content) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("alertId", alertId);
        msg.put("deviceId", deviceId);
        msg.put("alertLevel", alertLevel);
        msg.put("content", content);
        msg.put("timestamp", System.currentTimeMillis());
        rabbitTemplate.convertAndSend(EXCHANGE, "iot.alert", msg);
        log.info("告警消息已发送：alertId={}, level={}", alertId, alertLevel);
    }
}
