package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_alert_record")
public class AlertRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String alertId;
    private String deviceId;
    private Long ruleId;
    private String alertLevel;
    private String alertType;
    private String alertContent;
    private String triggerValue;
    private String handleStatus;
    private Long handlerId;
    private String handlerName;
    private String handleComment;
    private LocalDateTime handleTime;
    private LocalDateTime alertTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
