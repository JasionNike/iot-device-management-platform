package com.iot.platform.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JWT 工具类 — 无状态认证令牌的生成与校验
 * <p>
 * 使用 HMAC-SHA256 签名算法，令牌包含：
 * - sub: 用户名
 * - roles: 角色列表（逗号分隔）
 * - iat: 签发时间
 * - exp: 过期时间
 * <p>
 * 生产环境密钥应通过配置中心下发，此处为演示方便使用配置项。
 *
 * @author 王恒
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${iot.jwt.secret:iot-platform-secret-key-2024-min-length-32}")
    private String secret;

    @Value("${iot.jwt.expiration:7200000}")
    private long expiration; // 默认2小时

    private SecretKey getKey() {
        // HMAC-SHA256 要求密钥至少256位(32字节)
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** 生成JWT令牌 */
    public String generateToken(String username, String roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** 从令牌中提取用户名 */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** 从令牌中提取角色 */
    public String getRoles(String token) {
        return parseClaims(token).get("roles", String.class);
    }

    /** 校验令牌是否有效 */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT无效: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
