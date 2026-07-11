package com.iot.mock.notification.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 通知发送挡板控制器
 *
 * 模拟通知服务（短信/邮件/App推送）的行为：
 * - 60% 成功率（模拟真实通知服务的偶然失败，触发 Sentinel 熔断降级）
 * - 随机 0-500ms 延迟（模拟网络波动）
 * - 完整的请求日志
 *
 * @author 王恒
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);
    private static final Random RANDOM = new Random();
    private static final double SUCCESS_RATE = 0.6; // 60% 成功率

    /**
     * 发送通知
     *
     * 请求体: { "alertId": 1, "deviceId": "MOCK-SENSOR-0001", "channel": "SMS",
     *           "content": "[IoT告警] 温度过高", "recipients": "13800138000" }
     *
     * 响应: { "code": 200, "message": "success", "data": { "sendId": "...", "status": "SENT" } }
     */
    @PostMapping("/send")
    public Map<String, Object> send(@RequestBody Map<String, Object> request) {
        String deviceId = (String) request.getOrDefault("deviceId", "unknown");
        String channel = (String) request.getOrDefault("channel", "SMS");
        String content = (String) request.getOrDefault("content", "");
        String traceId = extractTraceId();

        // 模拟网络延迟 0-500ms
        int delay = RANDOM.nextInt(500);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        // 60% 概率成功
        boolean success = RANDOM.nextDouble() < SUCCESS_RATE;

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = new HashMap<>();

        if (success) {
            String sendId = "SEND-" + System.currentTimeMillis() + "-" + RANDOM.nextInt(10000);
            data.put("sendId", sendId);
            data.put("status", "SENT");
            data.put("channel", channel);
            data.put("sendTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            data.put("delay", delay + "ms");

            result.put("code", 200);
            result.put("message", "success");
            result.put("data", data);

            log.info("[通知挡板] 发送成功, deviceId={}, channel={}, delay={}ms, traceId={}",
                deviceId, channel, delay, traceId);
        } else {
            data.put("status", "FAILED");
            data.put("reason", "运营商返回：号码不在服务区");
            data.put("channel", channel);

            result.put("code", 500);
            result.put("message", "发送失败");
            result.put("data", data);

            log.warn("[通知挡板] 发送失败(模拟), deviceId={}, channel={}, traceId={}",
                deviceId, channel, traceId);
        }

        return result;
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "iot-notification-service");
        result.put("successRate", SUCCESS_RATE);
        result.put("time", LocalDateTime.now().toString());
        return result;
    }

    private String extractTraceId() {
        // 从MDC或请求头获取traceId（简化实现）
        return "trace-" + System.currentTimeMillis() % 100000;
    }
}
