package com.iot.gateway.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 网关首页
 * <p>
 * 读取 classpath:static/index.html，作为平台统一入口。
 * HTML 文件与 frontend/index.html 保持同步。
 * </p>
 *
 * @author 王恒
 */
@RestController
public class IndexController {

    private static String cachedHtml;

    @GetMapping("/favicon.ico")
    public Mono<String> favicon() {
        return Mono.just("");
    }

    @GetMapping("/")
    public Mono<String> index() {
        if (cachedHtml != null) {
            return Mono.just(cachedHtml);
        }
        try {
            org.springframework.core.io.Resource resource = new ClassPathResource("static/index.html");
            cachedHtml = org.springframework.util.StreamUtils.copyToString(
                resource.getInputStream(), StandardCharsets.UTF_8);
            return Mono.just(cachedHtml);
        } catch (Exception e) {
            return Mono.just("<h1>智能物联设备管理平台</h1><p>首页加载中，请稍候...</p>");
        }
    }
}
