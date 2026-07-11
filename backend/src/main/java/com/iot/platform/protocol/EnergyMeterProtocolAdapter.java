package com.iot.platform.protocol;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 能源采集器协议适配器
 *
 * 适配ENERGY-MTR-001系列能源采集设备。
 * 处理不同厂商的字段命名差异，统一映射到平台标准字段。
 *
 * 字段映射规则（厂商私有字段 → 平台标准字段）：
 *   v/voltage              → voltage（电压，单位V）
 *   i/current              → current（电流，单位A）
 *   p/power                → power（功率，单位kW）
 *   total_p/total_energy   → total_energy（累计电量，单位kWh）
 *
 * 业务场景：能源管理项目中，不同楼宇使用的能源采集器品牌不同，
 * 私有协议字段命名各异，通过适配层统一为标准数据模型。
 *
 * @author 王恒
 */
@Slf4j
public class EnergyMeterProtocolAdapter extends AbstractProtocolAdapter {

    public EnergyMeterProtocolAdapter() {
        super("ENERGY-MTR-001", "ENERGY-MTR-002");
    }

    @Override
    public Map<String, Object> decode(String deviceId, String rawPayload) {
        JSONObject json = JSONUtil.parseObj(rawPayload);
        Map<String, Object> result = new HashMap<>();

        result.put("voltage", json.getDouble("voltage") != null
                ? json.getDouble("voltage") : json.getDouble("v", 0.0));
        result.put("current", json.getDouble("current") != null
                ? json.getDouble("current") : json.getDouble("i", 0.0));
        result.put("power", json.getDouble("power") != null
                ? json.getDouble("power") : json.getDouble("p", 0.0));
        result.put("total_energy", json.getDouble("total_energy") != null
                ? json.getDouble("total_energy") : json.getDouble("total_p", 0.0));

        log.debug("能源采集器协议解码完成：deviceId={}, data={}", deviceId, result);
        return result;
    }

    @Override
    public String encode(String deviceId, String command, Map<String, Object> params) {
        JSONObject json = new JSONObject();
        json.set("cmd", command);
        json.set("data", params);
        return json.toString();
    }

    /**
     * 能源采集器必须有电压或功率字段
     */
    @Override
    protected boolean doValidate(String deviceId, JSONObject json) {
        return json.containsKey("voltage") || json.containsKey("v")
                || json.containsKey("power") || json.containsKey("p");
    }
}
