package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.dto.LoginRequest;
import com.iot.platform.dto.LoginResponse;
import com.iot.platform.entity.User;
import com.iot.platform.mapper.UserMapper;
import com.iot.platform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器 — 登录/登出/Token刷新（含登录失败限流）
 * <p>
 * 安全机制：
 * 1. BCrypt密码加密存储
 * 2. JWT令牌2小时过期
 * 3. 同一IP 5分钟内连续失败5次 → 锁定5分钟（Redis计数器）
 * <p>
 * 生产环境建议增加：验证码、短信二次验证、异地登录检测
 *
 * @author 王恒
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final HttpServletRequest request;

    @Value("${iot.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${iot.login.lock-minutes:5}")
    private int lockMinutes;

    /** 用户登录 — 校验凭证 → 签发JWT */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String clientIp = getClientIp();

        // 1. 登录限流检查（Redis计数器）
        String lockKey = "login:lock:" + clientIp;
        String attemptsKey = "login:attempts:" + clientIp;
        String locked = redisTemplate.opsForValue().get(lockKey);
        if ("1".equals(locked)) {
            Long ttl = redisTemplate.getExpire(lockKey);
            log.warn("登录被限流: ip={}, username={}, 剩余锁定时间={}s", clientIp, request.getUsername(), ttl);
            return Result.fail("登录过于频繁，请" + (ttl != null && ttl > 0 ? ttl + "秒" : lockMinutes + "分钟") + "后再试");
        }

        // 2. 校验凭证
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(),
                user.getPassword() != null ? user.getPassword() : "")) {
            // 记录失败次数
            Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
            if (attempts == 1) {
                redisTemplate.expire(attemptsKey, lockMinutes, TimeUnit.MINUTES);
            }
            if (attempts >= maxAttempts) {
                redisTemplate.opsForValue().set(lockKey, "1", lockMinutes, TimeUnit.MINUTES);
                log.warn("IP登录锁定: ip={}, 连续失败{}次", clientIp, attempts);
                return Result.fail("登录失败次数过多，IP已锁定" + lockMinutes + "分钟");
            }
            log.warn("登录失败: ip={}, username={}, 第{}次", clientIp, request.getUsername(), attempts);
            return Result.fail("用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail("账户已被禁用，请联系管理员");
        }

        // 3. 登录成功 → 清除失败计数 + 签发JWT
        redisTemplate.delete(attemptsKey);
        redisTemplate.delete(lockKey);

        String roles = user.getRoles() != null ? user.getRoles() : "ROLE_VIEWER";
        String token = jwtUtil.generateToken(user.getUsername(), roles);

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: username={}, roles={}, ip={}", user.getUsername(), roles, clientIp);

        LoginResponse response = new LoginResponse(
                token, user.getUsername(),
                user.getRealName() != null ? user.getRealName() : user.getUsername(),
                roles, 7200L
        );
        return Result.success(response);
    }

    /** 获取当前用户信息 */
    @GetMapping("/me")
    public Result<LoginResponse> currentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return Result.fail("Token已过期");
        }
        String username = jwtUtil.getUsername(token);
        String roles = jwtUtil.getRoles(token);
        User user = userMapper.selectByUsername(username);
        return Result.success(new LoginResponse(
                token, username,
                user != null ? user.getRealName() : username,
                roles, 7200L
        ));
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
