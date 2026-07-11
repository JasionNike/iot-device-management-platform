package com.iot.platform.protocol;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 消费网关协议适配器
 *
 * 适配CONSUMER-GW-001系列智能网关设备。
 * 处理不同厂商的字段命名差异，统一映射到平台标准字段。
 *
 * 字段映射规则（厂商私有字段 → 平台标准字段）：
 *   cpu/cpu_usage                   → cpu_usage（CPU使用率，%）
 *   mem/memory_usage                → memory_usage（内存使用率，%）
 *   conn_dev/connected_devices      → connected_devices（连接设备数）
 *   up_traffic/uplink_traffic       → uplink_traffic（上行流量，KB/s）
 *   down_traffic/downlink_traffic   → downlink_traffic（下行流量，KB/s）
 *
 * 业务场景：智能家居/消费电子场景中，不同品牌的网关设备接入平台，
 * 协议适配层屏蔽厂商差异，为上层统一监控和管理提供标准化数据。
 *
 * @author 王恒
 */
@Slf4j
public class ConsumerGatewayProtocolAdapter extends AbstractProtocolAdapter {

    public ConsumerGatewayProtocolAdapter() {
        super("CONSUMER-GW-001");
    }

    @Override
    public Map<String, Object> decode(String deviceId, String rawPayload) {
        JSONObject json = JSONUtil.parseObj(rawPayload);
        Map<String, Object> result = new HashMap<>();

        result.put("cpu_usage", json.getObj("cpu_usage") != null
                ? json.getObj("cpu_usage") : json.getObj("cpu"));
        result.put("memory_usage", json.getObj("memory_usage") != null
                ? json.getObj("memory_usage") : json.getObj("mem"));
        result.put("connected_devices", json.getObj("connected_devices") != null
                ? json.getObj("connected_devices") : json.getObj("conn_dev"));
        result.put("uplink_traffic", json.getObj("uplink_traffic") != null
                ? json.getObj("uplink_traffic") : json.getObj("up_traffic"));
        result.put("downlink_traffic", json.getObj("downlink_traffic") != null
                ? json.getObj("downlink_traffic") : json.getObj("down_traffic"));

        log.debug("消费网关协议解码完成：deviceId={}, data={}", deviceId, result);
        return result;
    }

    @Override
    public String encode(String deviceId, String command, Map<String, Object> params) {
        JSONObject json = new JSONObject();
        json.set("command", command);
        json.set("data", params);
        return json.toString();
    }
}
