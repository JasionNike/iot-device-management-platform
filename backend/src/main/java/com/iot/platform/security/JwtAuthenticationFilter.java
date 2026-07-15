package com.iot.platform.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器 — 从请求头提取Token并设置安全上下文
 * <p>
 * 每个请求经过此过滤器时：
 * 1. 从 Authorization Header 提取 Bearer Token
 * 2. 校验Token有效性
 * 3. 提取用户名和角色 → 设置 SecurityContext
 * 4. 白名单路径直接放行
 * <p>
 * 此过滤器在 Spring Security 过滤器链中执行，
 * 优先级高于方法级的 @PreAuthorize 注解校验。
 *
 * @author 王恒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /** 无需认证的白名单路径 */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/captcha",
            "/api/auth/register",
            "/actuator/health",
            "/api/device/dashboard"  // 大盘允许匿名访问
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // 白名单放行
        if (isWhiteListed(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取并校验Token
        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsername(token);
            String roles = jwtUtil.getRoles(token);

            List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(","))
                    .filter(StringUtils::hasText)
                    .map(r -> new SimpleGrantedAuthority(r.trim()))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT认证成功: username={}, roles={}, path={}", username, roles, path);
        } else if (token != null) {
            log.warn("JWT无效或已过期: path={}", path);
        }

        filterChain.doFilter(request, response);
    }

    /** 从 Authorization Header 提取 Bearer Token */
    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }
}
