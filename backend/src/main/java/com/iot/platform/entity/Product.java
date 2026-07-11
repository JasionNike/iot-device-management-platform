package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productKey;
    private String productName;
    private String manufacturer;
    private String deviceType;
    private String protocol;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
