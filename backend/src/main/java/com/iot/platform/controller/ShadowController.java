package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.entity.DeviceTelemetry;
import com.iot.platform.mapper.DeviceMapper;
import com.iot.platform.mapper.DeviceTelemetryMapper;
import com.iot.platform.service.AlertService;
import com.iot.platform.service.DeviceService;
import com.iot.platform.service.ShadowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备影子控制器
 *
 * @author 王恒
 */
@RestController
@RequestMapping("/api/shadow")
@RequiredArgsConstructor
public class ShadowController {

    private final ShadowService shadowService;
    private final DeviceTelemetryMapper deviceTelemetryMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceService deviceService;
    private final AlertService alertService;

    @GetMapping("/{deviceId}")
    public Result<Map<String, Object>> get(@PathVariable String deviceId) {
        return Result.success(shadowService.getShadow(deviceId));
    }

    @PostMapping("/desired/{deviceId}")
    public Result<Map<String, Object>> setDesired(@PathVariable String deviceId,
                                                   @RequestBody Map<String, Object> desired) {
        return Result.success(shadowService.setDesired(deviceId, desired));
    }

    /**
     * 设备上报遥测数据
     * 同时完成：更新影子 + 写遥测表 + 更新设备在线 + 触发告警检查
     */
    @PostMapping("/reported/{deviceId}")
    public Result<Map<String, Object>> updateReported(@PathVariable String deviceId,
                                                       @RequestBody Map<String, Object> reported) {
        // 1. 更新设备影子
        Map<String, Object> shadow = shadowService.updateReported(deviceId, reported);

        // 2. 写入遥测数据表
        for (Map.Entry<String, Object> entry : reported.entrySet()) {
            DeviceTelemetry telemetry = new DeviceTelemetry();
            telemetry.setDeviceId(deviceId);
            telemetry.setPropertyName(entry.getKey());
            telemetry.setPropertyValue(String.valueOf(entry.getValue()));
            telemetry.setDataType(entry.getValue() instanceof Number ? "NUMBER" : "STRING");
            telemetry.setReportTime(LocalDateTime.now());
            try { deviceTelemetryMapper.insert(telemetry); } catch (Exception ignored) {}
        }

        // 3. 更新设备心跳（保持在线状态）
        deviceService.handleHeartbeat(deviceId);

        // 4. 触发告警规则检查
        alertService.processEvent(deviceId, "telemetry", reported);

        return Result.success(shadow);
    }

    @DeleteMapping("/{deviceId}")
    public Result<Void> delete(@PathVariable String deviceId) {
        shadowService.deleteShadow(deviceId);
        return Result.success();
    }
}
