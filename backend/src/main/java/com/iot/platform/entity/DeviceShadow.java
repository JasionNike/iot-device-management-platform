package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_device_shadow")
public class DeviceShadow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String shadowJson;
    private Integer version;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
}
