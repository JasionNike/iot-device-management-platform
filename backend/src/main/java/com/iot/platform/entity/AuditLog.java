package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作审计日志实体 — 记录所有写操作的审计轨迹
 *
 * @author 王恒
 */
@Data
@TableName("t_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operatorId;      // 操作人ID（用户名）
    private String operatorName;    // 操作人姓名
    private String operation;       // 操作类型（类名.方法名）
    private String targetType;      // 操作对象类型
    private String targetId;        // 操作对象ID
    private String detail;          // 操作详情（参数+结果+耗时+IP）
    private String status;          // SUCCESS / FAILED
    private LocalDateTime createTime;
}
