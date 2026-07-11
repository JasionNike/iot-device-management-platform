-- ============================================
-- 智能物联设备管理平台 - 数据库初始化脚本
-- 12张业务表 + 初始数据
-- ============================================

CREATE DATABASE IF NOT EXISTS iot_platform
    DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;

USE iot_platform;

-- ============================================
-- 1. 产品型号表
-- ============================================
DROP TABLE IF EXISTS t_product;
CREATE TABLE t_product (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_key     VARCHAR(64)  NOT NULL COMMENT '产品标识（唯一）',
    product_name    VARCHAR(128) NOT NULL COMMENT '产品名称',
    manufacturer    VARCHAR(128) COMMENT '厂商名称',
    device_type     VARCHAR(30)  NOT NULL COMMENT '设备类型:SENSOR/GATEWAY/CONTROLLER/CONSUMER',
    protocol        VARCHAR(10)  DEFAULT 'MQTT' COMMENT '通信协议:MQTT/HTTP',
    description     VARCHAR(500) COMMENT '产品描述',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_key (product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品型号表';

-- ============================================
-- 2. 设备信息表（核心表）
-- ============================================
DROP TABLE IF EXISTS t_device;
CREATE TABLE t_device (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id           VARCHAR(64)  NOT NULL COMMENT '全局唯一设备ID',
    device_name         VARCHAR(128) COMMENT '设备名称',
    product_key         VARCHAR(64)  NOT NULL COMMENT '所属产品型号',
    device_sn           VARCHAR(128) COMMENT '设备出厂序列号',
    device_secret       VARCHAR(256) COMMENT '设备认证密钥',
    firmware_version    VARCHAR(32)  DEFAULT '1.0.0' COMMENT '当前固件版本',
    location            VARCHAR(256) COMMENT '部署位置/区域',
    online_status       TINYINT      DEFAULT 0 COMMENT '在线状态:0-离线,1-在线',
    register_time       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    last_online_time    DATETIME     COMMENT '最近上线时间',
    heartbeat_time      DATETIME     COMMENT '最近心跳时间',
    heartbeat_miss_count INT         DEFAULT 0 COMMENT '心跳丢失次数(>=3判离线)',
    status              VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '设备状态:ACTIVE/DISABLED/DELETED',
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_id (device_id),
    KEY idx_product_key (product_key),
    KEY idx_online_status (online_status),
    KEY idx_register_time (register_time),
    KEY idx_status_product (status, product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

-- ============================================
-- 3. 设备影子表（核心表 - JSON存储）
-- ============================================
DROP TABLE IF EXISTS t_device_shadow;
CREATE TABLE t_device_shadow (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id   VARCHAR(64)  NOT NULL COMMENT '设备ID',
    shadow_json TEXT         COMMENT '影子JSON完整文档',
    version     INT          DEFAULT 0 COMMENT '乐观锁版本号',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_id (device_id),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备影子表(Redis为主,MySQL为持久化兜底)';

-- ============================================
-- 4. 固件包表
-- ============================================
DROP TABLE IF EXISTS t_firmware;
CREATE TABLE t_firmware (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    firmware_id VARCHAR(64)  NOT NULL COMMENT '固件唯一ID',
    product_key VARCHAR(64)  NOT NULL COMMENT '适用产品型号',
    version     VARCHAR(32)  NOT NULL COMMENT '固件版本号',
    file_name   VARCHAR(256) NOT NULL COMMENT '文件名',
    file_path   VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size   BIGINT       COMMENT '文件大小(字节)',
    md5         VARCHAR(64)  NOT NULL COMMENT 'MD5校验值',
    description VARCHAR(1000) COMMENT '版本说明',
    upload_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    status      VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态:ACTIVE/ARCHIVED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_firmware_id (firmware_id),
    UNIQUE KEY uk_product_version (product_key, version),
    KEY idx_product_key (product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固件包表';

-- ============================================
-- 5. OTA升级任务表
-- ============================================
DROP TABLE IF EXISTS t_ota_task;
CREATE TABLE t_ota_task (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id         VARCHAR(64)  NOT NULL COMMENT '任务唯一ID',
    task_name       VARCHAR(256) NOT NULL COMMENT '任务名称',
    firmware_id     VARCHAR(64)  NOT NULL COMMENT '目标固件ID',
    product_key     VARCHAR(64)  NOT NULL COMMENT '目标产品',
    target_type     VARCHAR(20)  DEFAULT 'ALL' COMMENT '目标类型:ALL/GROUP/DEVICE_LIST',
    target_value    VARCHAR(2000) COMMENT '目标值(分组名/设备ID列表JSON)',
    gray_percent    INT          DEFAULT 100 COMMENT '灰度百分比',
    current_percent INT          DEFAULT 0 COMMENT '当前灰度进度百分比',
    task_status     VARCHAR(20)  DEFAULT 'CREATED' COMMENT '任务状态:CREATED/RUNNING/PAUSED/COMPLETED/CANCELLED',
    time_window_start DATETIME   COMMENT '升级窗口开始时间',
    time_window_end   DATETIME   COMMENT '升级窗口结束时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_product_key (product_key),
    KEY idx_task_status (task_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTA升级任务表';

-- ============================================
-- 6. 设备OTA升级记录表
-- ============================================
DROP TABLE IF EXISTS t_ota_record;
CREATE TABLE t_ota_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id       VARCHAR(64)  NOT NULL COMMENT '设备ID',
    task_id         VARCHAR(64)  NOT NULL COMMENT 'OTA任务ID',
    from_version    VARCHAR(32)  COMMENT '升级前版本',
    to_version      VARCHAR(32)  COMMENT '升级目标版本',
    ota_status      VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态:PENDING/DOWNLOADING/DOWNLOADED/INSTALLING/INSTALLED/REBOOTING/SUCCESS/FAILED',
    progress        INT          DEFAULT 0 COMMENT '升级进度百分比',
    retry_count     INT          DEFAULT 0 COMMENT '重试次数',
    fail_reason     VARCHAR(500) COMMENT '失败原因',
    start_time      DATETIME     COMMENT '开始时间',
    end_time        DATETIME     COMMENT '结束时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_device_id (device_id),
    KEY idx_task_id (task_id),
    KEY idx_ota_status (ota_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备OTA升级记录表';

-- ============================================
-- 7. 告警规则表
-- ============================================
DROP TABLE IF EXISTS t_alert_rule;
CREATE TABLE t_alert_rule (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_name       VARCHAR(128) NOT NULL COMMENT '规则名称',
    rule_type       VARCHAR(30)  NOT NULL COMMENT '规则类型:THRESHOLD/STATUS/EVENT',
    product_key     VARCHAR(64)  COMMENT '适用产品(空=全部产品)',
    condition_json  VARCHAR(1000) NOT NULL COMMENT '条件JSON:{property:temperature,operator:>,threshold:60}',
    alert_level     VARCHAR(5)   DEFAULT 'P2' COMMENT '告警等级:P0/P1/P2/P3',
    converge_window INT          DEFAULT 10 COMMENT '收敛窗口(分钟)',
    is_enabled      TINYINT      DEFAULT 1 COMMENT '是否启用:1-启用,0-禁用',
    description     VARCHAR(500) COMMENT '规则描述',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_rule_type (rule_type),
    KEY idx_product_key (product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- ============================================
-- 8. 告警记录表
-- ============================================
DROP TABLE IF EXISTS t_alert_record;
CREATE TABLE t_alert_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    alert_id        VARCHAR(64)  NOT NULL COMMENT '告警唯一ID',
    device_id       VARCHAR(64)  NOT NULL COMMENT '设备ID',
    rule_id         BIGINT       COMMENT '关联规则ID',
    alert_level     VARCHAR(5)   NOT NULL COMMENT '告警等级:P0/P1/P2/P3',
    alert_type      VARCHAR(30)  NOT NULL COMMENT '告警类型',
    alert_content   VARCHAR(1000) NOT NULL COMMENT '告警内容',
    trigger_value   VARCHAR(256) COMMENT '触发值',
    handle_status   VARCHAR(20)  DEFAULT 'PENDING' COMMENT '处理状态:PENDING/CONFIRMED/PROCESSING/RESOLVED/CLOSED',
    handler_id      VARCHAR(64)  COMMENT '处理人ID',
    handler_name    VARCHAR(50)  COMMENT '处理人姓名',
    handle_comment  VARCHAR(500) COMMENT '处理备注',
    handle_time     DATETIME     COMMENT '处理时间',
    alert_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_alert_id (alert_id),
    KEY idx_device_id (device_id),
    KEY idx_handle_status (handle_status),
    KEY idx_alert_time (alert_time),
    KEY idx_device_alert_time (device_id, alert_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- ============================================
-- 9. 设备事件表（MQTT上报的事件记录）
-- ============================================
DROP TABLE IF EXISTS t_device_event;
CREATE TABLE t_device_event (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id       VARCHAR(64)  NOT NULL COMMENT '设备ID',
    event_type      VARCHAR(30)  NOT NULL COMMENT '事件类型:ALARM/INFO/ERROR/STATUS_CHANGE',
    event_data      VARCHAR(2000) COMMENT '事件数据JSON',
    event_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '事件时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_device_id_time (device_id, event_time),
    KEY idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备事件表';

-- ============================================
-- 10. 设备遥测数据表
-- ============================================
DROP TABLE IF EXISTS t_device_telemetry;
CREATE TABLE t_device_telemetry (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    device_id       VARCHAR(64)  NOT NULL COMMENT '设备ID',
    property_name   VARCHAR(64)  NOT NULL COMMENT '属性名称',
    property_value  VARCHAR(256) NOT NULL COMMENT '属性值',
    data_type       VARCHAR(20)  DEFAULT 'STRING' COMMENT '数据类型:NUMBER/STRING/BOOLEAN',
    report_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上报时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_device_property_time (device_id, property_name, report_time),
    KEY idx_report_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备遥测数据表(演示用单表,生产按天分表)';

-- ============================================
-- 11. 字典配置表（复用平安银行结构）
-- ============================================
DROP TABLE IF EXISTS t_sys_dict;
CREATE TABLE t_sys_dict (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_type       VARCHAR(50)  NOT NULL COMMENT '字典类型',
    dict_code       VARCHAR(50)  NOT NULL COMMENT '字典编码',
    dict_name       VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_value      VARCHAR(500) COMMENT '字典值',
    sort_order      INT          DEFAULT 0 COMMENT '排序',
    is_enabled      TINYINT      DEFAULT 1 COMMENT '是否启用',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_code (dict_type, dict_code),
    KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典配置表';

-- ============================================
-- 12. 操作审计日志表
-- ============================================
DROP TABLE IF EXISTS t_audit_log;
CREATE TABLE t_audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    operator_id     VARCHAR(64)  COMMENT '操作人ID（用户名）',
    operator_name   VARCHAR(50)  COMMENT '操作人姓名',
    operation       VARCHAR(256) NOT NULL COMMENT '操作类型（类名.方法名）',
    target_type     VARCHAR(30)  COMMENT '操作对象类型',
    target_id       VARCHAR(64)  COMMENT '操作对象ID',
    detail          VARCHAR(2000) COMMENT '操作详情（参数/耗时/IP/错误）',
    status          VARCHAR(20)  DEFAULT 'SUCCESS' COMMENT '执行结果：SUCCESS/FAILED',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_operator_time (operator_id, create_time),
    KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';

-- ============================================
-- 13. 通知补发表（Sentinel熔断降级后持久化待补发）
-- ============================================
DROP TABLE IF EXISTS t_notification_retry;
CREATE TABLE t_notification_retry (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    alert_id        BIGINT       DEFAULT NULL COMMENT '关联告警记录ID(t_alert_record.id)',
    device_id       VARCHAR(64)  NOT NULL COMMENT '设备ID',
    channel         VARCHAR(20)  NOT NULL DEFAULT 'SMS' COMMENT '通知渠道：SMS/EMAIL/APP_PUSH',
    content         VARCHAR(500) NOT NULL COMMENT '通知内容',
    recipients      VARCHAR(255) DEFAULT '' COMMENT '接收人（手机号/邮箱/设备Token）',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '补发状态：PENDING/SENT/FAILED',
    retry_count     INT          NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry       INT          NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    error_msg       VARCHAR(500) DEFAULT '' COMMENT '最近一次失败原因',
    next_retry_time DATETIME     DEFAULT NULL COMMENT '下次重试时间',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_alert_id (alert_id),
    INDEX idx_status_retry (status, retry_count),
    INDEX idx_next_retry_time (next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知补发表（Sentinel降级后持久化待补发）';

-- ============================================
-- 14. 产品属性定义表（能力模型—属性）
-- ============================================
DROP TABLE IF EXISTS t_product_property;
CREATE TABLE t_product_property (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_key     VARCHAR(64)  NOT NULL COMMENT '所属产品标识',
    property_name   VARCHAR(64)  NOT NULL COMMENT '属性标识名',
    property_alias  VARCHAR(128) COMMENT '属性别名',
    data_type       VARCHAR(20)  NOT NULL DEFAULT 'FLOAT' COMMENT '数据类型:INTEGER/FLOAT/STRING/BOOLEAN/ENUM',
    unit            VARCHAR(20)  COMMENT '单位',
    min_value       DOUBLE       COMMENT '最小值',
    max_value       DOUBLE       COMMENT '最大值',
    default_value   VARCHAR(64)  COMMENT '默认值',
    description     VARCHAR(500) COMMENT '描述',
    sort_order      INT          DEFAULT 0 COMMENT '排序',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_key (product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品属性定义表';

-- ============================================
-- 15. 产品事件定义表（能力模型—事件）
-- ============================================
DROP TABLE IF EXISTS t_product_event;
CREATE TABLE t_product_event (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_key     VARCHAR(64)  NOT NULL COMMENT '所属产品标识',
    event_name      VARCHAR(64)  NOT NULL COMMENT '事件标识名',
    event_alias     VARCHAR(128) COMMENT '事件别名',
    event_type      VARCHAR(20)  NOT NULL DEFAULT 'INFO' COMMENT '事件类型:INFO/WARN/ERROR/FATAL',
    output_params   VARCHAR(500) COMMENT '输出参数JSON',
    description     VARCHAR(500) COMMENT '描述',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_key (product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品事件定义表';

-- ============================================
-- 16. 产品指令定义表（能力模型—指令）
-- ============================================
DROP TABLE IF EXISTS t_product_command;
CREATE TABLE t_product_command (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_key     VARCHAR(64)  NOT NULL COMMENT '所属产品标识',
    command_name    VARCHAR(64)  NOT NULL COMMENT '指令标识名',
    command_alias   VARCHAR(128) COMMENT '指令别名',
    input_params    VARCHAR(500) COMMENT '输入参数JSON',
    async           TINYINT(1)   DEFAULT 0 COMMENT '是否异步执行',
    description     VARCHAR(500) COMMENT '描述',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_product_key (product_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品指令定义表';

-- ============================================
-- 17. 平台用户表（Spring Security + JWT认证）
-- ============================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(64)  NOT NULL COMMENT '用户名（唯一）',
    password        VARCHAR(256) NOT NULL COMMENT 'BCrypt加密密码',
    real_name       VARCHAR(64)  COMMENT '真实姓名',
    roles           VARCHAR(256) DEFAULT 'ROLE_VIEWER' COMMENT '角色（逗号分隔）',
    phone           VARCHAR(20)  COMMENT '手机号',
    email           VARCHAR(128) COMMENT '邮箱',
    status          TINYINT      DEFAULT 1 COMMENT '1=启用 0=禁用',
    last_login_time DATETIME     COMMENT '最后登录时间',
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台用户表';
