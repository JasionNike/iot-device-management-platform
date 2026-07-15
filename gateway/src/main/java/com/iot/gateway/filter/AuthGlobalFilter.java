package com.iot.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 全局认证过滤器（商用标准）
 *
 * 过滤器链顺序：AuthGlobalFilter(-100) → Sentinel Filter → Route Filter
 *
 * 职责：
 * 1. TraceId 生成与透传 — 全链路追踪
 * 2. Token 校验 — 白名单路径放行，其他需 Bearer Token
 * 3. 请求日志 — 记录 method + path + traceId
 *
 * @author 王恒
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthGlobalFilter.class);

    /** 白名单路径 — 无需认证 */
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/auth/login",           // 登录接口
        "/api/auth/captcha",         // 验证码
        "/api/device/dashboard",     // 统计大盘
        "/api/device/register",      // 设备注册（MQTT自动注册）
        "/actuator/health",          // 健康检查
        "/actuator/info"             // 应用信息
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 生成 TraceId 并透传到下游
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        ServerHttpRequest mutatedRequest = request.mutate()
            .header("X-Trace-Id", traceId)
            .build();
        exchange = exchange.mutate().request(mutatedRequest).build();

        // 2. 白名单路径跳过认证
        if (isWhiteListed(path)) {
            log.debug("[Gateway] {} {} [traceId={}] (白名单放行)", request.getMethod(), path, traceId);
            return chain.filter(exchange);
        }

        // 3. Bearer Token 存在性校验（JWT有效性由后端 Spring Security 校验）
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[Gateway] {} {} [traceId={}] — 未提供Token", request.getMethod(), path, traceId);
            return unauthorizedResponse(exchange, "未提供认证Token，请先登录");
        }

        log.debug("[Gateway] {} {} [traceId={}] (Token已透传)", request.getMethod(), path, traceId);
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100; // 最先执行
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 返回 401 JSON 响应（而非默认的 HTML 错误页）
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }
}
