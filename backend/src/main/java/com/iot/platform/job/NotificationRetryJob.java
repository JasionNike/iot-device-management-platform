package com.iot.platform.job;

import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 通知监控与清理任务 — XXL-Job 分布式调度
 *
 * 两个核心 Job：
 * 1. notificationMonitorJob：每10分钟检查 DLQ 积压和 FAILED 记录数
 * 2. notificationCleanupJob：每天凌晨2点清理7天前的 SENT 记录
 *
 * 注意：实际重试流程由 RabbitMQ DLX 延迟队列驱动（NotificationRetryConsumer），
 *       此 Job 仅负责监控告警和数据清理，不参与重试调度。
 *
 * @author 王恒
 */
@Component
public class NotificationRetryJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int DLQ_ALERT_THRESHOLD = 10;  // DLQ积压超过10条触发告警
    private static final int FAILED_ALERT_THRESHOLD = 20; // FAILED超20条触发告警

    /**
     * 监控通知补发状态
     * Cron: 每10分钟执行一次
     */
    @XxlJob("notificationMonitorJob")
    public void monitor() {
        log.info("===== 通知补发监控开始 =====");

        try {
            // 1. 检查 PENDING 状态积压（超过30分钟未处理的）
            List<Map<String, Object>> stalePending = jdbcTemplate.queryForList(
                "SELECT COUNT(*) AS cnt FROM t_notification_retry " +
                "WHERE status = 'PENDING' AND create_time < ?",
                LocalDateTime.now().minusMinutes(30).toString());
            long staleCount = ((Number) stalePending.get(0).get("cnt")).longValue();
            if (staleCount > 0) {
                log.warn("[监控] PENDING超30分钟未处理: {} 条", staleCount);
            }

            // 2. 检查 FAILED 记录数（超过阈值需人工介入）
            List<Map<String, Object>> failed = jdbcTemplate.queryForList(
                "SELECT COUNT(*) AS cnt FROM t_notification_retry WHERE status = 'FAILED'");
            long failedCount = ((Number) failed.get(0).get("cnt")).longValue();
            if (failedCount > FAILED_ALERT_THRESHOLD) {
                log.error("[监控告警] FAILED通知超过阈值: {} > {}，需人工介入！", failedCount, FAILED_ALERT_THRESHOLD);
            }

            // 3. 统计重试成功率
            List<Map<String, Object>> total = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM t_notification_retry GROUP BY status");
            long sentCount = 0;
            for (Map<String, Object> row : total) {
                String status = (String) row.get("status");
                long cnt = ((Number) row.get("cnt")).longValue();
                if ("SENT".equals(status)) sentCount = cnt;
                log.info("[监控] 状态={}, 数量={}", status, cnt);
            }

            log.info("===== 通知补发监控完成: PENDING超时={}, FAILED={} =====", staleCount, failedCount);

        } catch (Exception e) {
            log.error("通知监控任务异常", e);
        }
    }

    /**
     * 清理过期通知记录
     * Cron: 每天凌晨2点执行，删除7天前已发送成功的记录
     */
    @XxlJob("notificationCleanupJob")
    public void cleanup() {
        log.info("===== 通知记录清理开始 =====");
        try {
            String cutoff = LocalDateTime.now().minusDays(7)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            int deleted = jdbcTemplate.update(
                "DELETE FROM t_notification_retry WHERE status = 'SENT' AND create_time < ?",
                cutoff);
            log.info("===== 通知记录清理完成: 删除{}条7天前SENT记录 =====", deleted);
        } catch (Exception e) {
            log.error("通知记录清理异常", e);
        }
    }
}
