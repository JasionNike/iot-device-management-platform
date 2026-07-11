package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ota_task")
public class OtaTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String taskName;
    private Long firmwareId;
    private String productKey;
    private String targetType;
    private String targetValue;
    private Integer grayPercent;
    private Integer currentPercent;
    private String taskStatus;
    private LocalDateTime timeWindowStart;
    private LocalDateTime timeWindowEnd;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
