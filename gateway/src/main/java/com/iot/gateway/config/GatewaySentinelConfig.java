package com.iot.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * Gateway Sentinel 限流降级配置
 *
 * 当 Sentinel 触发限流时，返回 JSON 格式错误响应（而非默认 HTML 页面），
 * 便于前端统一处理。
 *
 * @author 王恒
 */
@Configuration
public class GatewaySentinelConfig {

    /**
     * IP 维度限流 Key 解析器
     * 按客户端 IP 限流，防止单 IP 刷接口
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = Objects.requireNonNull(
                exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }

    @PostConstruct
    public void init() {
        BlockRequestHandler blockHandler = (ServerWebExchange exchange, Throwable t) ->
            ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");

        GatewayCallbackManager.setBlockHandler(blockHandler);
    }
}
