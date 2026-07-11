package com.iot.platform.feign;

import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OpenFeign 全局配置（商用标准）
 * <p>
 * - 连接超时 3s、读取超时 5s（考虑通知服务可能较慢）
 * - 重试 1 次，间隔 1s→2s（指数退避）
 * - 请求拦截器：自动透传 TraceId 到下游服务
 * </p>
 *
 * @author 王恒
 */
@Configuration(proxyBeanMethods = false)
public class FeignConfig {

    /**
     * 超时配置
     * connectTimeout: 建立TCP连接的超时
     * readTimeout: 等待响应的超时（通知服务可能较慢，设5s）
     * followRedirects: 是否跟随重定向
     */
    @Bean
    public Request.Options options() {
        return new Request.Options(3, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }

    /**
     * 重试策略
     * period: 首次重试等待 1000ms
     * maxPeriod: 最大等待 2000ms
     * maxAttempts: 最多重试 1 次（含首次调用共2次）
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(1000, 2000, 1);
    }

    /**
     * 请求拦截器：透传 TraceId
     * 从 MDC 中获取当前请求的 traceId，通过 HTTP Header 传递给下游服务，
     * 实现全链路追踪。
     */
    @Bean
    public RequestInterceptor traceInterceptor() {
        return template -> {
            String traceId = MDC.get("traceId");
            if (traceId == null || traceId.isEmpty()) {
                traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            template.header("X-Trace-Id", traceId);
        };
    }
}
