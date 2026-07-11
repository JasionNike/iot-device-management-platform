package com.iot.mock;

import com.iot.mock.config.MockConfig;
import com.iot.mock.device.AbstractMockDevice;
import com.iot.mock.device.ConsumerGatewayDevice;
import com.iot.mock.device.EnergyMeterDevice;
import com.iot.mock.device.FinancialTerminalDevice;
import com.iot.mock.device.SensorDevice;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Mock Device Simulator — 启动类
 *
 * 模拟200台物联网设备同时接入平台，进行：
 * 1. 自动注册
 * 2. 定时遥测数据上报（30秒/次）
 * 3. 心跳保持
 * 4. 随机事件/告警触发
 *
 * 使用方法:
 *   java -jar mock-device-simulator.jar --count=50 --product=SENSOR-TH-001
 *
 * @author 王恒
 */
public class MockDeviceApplication {

    public static void main(String[] args) {
        // 默认参数
        int count = 100;          // 传感器数量
        int energyCount = 50;     // 能源采集器数量
        int terminalCount = 20;   // 金融终端数量
        int gatewayCount = 30;    // 网关数量
        String platformUrl = "http://localhost:8081/api";
        int interval = 30;        // 上报间隔（秒）

        // 解析命令行参数
        for (int i = 0; i < args.length; i++) {
            if ("--count".equals(args[i])) count = Integer.parseInt(args[++i]);
            if ("--energy".equals(args[i])) energyCount = Integer.parseInt(args[++i]);
            if ("--terminal".equals(args[i])) terminalCount = Integer.parseInt(args[++i]);
            if ("--gateway".equals(args[i])) gatewayCount = Integer.parseInt(args[++i]);
            if ("--url".equals(args[i])) platformUrl = args[++i];
            if ("--interval".equals(args[i])) interval = Integer.parseInt(args[++i]);
        }

        MockConfig config = new MockConfig(platformUrl, interval);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
        List<AbstractMockDevice> allDevices = new ArrayList<>();

        System.out.println("========================================");
        System.out.println("  IoT Mock Device Simulator");
        System.out.println("  Platform: " + platformUrl);
        System.out.println("  Report interval: " + interval + "s");
        System.out.println("========================================");

        // 创建传感器设备
        System.out.println("[1/4] Creating " + count + " sensor devices...");
        for (int i = 1; i <= count; i++) {
            String deviceSn = "MOCK-SENSOR-" + String.format("%04d", i);
            SensorDevice device = new SensorDevice(deviceSn, config, i);
            allDevices.add(device);
        }

        // 创建能源采集器
        System.out.println("[2/4] Creating " + energyCount + " energy meter devices...");
        for (int i = 1; i <= energyCount; i++) {
            String deviceSn = "MOCK-ENERGY-" + String.format("%04d", i);
            EnergyMeterDevice device = new EnergyMeterDevice(deviceSn, config, i);
            allDevices.add(device);
        }

        // 创建金融终端
        System.out.println("[3/4] Creating " + terminalCount + " financial terminal devices...");
        for (int i = 1; i <= terminalCount; i++) {
            String deviceSn = "MOCK-FIN-" + String.format("%04d", i);
            FinancialTerminalDevice device = new FinancialTerminalDevice(deviceSn, config, i);
            allDevices.add(device);
        }

        // 创建消费网关
        System.out.println("[4/4] Creating " + gatewayCount + " consumer gateway devices...");
        for (int i = 1; i <= gatewayCount; i++) {
            String deviceSn = "MOCK-GW-" + String.format("%04d", i);
            ConsumerGatewayDevice device = new ConsumerGatewayDevice(deviceSn, config, i);
            allDevices.add(device);
        }

        // 注册所有设备
        System.out.println("\nRegistering " + allDevices.size() + " devices...");
        for (AbstractMockDevice device : allDevices) {
            device.register(scheduler);
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }

        System.out.println("\n========================================");
        System.out.println("  All " + allDevices.size() + " devices started!");
        System.out.println("  Press Ctrl+C to stop");
        System.out.println("========================================");

        // 保持运行
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down all devices...");
            scheduler.shutdownNow();
        }));

        while (!Thread.currentThread().isInterrupted()) {
            try { Thread.sleep(5000); } catch (InterruptedException e) { break; }
        }
    }
}
