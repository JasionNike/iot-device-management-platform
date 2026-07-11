package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_device_event")
public class DeviceEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String eventType;
    private String eventData;
    private LocalDateTime eventTime;
    private LocalDateTime createTime;
}
