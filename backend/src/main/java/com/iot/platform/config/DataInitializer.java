package com.iot.platform.config;

import com.iot.platform.entity.User;
import com.iot.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器 — 确保系统启动时基础数据就绪
 * <p>
 * 1. 创建默认管理员账号（如不存在）
 * 2. 创建默认运维和只读账号（演示RBAC角色体系）
 *
 * @author 王恒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        createUserIfAbsent("admin", "admin123", "系统管理员", "ROLE_ADMIN,ROLE_OPERATOR,ROLE_VIEWER");
        createUserIfAbsent("operator", "oper123", "运维工程师", "ROLE_OPERATOR,ROLE_VIEWER");
        createUserIfAbsent("viewer", "view123", "只读用户", "ROLE_VIEWER");

        log.info("默认用户已就绪: admin/admin123(管理员), operator/oper123(运维), viewer/view123(只读)");
    }

    private void createUserIfAbsent(String username, String rawPwd, String realName, String roles) {
        if (userMapper.selectByUsername(username) != null) {
            return; // 已存在，跳过
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPwd));
        user.setRealName(realName);
        user.setRoles(roles);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("默认用户已创建: username={}, roles={}", username, roles);
    }
}
