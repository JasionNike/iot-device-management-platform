package com.iot.platform.aspect;

import com.iot.platform.entity.AuditLog;
import com.iot.platform.mapper.AuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 审计日志切面 — 自动记录所有写操作的审计轨迹
 * <p>
 * 拦截所有 Controller 层 POST/PUT/DELETE 方法，
 * 记录操作人、操作内容、IP地址、执行结果。
 * 查询类(GET)操作不记录，避免日志膨胀。
 * <p>
 * 技术要点：
 * 1. @Around 环绕通知：可在方法执行前后获取参数和结果
 * 2. 从 SecurityContext 获取当前登录用户
 * 3. 异常时也记录失败日志
 * 4. 异步写入不阻塞主流程（通过 @Async 或直接 insert）
 *
 * @author 王恒
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogMapper auditLogMapper;
    private final HttpServletRequest request;

    /** 拦截所有Controller层的POST/PUT/DELETE方法 */
    @Around("execution(* com.iot.platform.controller..*.*(..)) && " +
            "(@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String params = truncate(joinPoint.getArgs());

        // 获取当前用户
        String username = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            username = auth.getName();
        }

        Object result = null;
        String status = "SUCCESS";
        String errorMsg = null;

        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            status = "FAILED";
            errorMsg = truncate(e.getMessage());
            throw e;
        } finally {
            try {
                AuditLog auditLog = new AuditLog();
                auditLog.setOperatorId(username);
                auditLog.setOperatorName(username);
                auditLog.setOperation(className + "." + methodName);
                auditLog.setTargetType("CONTROLLER");
                auditLog.setTargetId("-");
                int duration = (int) (System.currentTimeMillis() - start);
                auditLog.setDetail(String.format("参数:%s | 耗时:%dms | IP:%s | %s",
                        params, duration, getClientIp(), errorMsg != null ? "错误:" + errorMsg : ""));
                auditLog.setStatus(status);
                auditLog.setCreateTime(LocalDateTime.now());
                auditLogMapper.insert(auditLog);
            } catch (Exception e) {
                log.error("审计日志写入失败: {}", e.getMessage());
            }
        }
        return result;
    }

    private String truncate(Object obj) {
        if (obj == null) return "-";
        String s;
        if (obj instanceof Object[]) {
            s = Arrays.stream((Object[]) obj)
                    .map(o -> o != null ? o.toString() : "null")
                    .collect(Collectors.joining(", "));
        } else {
            s = obj.toString();
        }
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
