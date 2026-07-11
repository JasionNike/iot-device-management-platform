package com.iot.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置 — 无状态JWT认证体系
 * <p>
 * 核心设计：
 * 1. 禁用Session（STATELESS），每次请求独立认证
 * 2. JWT过滤器在 UsernamePasswordAuthenticationFilter 之前执行
 * 3. 开启方法级鉴权 @PreAuthorize
 * 4. BCrypt加密用户密码
 * <p>
 * 角色体系：
 * - ROLE_ADMIN：全部权限
 * - ROLE_OPERATOR：设备操作+告警处理
 * - ROLE_VIEWER：只读查询
 *
 * @author 王恒
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF（REST API不需要）
            .csrf().disable()
            // 无状态会话
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 请求授权规则
            .authorizeRequests()
                // 白名单
                .antMatchers("/api/auth/**").permitAll()
                .antMatchers("/actuator/health").permitAll()
                .antMatchers("/api/device/dashboard").permitAll()
                // Knife4j / Swagger 文档
                .antMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                // 只读接口 VIEWER即可
                .antMatchers(HttpMethod.GET, "/api/**").authenticated()
                // 写操作需要更高权限
                .antMatchers("/api/product/**", "/api/ota/task", "/api/ota/firmware").hasAnyRole("ADMIN", "OPERATOR")
                .antMatchers("/api/alert/handle").hasAnyRole("ADMIN", "OPERATOR")
                // 其余需要认证
                .anyRequest().authenticated()
            .and()
            // JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
