package com.iot.mock.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通知服务挡板 — 模拟短信/邮件/App推送发送
 *
 * 注册到 Nacos 作为 iot-notification-service，
 * 供 Backend 通过 Feign 调用。
 *
 * 模拟真实通知服务的以下特性：
 * - 60% 概率成功（模拟真实场景中的偶然失败）
 * - 可配置延迟（模拟网络延迟）
 * - 完整的请求/响应日志
 *
 * @author 王恒
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationStubApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationStubApplication.class, args);
    }
}
