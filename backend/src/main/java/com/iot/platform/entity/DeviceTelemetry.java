package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_device_telemetry")
public class DeviceTelemetry {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String propertyName;
    private String propertyValue;
    private String dataType;
    private LocalDateTime reportTime;
    private LocalDateTime createTime;
}
