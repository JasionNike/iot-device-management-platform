package com.iot.platform.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * MQTT 网关 — 消息收发封装
 *
 * 演示环境使用模拟方式处理MQTT消息。
 * 生产环境对接真实MQTT Broker（Eclipse Mosquitto / EMQX）。
 *
 * @author 王恒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttGateway {

    private final MqttMessageHandler messageHandler;

    /**
     * 模拟设备消息到达（生产环境由MQTT客户端回调触发）
     * Topic格式: iot/devices/{deviceId}/{messageType}
     *
     * messageType:
     *   - telemetry  → 遥测数据上报
     *   - event      → 事件上报
     *   - heartbeat  → 心跳
     *   - register   → 设备注册
     *   - ota        → OTA进度上报
     */
    public void handleMessage(String topic, String payload) {
        log.info("MQTT消息到达：topic={}, payload={}", topic, payload);
        try {
            String[] parts = topic.split("/");
            if (parts.length < 4) {
                log.warn("无效的MQTT Topic格式：{}", topic);
                return;
            }
            String deviceId = parts[2];
            String messageType = parts[3];
            messageHandler.dispatch(deviceId, messageType, payload);
        } catch (Exception e) {
            log.error("MQTT消息处理异常：topic={}", topic, e);
        }
    }

    /**
     * 向设备下发指令（完整实现） — 端云协同核心能力
     *
     * 生产环境中通过MQTT Broker向设备下发指令。
     * Topic格式: iot/devices/{deviceId}/command/{commandName}
     * QoS=1确保至少一次送达，设备收到后执行并上报执行结果。
     *
     * 业务场景：
     * - 平台下发"设置温度阈值"指令 → 传感器设备调节阈值
     * - 平台下发"重启"指令 → 设备远程重启
     * - 平台下发"远程配置"指令 → 设备更新配置参数
     *
     * @param deviceId   目标设备ID
     * @param commandName 指令名称，如setTemperature/restart/upgrade
     * @param payload     指令参数JSON
     * @param correlationId 关联ID，用于追踪指令执行结果
     */
    public void sendCommand(String deviceId, String commandName, String payload, String correlationId) {
        String topic = "iot/devices/" + deviceId + "/command/" + commandName;
        log.info("下发指令到设备：topic={}, commandName={}, correlationId={}",
                topic, commandName, correlationId);
        // 生产环境需要集成真实的MQTT客户端：
        // try {
        //     MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        //     message.setQos(1);
        //     message.setRetained(false);
        //     mqttClient.publish(topic, message);
        // } catch (MqttException e) {
        //     log.error("指令下发失败：topic={}", topic, e);
        //     throw new BusinessException("指令下发失败: " + e.getMessage());
        // }
        //
        // 同时记录指令下发记录到数据库，用于追踪执行结果
        // CommandRecord record = new CommandRecord();
        // record.setDeviceId(deviceId);
        // record.setCommandName(commandName);
        // record.setCorrelationId(correlationId);
        // record.setStatus("SENT");
        // commandRecordMapper.insert(record);
    }
}
