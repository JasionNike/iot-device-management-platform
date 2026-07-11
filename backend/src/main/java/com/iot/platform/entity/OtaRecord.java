package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_ota_record")
public class OtaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String taskId;
    private String fromVersion;
    private String toVersion;
    private String otaStatus;
    private Integer progress;
    private Integer retryCount;
    private String failReason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
