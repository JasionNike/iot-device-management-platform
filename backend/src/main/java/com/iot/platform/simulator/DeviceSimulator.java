package com.iot.platform.simulator;

import cn.hutool.json.JSONObject;
import com.iot.platform.entity.Device;
import com.iot.platform.mapper.DeviceMapper;
import com.iot.platform.mqtt.MqttGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 设备模拟器 — 模拟真实设备完整生命周期
 * <p>
 * 模拟200台MOCK设备执行完整IoT数据链路：
 * 1. 首次上电 → 设备注册(register)
 * 2. 定时上报 → 遥测数据(telemetry) → 写DB+更新影子+发RabbitMQ
 * 3. 随机触发 → 告警事件(event) → 规则匹配→生成告警记录
 * 4. 保活 → 心跳(heartbeat) → 更新在线状态
 * 5. 偶尔断线 → 心跳超时 → 平台检测离线
 * <p>
 * 所有数据经过 MqttGateway.handleMessage() → MqttMessageHandler.dispatch()
 * 走与真实设备完全相同的业务处理链路。
 *
 * @author 王恒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceSimulator {

    private final MqttGateway mqttGateway;
    private final DeviceMapper deviceMapper;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicInteger roundCount = new AtomicInteger(0);
    private final Random rng = new Random();

    // ========== 启动初始化 ==========

    /** 模拟器启动：所有MOCK设备批量上线（模拟设备上电注册） */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("══════════════════════════════════════════");
        log.info("  设备模拟器启动 — 模拟真实IoT设备行为");
        log.info("══════════════════════════════════════════");

        // 1. 批量上线所有MOCK设备
        int updated = deviceMapper.updateOnlineStatusByPrefix("MOCK-", 1);
        log.info("[模拟器] ① {} 台MOCK设备已批量上线", updated);

        // 2. 每台设备发送注册消息（模拟首次上电注册）
        List<Device> allMock = deviceMapper.selectAllOnlineMockDevices();
        log.info("[模拟器] ② 开始模拟设备注册（{}台）...", allMock.size());
        int regCount = 0;
        for (Device device : allMock) {
            try {
                sendRegister(device);
                regCount++;
                if (regCount % 3 == 0) {
                    try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            } catch (Exception e) {
                log.error("[模拟器] 设备{}注册模拟异常: {}", device.getDeviceSn(), e.getMessage());
            }
        }
        log.info("[模拟器] ② 设备注册完成：{}台", regCount);

        log.info("[模拟器] ③ 遥测+事件+心跳持续上报中（每8秒一轮，每轮~15台）");
        log.info("══════════════════════════════════════════");
    }

    // ========== 核心调度：遥测+事件上报 ==========

    /**
     * 每8秒执行一轮：随机选取~15台在线设备上报遥测或事件
     * <p>
     * 概率分配：
     * - 70% 遥测上报（其中5%为超阈值数据触发告警）
     * - 15% 事件上报（触发告警规则匹配）
     * - 15% 仅发心跳
     */
    @Scheduled(fixedRate = 8000)
    public void simulate() {
        if (!running.get()) return;

        try {
            int round = roundCount.incrementAndGet();
            List<Device> devices = deviceMapper.selectRandomOnlineMockDevices(15);
            if (devices.isEmpty() && round % 20 == 1) {
                log.info("[模拟器] 未找到在线MOCK设备，跳过");
                return;
            }

            for (Device device : devices) {
                try {
                    double dice = rng.nextDouble();
                    if (dice < 0.70) {
                        sendTelemetry(device);       // 遥测
                    } else if (dice < 0.85) {
                        sendEvent(device);            // 事件
                    } else {
                        sendHeartbeat(device);        // 心跳
                    }
                } catch (Exception ignored) {}
            }

            if (round % 50 == 1) {
                log.info("[模拟器] 第{}轮完成 | 本轮模拟{}台 | 持续运行中...", round, devices.size());
            }
        } catch (Exception e) {
            log.error("[模拟器] 调度异常: {}", e.getMessage());
        }
    }

    /** 每30秒批量心跳：随机30台 */
    @Scheduled(fixedRate = 30000)
    public void batchHeartbeat() {
        if (!running.get()) return;
        try {
            List<Device> devices = deviceMapper.selectRandomOnlineMockDevices(30);
            for (Device d : devices) sendHeartbeat(d);
        } catch (Exception ignored) {}
    }

    /** 每90秒：随机让1-2台设备"离线"（模拟断网），5秒后自动恢复 */
    @Scheduled(fixedRate = 90000)
    public void simulateOffline() {
        if (!running.get()) return;
        try {
            List<Device> devices = deviceMapper.selectRandomOnlineMockDevices(2);
            for (Device d : devices) {
                String topic = "iot/devices/" + d.getDeviceId() + "/event";
                JSONObject json = new JSONObject();
                json.set("eventType", "ERROR");
                json.set("eventName", "device_offline");
                json.set("message", "设备离线（模拟断网）");
                json.set("timestamp", System.currentTimeMillis());
                mqttGateway.handleMessage(topic, json.toString());
                log.info("[模拟器] 设备{} 模拟离线", d.getDeviceSn());
            }
        } catch (Exception ignored) {}
    }

    // ========== 设备注册 ==========

    /**
     * 模拟设备首次上电注册
     * Topic: iot/devices/{deviceId}/register
     */
    private void sendRegister(Device device) {
        JSONObject json = new JSONObject();
        json.set("deviceSn", device.getDeviceSn());
        json.set("productKey", device.getProductKey());
        json.set("firmwareVersion", device.getFirmwareVersion() != null ? device.getFirmwareVersion() : "V1.0.0");
        json.set("timestamp", System.currentTimeMillis());
        String topic = "iot/devices/" + device.getDeviceId() + "/register";
        mqttGateway.handleMessage(topic, json.toString());
    }

    // ========== 遥测数据生成 ==========

    private void sendTelemetry(Device device) {
        String topic = "iot/devices/" + device.getDeviceId() + "/telemetry";
        mqttGateway.handleMessage(topic, buildTelemetryPayload(device));
    }

    private String buildTelemetryPayload(Device device) {
        String productKey = device.getProductKey();
        boolean triggerAlert = rng.nextDouble() < 0.05;
        JSONObject data = new JSONObject();

        switch (productKey) {
            case "SENSOR-TH-001": {
                double temp = triggerAlert ? 55 + rng.nextDouble() * 10 : 22 + rng.nextDouble() * 10;
                double hum = 45 + rng.nextDouble() * 20;
                int pm25 = triggerAlert ? 120 + rng.nextInt(80) : 20 + rng.nextInt(50);
                int battery = 50 + rng.nextInt(50);
                data.set("temperature", round(temp, 1));
                data.set("humidity", round(hum, 1));
                data.set("pm25", pm25);
                data.set("battery", battery);
                break;
            }
            case "ENERGY-MTR-001": {
                double voltage = triggerAlert ? 250 + rng.nextDouble() * 15 : 215 + rng.nextDouble() * 15;
                double current = 8 + rng.nextDouble() * 10;
                data.set("voltage", round(voltage, 1));
                data.set("current", round(current, 1));
                data.set("power", round(voltage * current / 1000, 2));
                data.set("total_energy", round(12580 + roundCount.get() * 0.01 + rng.nextDouble(), 1));
                break;
            }
            case "FIN-ATM-001": {
                data.set("status", triggerAlert ? (rng.nextBoolean() ? "ERROR" : "WARN") : "NORMAL");
                data.set("cash_remaining", triggerAlert ? rng.nextInt(20) : 20 + rng.nextInt(60));
                data.set("network_strength", 1 + rng.nextInt(5));
                data.set("printer_paper", 30 + rng.nextInt(70));
                data.set("transaction_count", 500 + rng.nextInt(200));
                break;
            }
            case "CONSUMER-GW-001": {
                data.set("cpu_usage", round(triggerAlert ? 85 + rng.nextDouble() * 15 : 15 + rng.nextDouble() * 60, 1));
                data.set("memory_usage", round(35 + rng.nextDouble() * 45, 1));
                data.set("connected_devices", triggerAlert ? 180 + rng.nextInt(30) : 5 + rng.nextInt(30));
                data.set("uplink_traffic", round(rng.nextDouble() * 10, 1));
                data.set("downlink_traffic", round(rng.nextDouble() * 25, 1));
                break;
            }
            default: {
                data.set("temperature", round(22 + rng.nextDouble() * 10, 1));
                data.set("humidity", round(45 + rng.nextDouble() * 20, 1));
            }
        }
        data.set("timestamp", System.currentTimeMillis());
        return data.toString();
    }

    // ========== 事件上报 ==========

    private void sendEvent(Device device) {
        String topic = "iot/devices/" + device.getDeviceId() + "/event";
        mqttGateway.handleMessage(topic, buildEventPayload(device));
    }

    private String buildEventPayload(Device device) {
        String productKey = device.getProductKey();
        JSONObject event = new JSONObject();
        String eventType, eventName;

        switch (productKey) {
            case "SENSOR-TH-001": {
                int idx = rng.nextInt(3);
                if (idx == 0) { eventType = "WARN"; eventName = "over_temp"; event.set("temperature", round(55 + rng.nextDouble() * 10, 1)); }
                else if (idx == 1) { eventType = "WARN"; eventName = "low_battery"; event.set("battery", rng.nextInt(15)); }
                else { eventType = "ERROR"; eventName = "sensor_fault"; event.set("sensorType", rng.nextBoolean() ? "temperature" : "humidity"); }
                break;
            }
            case "ENERGY-MTR-001": {
                int idx = rng.nextInt(3);
                if (idx == 0) { eventType = "WARN"; eventName = "over_voltage"; event.set("voltage", round(250 + rng.nextDouble() * 15, 1)); event.set("phase", "A"); }
                else if (idx == 1) { eventType = "WARN"; eventName = "over_current"; event.set("current", round(25 + rng.nextDouble() * 10, 1)); }
                else { eventType = "ERROR"; eventName = "phase_loss"; event.set("phase", rng.nextBoolean() ? "B" : "C"); }
                break;
            }
            case "FIN-ATM-001": {
                int idx = rng.nextInt(3);
                if (idx == 0) { eventType = "WARN"; eventName = "cash_low"; event.set("cassetteId", "C" + (1 + rng.nextInt(4))); event.set("remainCount", rng.nextInt(50)); }
                else if (idx == 1) { eventType = "WARN"; eventName = "paper_low"; event.set("paperType", rng.nextBoolean() ? "凭条纸" : "流水纸"); }
                else { eventType = "ERROR"; eventName = "device_error"; event.set("errorCode", "ERR-" + (100 + rng.nextInt(900))); }
                break;
            }
            case "CONSUMER-GW-001": {
                int idx = rng.nextInt(3);
                if (idx == 0) { eventType = "WARN"; eventName = "conn_full"; event.set("currentConns", 180 + rng.nextInt(30)); event.set("maxConns", 200); }
                else if (idx == 1) { eventType = "WARN"; eventName = "cpu_high"; event.set("cpuUsage", round(85 + rng.nextDouble() * 15, 1)); }
                else { eventType = "ERROR"; eventName = "gw_offline"; event.set("lastSeen", LocalDateTime.now().minusMinutes(5 + rng.nextInt(10)).toString()); }
                break;
            }
            default: { eventType = "INFO"; eventName = "status_check"; event.set("message", "设备状态检查"); }
        }

        event.set("eventType", eventType);
        event.set("eventName", eventName);
        event.set("timestamp", System.currentTimeMillis());
        return event.toString();
    }

    // ========== 心跳 ==========

    private void sendHeartbeat(Device device) {
        String topic = "iot/devices/" + device.getDeviceId() + "/heartbeat";
        mqttGateway.handleMessage(topic, "{\"timestamp\":" + System.currentTimeMillis() + "}");
    }

    // ========== 工具 ==========

    private double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
