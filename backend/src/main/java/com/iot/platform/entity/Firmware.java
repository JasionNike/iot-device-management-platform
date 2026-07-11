package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_firmware")
public class Firmware {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String firmwareId;
    private String productKey;
    private String version;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String md5;
    private String description;
    private LocalDateTime uploadTime;
    private String status;
}
