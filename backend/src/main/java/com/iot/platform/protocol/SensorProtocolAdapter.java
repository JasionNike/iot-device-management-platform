package com.iot.platform.protocol;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 温湿度传感器协议适配器
 *
 * 适配SENSOR-TH-001系列温湿度传感器。
 * 处理不同厂商的字段命名差异，统一映射到平台标准字段。
 *
 * 字段映射规则（厂商私有字段 → 平台标准字段）：
 *   temp/temperature        → temperature（温度，单位℃）
 *   hum/humidity            → humidity（湿度，单位%）
 *   pm/pm25                 → pm25（PM2.5）
 *   bat/battery             → battery（电量，单位%）
 *
 * 业务场景：工业现场有多个传感器厂商，A厂商用temp，B厂商用temperature，
 * 适配器统一映射为temperature，上层业务无需关心厂商差异。
 *
 * @author 王恒
 */
@Slf4j
public class SensorProtocolAdapter extends AbstractProtocolAdapter {

    public SensorProtocolAdapter() {
        super("SENSOR-TH-001", "SENSOR-TH-002");
    }

    @Override
    public Map<String, Object> decode(String deviceId, String rawPayload) {
        JSONObject json = JSONUtil.parseObj(rawPayload);
        Map<String, Object> result = new HashMap<>();

        // 字段映射：优先使用标准字段名，其次使用厂商私有字段名
        result.put("temperature", json.getDouble("temperature") != null
                ? json.getDouble("temperature") : json.getDouble("temp", 0.0));
        result.put("humidity", json.getDouble("humidity") != null
                ? json.getDouble("humidity") : json.getDouble("hum", 0.0));
        result.put("pm25", json.getInt("pm25") != null
                ? json.getInt("pm25") : json.getInt("pm", 0));
        result.put("battery", json.getInt("battery") != null
                ? json.getInt("battery") : json.getInt("bat", 0));

        log.debug("传感器协议解码完成：deviceId={}, data={}", deviceId, result);
        return result;
    }

    @Override
    public String encode(String deviceId, String command, Map<String, Object> params) {
        JSONObject json = new JSONObject();
        json.set("command", command);
        json.set("params", params);
        json.set("timestamp", System.currentTimeMillis());
        return json.toString();
    }
}
