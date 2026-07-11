package com.iot.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.common.Result;
import com.iot.platform.entity.Firmware;
import com.iot.platform.entity.OtaRecord;
import com.iot.platform.entity.OtaTask;
import com.iot.platform.mapper.FirmwareMapper;
import com.iot.platform.service.OtaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OTA升级控制器
 *
 * @author 王恒
 */
@RestController
@RequestMapping("/api/ota")
@RequiredArgsConstructor
public class OtaController {

    private final OtaService otaService;
    private final FirmwareMapper firmwareMapper;

    /** 固件列表 */
    @GetMapping("/firmwares")
    public Result<List<Firmware>> firmwares() {
        return Result.success(firmwareMapper.selectList(
            new LambdaQueryWrapper<Firmware>().orderByDesc(Firmware::getUploadTime)));
    }

    /** 上传固件（演示环境为JSON表单提交，生产环境改为Multipart+对象存储） */
    @PostMapping("/firmware")
    public Result<Firmware> addFirmware(@RequestBody Firmware firmware) {
        firmware.setFirmwareId("FW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        firmware.setFilePath("/firmware/" + firmware.getFirmwareId() + "/" + firmware.getFileName());
        firmware.setMd5(UUID.randomUUID().toString().replace("-", ""));
        firmware.setUploadTime(LocalDateTime.now());
        if (firmware.getStatus() == null) firmware.setStatus("ACTIVE");
        firmwareMapper.insert(firmware);
        return Result.success(firmware);
    }

    /** 创建升级任务 */
    @PostMapping("/task")
    public Result<OtaTask> createTask(@RequestBody OtaTask task) {
        return Result.success(otaService.createTask(task));
    }

    /** 推进灰度 */
    @PostMapping("/gray/{taskId}")
    public Result<Void> advanceGray(@PathVariable String taskId) {
        otaService.advanceGrayPercent(taskId);
        return Result.success();
    }

    /** 上报升级进度 */
    @PostMapping("/progress")
    public Result<OtaRecord> updateProgress(@RequestBody Map<String, Object> body) {
        OtaRecord record = otaService.updateProgress(
                (String) body.get("deviceId"),
                (String) body.get("taskId"),
                (String) body.get("status"),
                (Integer) body.getOrDefault("progress", 0),
                (String) body.get("failReason"));
        return Result.success(record);
    }

    /** 查询设备OTA记录 */
    @GetMapping("/records/{deviceId}")
    public Result<List<OtaRecord>> deviceRecords(@PathVariable String deviceId) {
        return Result.success(otaService.getDeviceOtaRecords(deviceId));
    }

    /** 任务列表 */
    @GetMapping("/tasks")
    public Result<List<OtaTask>> tasks() {
        return Result.success(otaService.listTasks());
    }
}
