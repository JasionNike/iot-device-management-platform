package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品指令定义 — 能力模型的指令部分
 *
 * 定义平台可向设备下发的指令，如设置温度阈值、重启设备、远程配置等。
 * 指令包含输入参数定义，设备收到指令后执行相应操作并返回执行结果。
 * 指令是端云协同的重要环节，实现平台对设备的远程控制。
 *
 * 业务场景：
 * - 设置温度阈值：下发setThreshold指令，参数{"maxTemp": 60}
 * - 设备重启：下发restart指令，参数{"delay": 5}
 * - 远程配置：下发config指令，参数{"interval": 30}
 *
 * @author 王恒
 */
@Data
@TableName("t_product_command")
public class ProductCommand {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productKey;          // 所属产品标识
    private String commandName;         // 指令标识名
    private String commandAlias;        // 指令别名
    private String inputParams;         // 输入参数JSON：定义指令需要的参数
    private Boolean async;              // 是否异步执行（true=设备响应后回执）
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
