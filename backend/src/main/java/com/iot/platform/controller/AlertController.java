package com.iot.platform.controller;

import com.iot.platform.common.Result;
import com.iot.platform.entity.AlertRecord;
import com.iot.platform.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.*;

/**
 * 告警中心控制器
 *
 * @author 王恒
 */
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;
    private final JdbcTemplate jdbcTemplate;

    /** 告警统计（等级分布 + 通知补发状态） */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();

        // 告警等级分布
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT alert_level, COUNT(*) cnt FROM t_alert_record GROUP BY alert_level");
        Map<String, Long> levelCount = new LinkedHashMap<>();
        levelCount.put("P0", 0L);
        levelCount.put("P1", 0L);
        levelCount.put("P2", 0L);
        levelCount.put("P3", 0L);
        for (Map<String, Object> row : rows) {
            String lv = (String) row.get("alert_level");
            if (lv != null && levelCount.containsKey(lv)) {
                levelCount.put(lv, ((Number) row.get("cnt")).longValue());
            }
        }
        result.put("alertLevels", levelCount);

        // 通知补发状态
        try {
            List<Map<String, Object>> retries = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) cnt FROM t_notification_retry GROUP BY status");
            long pending = 0, sent = 0, failed = 0;
            for (Map<String, Object> row : retries) {
                String st = (String) row.get("status");
                long cnt = ((Number) row.get("cnt")).longValue();
                if ("PENDING".equals(st)) pending = cnt;
                else if ("SENT".equals(st)) sent = cnt;
                else if ("FAILED".equals(st)) failed = cnt;
            }
            Map<String, Long> retryStatus = new LinkedHashMap<>();
            retryStatus.put("retrying", pending);
            retryStatus.put("sent", sent);
            retryStatus.put("failed", failed);
            result.put("notifyRetry", retryStatus);
        } catch (Exception e) {
            Map<String, Long> empty = new LinkedHashMap<>();
            empty.put("retrying", 0L); empty.put("sent", 0L); empty.put("failed", 0L);
            result.put("notifyRetry", empty);
        }

        return Result.success(result);
    }

    /** 告警列表（分页） */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) String deviceId,
                                             @RequestParam(required = false) String handleStatus) {
        Page<AlertRecord> page = alertService.listAlerts(pageNum, pageSize, deviceId, handleStatus);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.success(result);
    }

    /** 更新告警处理状态 */
    @PostMapping("/handle")
    public Result<AlertRecord> handle(@RequestBody Map<String, Object> body) {
        AlertRecord record = alertService.updateHandleStatus(
                (String) body.get("alertId"),
                (String) body.get("newStatus"),
                body.getOrDefault("handlerName", "system").toString(),
                body.getOrDefault("comment", "").toString());
        return Result.success(record);
    }
}
