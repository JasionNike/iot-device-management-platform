package com.iot.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应DTO
 *
 * @author 王恒
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;       // JWT令牌
    private String username;    // 用户名
    private String realName;    // 真实姓名
    private String roles;       // 角色
    private long expiresIn;     // 过期时间（秒）
}
