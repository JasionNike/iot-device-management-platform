package com.iot.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 智能物联设备管理平台 — API 网关
 *
 * 职责：
 * 1. 统一入口 — 外部请求通过网关进入，隐藏内部服务拓扑
 * 2. 认证鉴权 — 全局过滤器校验 Token，白名单路径放行
 * 3. 限流保护 — Sentinel Gateway 维度限流，防止恶意请求
 * 4. 路由转发 — 通过 Nacos 服务发现动态路由到下游服务
 * 5. 链路追踪 — 生成 TraceId 透传到下游
 *
 * @author 王恒
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
