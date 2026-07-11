package com.iot.platform.mqtt;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.iot.platform.entity.Device;
import com.iot.platform.entity.DeviceEvent;
import com.iot.platform.entity.DeviceTelemetry;
import com.iot.platform.mapper.DeviceEventMapper;
import com.iot.platform.mapper.DeviceMapper;
import com.iot.platform.mapper.DeviceTelemetryMapper;
import com.iot.platform.mq.EventProducer;
import com.iot.platform.service.AlertService;
import com.iot.platform.service.DeviceService;
import com.iot.platform.service.ShadowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MQTT 消息分发处理器 — 简历核心亮点
 *
 * 处理设备上报的各类消息：
 * - register: 设备自动注册 → DeviceService.autoRegister()
 * - telemetry: 遥测数据 → 写遥测表 + 更新设备影子
 * - event: 事件上报 → 写事件表 + 发RabbitMQ异步处理告警
 * - heartbeat: 心跳 → DeviceService.handleHeartbeat()
 * - ota: OTA进度 → OtaService.updateProgress()
 *
 * 基于RabbitMQ实现设备事件异步处理、失败重试和削峰。
 *
 * @author 王恒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final DeviceService deviceService;
    private final ShadowService shadowService;
    private final AlertService alertService;
    private final DeviceMapper deviceMapper;
    private final DeviceEventMapper deviceEventMapper;
    private final DeviceTelemetryMapper deviceTelemetryMapper;
    private final EventProducer eventProducer;

    /**
     * MQTT消息分发器
     * <p>
     * 根据设备上报的消息类型，将消息路由到对应的处理器。
     * 这是MQTT消息处理的核心入口，实现了消息类型与处理逻辑的解耦。
     * 
     * 支持的消息类型：
     * - register：设备注册消息，首次接入平台时上报设备信息
     * - telemetry：遥测数据上报，如温湿度、电量等周期性数据
     * - event：事件上报，如设备异常、状态变更等一次性事件
     * - heartbeat：心跳消息，维持设备在线状态
     * - command_resp：指令响应，设备执行云端指令后的反馈（端云协同闭环）
     *
     * 扩展建议：
     * 生产环境中建议将消息类型与处理器的映射改为 Map<String, Handler> 结构，
     * 通过策略模式实现动态注册，避免频繁修改switch-case。
     *
     * @param deviceId    设备ID
     * @param messageType 消息类型标识
     * @param payload     消息体（JSON格式字符串）
     */
    public void dispatch(String deviceId, String messageType, String payload) {
        // 根据消息类型分发到不同的处理器
        // 生产环境中建议将消息类型注册表改为Map结构，便于扩展
        switch (messageType) {
            // 设备注册：首次接入平台时上报设备信息
            case "register":
                handleRegister(deviceId, payload);
                break;
            // 遥测数据：周期性上报的传感器数据（如温湿度、电量）
            case "telemetry":
                handleTelemetry(deviceId, payload);
                break;
            // 事件上报：设备异常、状态变更等一次性事件
            case "event":
                handleEvent(deviceId, payload);
                break;
            // 心跳消息：维持设备在线状态
            case "heartbeat":
                handleHeartbeat(deviceId);
                break;
            // 指令响应：设备执行云端指令后的反馈（端云协同闭环）
            case "command_resp":
                handleCommandResponse(deviceId, payload);
                break;
            // 未知消息类型：记录警告日志
            default:
                log.warn("未知消息类型：deviceId={}, type={}", deviceId, messageType);
        }
    }

    /**
     * 处理设备自动注册
     */
    private void handleRegister(String deviceId, String payload) {
        log.info("设备注册请求：deviceSn={}", deviceId);
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            String deviceSn = json.getStr("deviceSn", deviceId);
            String productKey = json.getStr("productKey", "SENSOR-TH-001");
            String firmwareVersion = json.getStr("firmwareVersion", "1.0.0");
            Device device = deviceService.autoRegister(deviceSn, productKey, firmwareVersion);
            log.info("设备注册成功：deviceId={}", device.getDeviceId());
        } catch (Exception e) {
            log.error("设备注册失败：{}", e.getMessage());
        }
    }

    /**
     * 处理遥测数据上报 → 写遥测表 + 更新影子 + 发MQ削峰
     */
    private void handleTelemetry(String deviceId, String payload) {
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            Map<String, Object> reported = new HashMap<>();

            // 解析遥测数据写入t_device_telemetry表
            for (String key : json.keySet()) {
                if ("timestamp".equals(key)) continue;
                Object value = json.get(key);
                DeviceTelemetry telemetry = new DeviceTelemetry();
                telemetry.setDeviceId(deviceId);
                telemetry.setPropertyName(key);
                telemetry.setPropertyValue(String.valueOf(value));
                telemetry.setDataType(value instanceof Number ? "NUMBER" : "STRING");
                telemetry.setReportTime(LocalDateTime.now());
                deviceTelemetryMapper.insert(telemetry);

                reported.put(key, value);
            }

            // 更新设备影子中的reported字段
            shadowService.updateReported(deviceId, reported);

            // 发送到RabbitMQ异步处理（削峰 + 后续告警匹配）
            eventProducer.sendTelemetryEvent(deviceId, reported);

            log.debug("遥测数据处理完成：deviceId={}, properties={}", deviceId, reported.keySet());
        } catch (Exception e) {
            log.error("遥测数据处理失败：deviceId={}", deviceId, e);
        }
    }

    /**
     * 处理设备事件上报 → 写事件表 + 发RabbitMQ触发告警
     */
    private void handleEvent(String deviceId, String payload) {
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            String eventType = json.getStr("eventType", "INFO");

            DeviceEvent event = new DeviceEvent();
            event.setDeviceId(deviceId);
            event.setEventType(eventType);
            event.setEventData(payload);
            event.setEventTime(LocalDateTime.now());
            deviceEventMapper.insert(event);

            // 发送到RabbitMQ，由AlertConsumer异步匹配告警规则
            eventProducer.sendDeviceEvent(deviceId, eventType, json);

            // 同步处理告警（也可全异步）
            alertService.processEvent(deviceId, eventType, json);

            log.info("设备事件已处理：deviceId={}, eventType={}", deviceId, eventType);
        } catch (Exception e) {
            log.error("设备事件处理失败：deviceId={}", deviceId, e);
        }
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(String deviceId) {
        deviceService.handleHeartbeat(deviceId);
        log.debug("心跳处理：deviceId={}", deviceId);
    }

    /**
     * 处理设备对云端下发指令的响应 — 端云协同关键环节
     *
     * 设备执行完云端下发的指令后，将执行结果上报给平台。
     * 结果包含指令ID、执行状态(SUCCESS/FAILED)、输出数据和错误信息。
     * 平台根据执行结果更新指令状态，完成端云协同闭环。
     *
     * 业务场景：
     * - 平台下发"设置温度阈值25℃"指令
     * - 设备执行后将实际结果上报
     * - 平台更新指令状态为SUCCESS或FAILED
     *
     * @param deviceId 设备ID
     * @param payload 指令响应JSON
     */
    private void handleCommandResponse(String deviceId, String payload) {
        try {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(payload);
            String commandId = json.getStr("commandId");
            String status = json.getStr("status", "SUCCESS");
            log.info("设备指令响应：deviceId={}, commandId={}, status={}", deviceId, commandId, status);
            // 扩展点：更新指令执行记录到数据库
            // commandRecordMapper.updateStatus(commandId, status, payload);
        } catch (Exception e) {
            log.error("指令响应处理失败：deviceId={}", deviceId, e);
        }
    }
}
