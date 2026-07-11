package com.iot.platform.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.common.Result;
import com.iot.platform.entity.Device;
import com.iot.platform.service.DeviceService;
import com.iot.platform.service.MonitorService;
import com.iot.platform.service.ShadowService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 设备管理控制器
 *
 * 高频接口使用 Sentinel 保护：
 * - /api/device/detail/{deviceId} — @SentinelResource 流控 + 降级
 *
 * @author 王恒
 */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);

    private final DeviceService deviceService;
    private final ShadowService shadowService;
    private final MonitorService monitorService;

    /** 设备注册 */
    @PostMapping("/register")
    public Result<Device> register(@RequestBody Map<String, Object> deviceInfo) {
        return Result.success(deviceService.manualRegister(deviceInfo));
    }

    /** 设备列表（分页+筛选） */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String productKey,
                                             @RequestParam(required = false) Integer onlineStatus,
                                             @RequestParam(required = false) String keyword) {
        Page<Device> page = deviceService.listPage(pageNum, pageSize, productKey, onlineStatus, keyword);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.success(result);
    }

    /** 设备详情 — 提示需要设备ID */
    @GetMapping("/detail")
    public Result<String> detailHelp() {
        return Result.fail("请提供设备SN，如 /api/device/detail/MOCK-SENSOR-0001");
    }

    /**
     * 设备详情（高频接口 — Sentinel 流控保护）
     */
    @GetMapping("/detail/{deviceId}")
    @SentinelResource(
        value = "deviceDetail",
        fallback = "deviceDetailFallback",
        blockHandler = "deviceDetailBlockHandler"
    )
    public Result<Device> detail(@PathVariable String deviceId) {
        Device device = deviceService.getByDeviceId(deviceId);
        if (device == null) {
            return Result.fail("设备不存在");
        }
        return Result.success(device);
    }

    /**
     * 业务异常降级方法
     * <p>
     * 触发条件：业务方法抛出异常（非 BlockException），由 Sentinel 统计异常比例，
     * 当异常比例超过阈值时熔断，后续请求直接走此降级方法。
     * </p>
     *
     * @param deviceId 设备ID
     * @param e        原始异常
     * @return 降级响应（可能返回缓存数据或降级提示）
     */
    public Result<Device> deviceDetailFallback(String deviceId, Throwable e) {
        log.error("设备详情查询降级, deviceId={}, error={}", deviceId, e.getMessage());
        // 尝试从缓存返回基础信息（不含实时遥测数据）
        try {
            Device basic = deviceService.getByDeviceId(deviceId);
            if (basic != null) {
                return Result.success("实时数据暂不可用，显示缓存数据", basic);
            }
        } catch (Exception ignored) {
            // 彻底失败
        }
        return Result.fail("设备详情查询服务暂不可用，请稍后再试");
    }

    /**
     * 流量控制降级方法
     * <p>
     * 触发条件：QPS 超过 Sentinel 流控规则配置的阈值（100 QPS），
     * 或被热点参数规则限流。
     * </p>
     *
     * @param deviceId 设备ID
     * @param e        BlockException（限流/降级/热点/系统规则触发）
     * @return 限流提示（HTTP 200 但业务失败，便于前端统一处理）
     */
    public Result<Device> deviceDetailBlockHandler(String deviceId, BlockException e) {
        log.warn("设备详情查询被限流, deviceId={}, rule={}", deviceId, e.getRule().getResource());
        return Result.fail("查询过于频繁，请稍后再试");
    }

    /** 设备影子 — 提示需要设备ID */
    @GetMapping("/shadow")
    public Result<String> shadowHelp() {
        return Result.fail("请提供设备SN，如 /api/device/shadow/MOCK-SENSOR-0001");
    }

    /** 设备影子 */
    @GetMapping("/shadow/{deviceId}")
    public Result<Map<String, Object>> shadow(@PathVariable String deviceId) {
        return Result.success(shadowService.getShadow(deviceId));
    }

    /** 设备遥测历史 — 提示需要设备ID */
    @GetMapping("/telemetry")
    public Result<String> telemetryHelp() {
        return Result.fail("请提供设备SN，如 /api/device/telemetry/MOCK-SENSOR-0001");
    }

    /** 设备遥测历史 */
    @GetMapping("/telemetry/{deviceId}")
    public Result<?> telemetry(@PathVariable String deviceId,
                                @RequestParam(required = false) String property,
                                @RequestParam(defaultValue = "30") int limit) {
        return Result.success(monitorService.getTelemetry(deviceId, property, limit));
    }

    /** 统计大盘 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(monitorService.getDashboard());
    }
}
