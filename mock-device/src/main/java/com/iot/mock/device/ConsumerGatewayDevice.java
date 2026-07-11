package com.iot.mock.device;

import com.iot.mock.config.MockConfig;
import java.util.HashMap;
import java.util.Map;

/** 消费电子网关模拟设备（CPU/内存/连接设备数/流量） */
public class ConsumerGatewayDevice extends AbstractMockDevice {
    private int connectedDevices = 5;

    public ConsumerGatewayDevice(String deviceSn, MockConfig config, int index) {
        super(deviceSn, config, index);
        connectedDevices = 3 + random.nextInt(20);
    }

    @Override public String getProductKey() { return "CONSUMER-GW-001"; }
    @Override public String getDeviceTypeName() { return "Mock-GW"; }

    @Override
    public Map<String, Object> generateTelemetry() {
        connectedDevices += (random.nextInt(3) - 1);
        connectedDevices = Math.max(1, Math.min(50, connectedDevices));

        Map<String, Object> data = new HashMap<>();
        data.put("cpu_usage", Math.round((40 + random.nextGaussian() * 15) * 10.0) / 10.0);
        data.put("memory_usage", Math.round((55 + random.nextGaussian() * 10) * 10.0) / 10.0);
        data.put("connected_devices", connectedDevices);
        data.put("uplink_traffic", random.nextInt(1000) + 100);
        data.put("downlink_traffic", random.nextInt(5000) + 500);
        return data;
    }
}
