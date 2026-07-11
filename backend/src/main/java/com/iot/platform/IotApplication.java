package com.iot.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 智能物联设备管理平台 - 启动类
 *
 * @author 王恒
 */
@SpringBootApplication
@EnableDiscoveryClient                      // 注册到Nacos服务发现中心
@EnableFeignClients(basePackages = "com.iot.platform.feign")  // 启用OpenFeign声明式调用
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy                       // 启用AOP（审计日志切面）
public class IotApplication {
    public static void main(String[] args) {
        SpringApplication.run(IotApplication.class, args);
    }
}
