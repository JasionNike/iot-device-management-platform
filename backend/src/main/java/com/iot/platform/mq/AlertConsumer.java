package com.iot.platform.mq;

import com.iot.platform.service.AlertService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 告警消息消费者 — 简历核心亮点
 *
 * 从RabbitMQ消费设备事件消息，异步匹配告警规则并生成告警记录。
 * 使用手动ACK保证消息不丢失，消费失败重新入队。
 *
 * @author 王恒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertConsumer {

    private final AlertService alertService;

    /**
     * 消费设备事件，触发告警规则匹配
     * queue: iot.device.event.queue
     */
    @RabbitListener(queues = "iot.device.event.queue")
    public void handleDeviceEvent(Map<String, Object> data, Channel channel, Message message) {
        try {
            String deviceId = (String) data.get("deviceId");
            String eventType = (String) data.getOrDefault("eventType", "telemetry");
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = (Map<String, Object>) data.get("data");
            if (eventData == null) eventData = data;

            log.info("消费设备事件：deviceId={}, eventType={}", deviceId, eventType);

            // 异步匹配告警规则
            alertService.processEvent(deviceId, eventType, eventData);

            // 手动ACK确认消费
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("设备事件消费失败：{}", e.getMessage());
            try {
                // 消费失败，NACK重新入队
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            } catch (IOException ex) {
                log.error("NACK失败", ex);
            }
        }
    }
}
