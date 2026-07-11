package com.iot.platform.feign;

import com.iot.platform.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * 通知服务 Feign 客户端
 * <p>
 * 调用链路：AlertService → Feign → Notification Stub(:8082)
 * 优先通过 Nacos 服务发现定位，Nacos不可用时降级为直连URL。
 * 熔断降级：NotificationClientFallback → 记录 t_notification_retry → 定时补发
 * </p>
 *
 * @author 王恒
 */
@FeignClient(
    name = "iot-notification-service",
    url = "${iot.notification.service-url:http://localhost:8082}",
    path = "/api/notification",
    fallbackFactory = NotificationClientFallback.class,
    configuration = FeignConfig.class
)
public interface NotificationClient {

    /**
     * 发送告警通知（短信/邮件/App推送）
     *
     * @param request 通知请求 {alertId, deviceId, channel, content, recipients}
     * @return 发送结果
     */
    @PostMapping("/send")
    Result<Map<String, Object>> send(@RequestBody Map<String, Object> request);
}
