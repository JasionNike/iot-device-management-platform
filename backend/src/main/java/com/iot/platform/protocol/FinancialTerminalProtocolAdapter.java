package com.iot.platform.protocol;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 金融自助终端协议适配器
 *
 * 适配FIN-ATM-001系列金融终端设备。
 * 处理不同厂商的字段命名差异，统一映射到平台标准字段。
 *
 * 字段映射规则（厂商私有字段 → 平台标准字段）：
 *   run_status/status               → status（运行状态：NORMAL/WARNING/ERROR）
 *   cash_left/cash_remaining        → cash_remaining（钞箱余量，单位元）
 *   signal/network_strength         → network_strength（网络信号强度，0-100）
 *   paper_left/printer_paper        → printer_paper（打印机纸量，0-100）
 *   tx_count/transaction_count      → transaction_count（交易笔数）
 *
 * 业务场景：银行网点的ATM、VTM等自助终端来自不同供应商，
 * 通过协议适配层实现统一接入和运维监控。
 *
 * @author 王恒
 */
@Slf4j
public class FinancialTerminalProtocolAdapter extends AbstractProtocolAdapter {

    public FinancialTerminalProtocolAdapter() {
        super("FIN-ATM-001");
    }

    @Override
    public Map<String, Object> decode(String deviceId, String rawPayload) {
        JSONObject json = JSONUtil.parseObj(rawPayload);
        Map<String, Object> result = new HashMap<>();

        result.put("status", json.getStr("status") != null
                ? json.getStr("status") : json.getStr("run_status", "NORMAL"));
        result.put("cash_remaining", json.getObj("cash_remaining") != null
                ? json.getObj("cash_remaining") : json.getObj("cash_left"));
        result.put("network_strength", json.getObj("network_strength") != null
                ? json.getObj("network_strength") : json.getObj("signal"));
        result.put("printer_paper", json.getObj("printer_paper") != null
                ? json.getObj("printer_paper") : json.getObj("paper_left"));
        result.put("transaction_count", json.getObj("transaction_count") != null
                ? json.getObj("transaction_count") : json.getObj("tx_count"));

        log.debug("金融终端协议解码完成：deviceId={}, data={}", deviceId, result);
        return result;
    }

    @Override
    public String encode(String deviceId, String command, Map<String, Object> params) {
        JSONObject json = new JSONObject();
        json.set("cmd", command);
        json.set("params", params);
        return json.toString();
    }
}
