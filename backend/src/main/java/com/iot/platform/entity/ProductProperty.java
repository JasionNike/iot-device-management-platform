package com.iot.platform.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 产品属性定义 — 能力模型的核心组成部分
 *
 * 定义产品的数据采集点，如温度传感器产品的temperature/humidity属性。
 * 每个属性包含数据类型、单位、取值范围等约束。
 * 设备注册时关联产品productKey，从而继承产品的能力模型定义。
 * 平台根据属性定义校验设备上报数据的合法性。
 *
 * @author 王恒
 */
@Data
@TableName("t_product_property")
public class ProductProperty {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productKey;          // 所属产品标识
    private String propertyName;        // 属性标识名，如temperature
    private String propertyAlias;       // 属性别名，如"温度"
    private String dataType;            // 数据类型：INTEGER/FLOAT/STRING/BOOLEAN/ENUM
    private String unit;                // 单位：℃/%/V/A/kW
    private Double minValue;            // 最小值
    private Double maxValue;            // 最大值
    private String defaultValue;        // 默认值
    private String description;         // 描述
    private Integer sortOrder;          // 排序
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
