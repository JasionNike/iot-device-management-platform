-- ============================================
-- 智能物联设备管理平台 - 初始数据
-- ============================================
SET NAMES utf8mb4;
USE iot_platform;

-- 产品型号初始化
INSERT INTO t_product (product_key, product_name, manufacturer, device_type, protocol, description) VALUES
('SENSOR-TH-001', '温湿度传感器', '润和智联', 'SENSOR', 'MQTT', '工业级温湿度传感器，支持-40~125℃测量'),
('ENERGY-MTR-001', '智能能源采集器', '润和智联', 'CONTROLLER', 'MQTT', '三相电能采集终端，支持电压/电流/功率监测'),
('FIN-ATM-001', '金融自助终端', '润和金融科技', 'CONSUMER', 'HTTP', '银行自助服务终端，支持存取款/查询/转账'),
('CONSUMER-GW-001', '消费电子网关', '润和智联', 'GATEWAY', 'MQTT', '智能家居网关，支持Zigbee/WiFi设备接入');

-- 告警规则初始化
INSERT INTO t_alert_rule (rule_name, rule_type, product_key, condition_json, alert_level, converge_window, description) VALUES
('高温告警', 'THRESHOLD', 'SENSOR-TH-001', '{"property":"temperature","operator":">","threshold":60}', 'P1', 10, '温度超过60℃触发告警'),
('低电量告警', 'THRESHOLD', 'SENSOR-TH-001', '{"property":"battery","operator":"<","threshold":10}', 'P2', 10, '电量低于10%触发告警'),
('设备离线告警', 'STATUS', NULL, '{"property":"online_status","operator":"==","threshold":0,"duration":300}', 'P1', 5, '设备离线超过5分钟触发告警'),
('功率异常告警', 'THRESHOLD', 'ENERGY-MTR-001', '{"property":"power","operator":">","threshold":10000}', 'P1', 10, '功率超过10kW触发告警'),
('终端故障告警', 'EVENT', 'FIN-ATM-001', '{"property":"status","operator":"==","threshold":"FAULT"}', 'P0', 1, '自助终端上报FAULT状态立即告警'),
('网关CPU告警', 'THRESHOLD', 'CONSUMER-GW-001', '{"property":"cpu_usage","operator":">","threshold":90}', 'P2', 10, '网关CPU使用率超过90%触发告警');

-- 字典数据
INSERT INTO t_sys_dict (dict_type, dict_code, dict_name, dict_value, sort_order) VALUES
('DEVICE_TYPE', 'SENSOR', '传感器', '传感器类设备', 1),
('DEVICE_TYPE', 'GATEWAY', '网关', '网关类设备', 2),
('DEVICE_TYPE', 'CONTROLLER', '控制器', '控制器类设备', 3),
('DEVICE_TYPE', 'CONSUMER', '消费终端', '消费电子类设备', 4),
('PROTOCOL', 'MQTT', 'MQTT协议', '基于MQTT的长连接通信', 1),
('PROTOCOL', 'HTTP', 'HTTP协议', '基于HTTP的短连接通信', 2),
('OTA_STATUS', 'PENDING', '待下载', NULL, 1),
('OTA_STATUS', 'DOWNLOADING', '下载中', NULL, 2),
('OTA_STATUS', 'DOWNLOADED', '下载完成', NULL, 3),
('OTA_STATUS', 'INSTALLING', '安装中', NULL, 4),
('OTA_STATUS', 'INSTALLED', '安装完成', NULL, 5),
('OTA_STATUS', 'REBOOTING', '重启中', NULL, 6),
('OTA_STATUS', 'SUCCESS', '升级成功', NULL, 7),
('OTA_STATUS', 'FAILED', '升级失败', NULL, 8),
('ALERT_LEVEL', 'P0', '紧急', '需立即处理，影响核心业务', 1),
('ALERT_LEVEL', 'P1', '严重', '需在30分钟内处理', 2),
('ALERT_LEVEL', 'P2', '一般', '需在2小时内处理', 3),
('ALERT_LEVEL', 'P3', '提示', '可在当日内处理', 4),
('HANDLE_STATUS', 'PENDING', '待处理', NULL, 1),
('HANDLE_STATUS', 'CONFIRMED', '已确认', NULL, 2),
('HANDLE_STATUS', 'PROCESSING', '处理中', NULL, 3),
('HANDLE_STATUS', 'RESOLVED', '已解决', NULL, 4),
('HANDLE_STATUS', 'CLOSED', '已关闭', NULL, 5);
