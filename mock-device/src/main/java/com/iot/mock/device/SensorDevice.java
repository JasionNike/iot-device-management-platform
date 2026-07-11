package com.iot.mock.device;

import com.iot.mock.config.MockConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能传感器模拟设备（温度/湿度/PM2.5/电量）
 * 行业：工业 | 协议：MQTT（演示用HTTP代替）
 */
public class SensorDevice extends AbstractMockDevice {
    private double temperature = 25.0;
    private double humidity = 55.0;

    public SensorDevice(String deviceSn, MockConfig config, int index) {
        super(deviceSn, config, index);
    }

    @Override public String getProductKey() { return "SENSOR-TH-001"; }
    @Override public String getDeviceTypeName() { return "Mock-Sensor"; }

    @Override
    public Map<String, Object> generateTelemetry() {
        // 模拟温度/湿度缓慢变化（正态分布噪声）
        temperature += (random.nextGaussian() * 0.5);
        humidity += (random.nextGaussian() * 1.0);
        humidity = Math.max(0, Math.min(100, humidity));

        Map<String, Object> data = new HashMap<>();
        data.put("temperature", Math.round(temperature * 10.0) / 10.0);
        data.put("humidity", Math.round(humidity * 10.0) / 10.0);
        data.put("pm25", random.nextInt(50) + 10);
        data.put("battery", Math.max(5, 100 - random.nextInt(20) - (deviceIndex % 10) * 3));
        return data;
    }
}
