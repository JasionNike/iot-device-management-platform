package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_alert_rule")
public class AlertRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField(exist = false)
    private Long ruleId;
    private String ruleName;
    private String ruleType;
    @TableField(exist = false)
    private String eventType;
    private String productKey;
    private String conditionJson;
    private String alertLevel;
    private Integer convergeWindow;
    @TableField("is_enabled")
    private Integer enabled;
    private String description;
    private LocalDateTime createTime;
}
