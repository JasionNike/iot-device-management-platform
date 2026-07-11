package com.iot.mock.device;

import com.iot.mock.config.MockConfig;
import java.util.HashMap;
import java.util.Map;

/** 能源采集器模拟设备（电压/电流/功率/累计电量） */
public class EnergyMeterDevice extends AbstractMockDevice {
    private double power = 5000.0;
    private double totalEnergy = 0;

    public EnergyMeterDevice(String deviceSn, MockConfig config, int index) {
        super(deviceSn, config, index);
        totalEnergy = 100000 + random.nextInt(50000);
    }

    @Override public String getProductKey() { return "ENERGY-MTR-001"; }
    @Override public String getDeviceTypeName() { return "Mock-Energy"; }

    @Override
    public Map<String, Object> generateTelemetry() {
        power += random.nextGaussian() * 100;
        power = Math.max(100, Math.min(15000, power));
        totalEnergy += power * config.getReportIntervalSec() / 3600.0 / 1000.0;

        Map<String, Object> data = new HashMap<>();
        data.put("voltage", 220 + random.nextInt(10) - 5);
        data.put("current", Math.round(power / 220.0 * 10.0) / 10.0);
        data.put("power", Math.round(power * 10.0) / 10.0);
        data.put("total_energy", Math.round(totalEnergy * 10.0) / 10.0);
        return data;
    }
}
