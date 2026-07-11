package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品事件定义 — 能力模型的事件部分
 *
 * 定义设备可能上报的事件类型，如告警事件、故障事件、OTA状态变更等。
 * 事件可以包含输出参数，作为告警规则匹配的依据。
 * 平台根据事件定义来决定如何解析和路由设备上报的事件数据。
 *
 * 业务场景：
 * - 温度传感器上报"超温告警"事件
 * - 金融终端上报"钞箱将空"事件
 * - 能源采集器上报"电压异常"事件
 *
 * @author 王恒
 */
@Data
@TableName("t_product_event")
public class ProductEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productKey;          // 所属产品标识
    private String eventName;           // 事件标识名
    private String eventAlias;          // 事件别名
    private String eventType;           // 事件类型：INFO/WARN/ERROR/FATAL
    private String outputParams;        // 输出参数JSON：定义事件携带的数据字段
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
