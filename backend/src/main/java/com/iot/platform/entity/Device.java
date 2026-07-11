package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_device")
public class Device {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String deviceName;
    private String productKey;
    private String deviceSn;
    private String deviceSecret;
    private String firmwareVersion;
    private String location;
    private Double latitude;
    private Double longitude;
    private String address;
    private Integer onlineStatus;
    private LocalDateTime registerTime;
    private LocalDateTime lastOnlineTime;
    private LocalDateTime heartbeatTime;
    private Integer heartbeatMissCount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
