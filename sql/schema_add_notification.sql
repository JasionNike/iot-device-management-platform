-- ============================================================
-- 智能物联设备管理平台 - 通知补发表
-- 当通知服务不可用时（Sentinel 熔断降级），将通知请求持久化到此表
-- 由定时任务定期扫描补发，确保告警通知不丢失
-- ============================================================

CREATE TABLE IF NOT EXISTS `t_notification_retry` (
    `id`            BIGINT(20)    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `alert_id`      BIGINT(20)    DEFAULT NULL COMMENT '关联告警记录ID',
    `device_id`     VARCHAR(64)   NOT NULL COMMENT '设备ID',
    `channel`       VARCHAR(20)   NOT NULL DEFAULT 'SMS' COMMENT '通知渠道：SMS/EMAIL/APP_PUSH',
    `content`       VARCHAR(500)  NOT NULL COMMENT '通知内容',
    `recipients`    VARCHAR(255)  DEFAULT '' COMMENT '接收人（手机号/邮箱/设备Token）',
    `status`        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '补发状态：PENDING/RETRYING/SENT/FAILED',
    `retry_count`   INT(11)       NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry`     INT(11)       NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    `error_msg`     VARCHAR(500)  DEFAULT '' COMMENT '最近一次失败原因',
    `next_retry_time` DATETIME    DEFAULT NULL COMMENT '下次重试时间',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_alert_id` (`alert_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_next_retry_time` (`next_retry_time`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知补发表（Sentinel降级后持久化待补发）';
