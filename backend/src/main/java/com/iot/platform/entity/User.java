package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 平台用户实体 — 系统登录与RBAC权限控制
 *
 * @author 王恒
 */
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;        // 用户名（唯一）
    private String password;        // BCrypt加密密文
    private String realName;        // 真实姓名
    private String roles;           // 角色：ROLE_ADMIN,ROLE_OPERATOR,ROLE_VIEWER（逗号分隔）
    private String phone;           // 手机号
    private String email;           // 邮箱
    private Integer status;         // 1=启用 0=禁用
    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
