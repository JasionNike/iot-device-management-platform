package com.iot.mock.device;

import com.iot.mock.config.MockConfig;
import java.util.HashMap;
import java.util.Map;

/** 金融自助终端模拟设备（运行状态/钞箱余量/网络强度/打印机） */
public class FinancialTerminalDevice extends AbstractMockDevice {

    public FinancialTerminalDevice(String deviceSn, MockConfig config, int index) {
        super(deviceSn, config, index);
    }

    @Override public String getProductKey() { return "FIN-ATM-001"; }
    @Override public String getDeviceTypeName() { return "Mock-FinTerm"; }

    @Override
    public Map<String, Object> generateTelemetry() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", random.nextInt(100) < 95 ? "NORMAL" : "WARNING");
        data.put("cash_remaining", random.nextInt(500000) + 100000);
        data.put("network_strength", random.nextInt(40) + 60);
        data.put("printer_paper", random.nextInt(80) + 20);
        data.put("transaction_count", random.nextInt(200));
        return data;
    }
}
