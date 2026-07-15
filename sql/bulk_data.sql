-- ============================================
-- 智能物联设备管理平台 - 批量数据扩充脚本
-- 产品: 50个 | 设备: 20000台 | 固件: 200个 | OTA任务: 400个 | 告警: 1000条
-- 执行方式: mysql -u root -p123456 < sql/bulk_data.sql
-- ============================================
SET NAMES utf8mb4;
USE iot_platform;

-- ============================================
-- Part 1: 新增46个产品（保留原有4个）
-- ============================================
INSERT IGNORE INTO t_product (product_key, product_name, manufacturer, device_type, protocol, description) VALUES
-- 传感器 SENSOR (13个新产品)
('SENSOR-SMK-001', '烟感探测器', '润和智联', 'SENSOR', 'MQTT', '光电式烟雾探测器，支持远程消音和灵敏度调节'),
('SENSOR-WLK-001', '水浸探测器', '润和智联', 'SENSOR', 'MQTT', '点式水浸传感器，IP67防护，响应时间<1秒'),
('SENSOR-DOR-001', '门磁传感器', '润和智联', 'SENSOR', 'MQTT', '无线门磁开关传感器，低功耗设计，电池续航3年'),
('SENSOR-PIR-001', '红外探测器', '润和智联', 'SENSOR', 'MQTT', '被动红外人体探测器，探测距离12米，角度110°'),
('SENSOR-GAS-001', '气体探测器', '润和智联', 'SENSOR', 'MQTT', '可燃气体/有毒气体复合探测器，支持CH4/CO/H2S检测'),
('SENSOR-PRS-001', '压力传感器', '润和智联', 'SENSOR', 'MQTT', '工业压力变送器，量程0-10MPa，精度0.1%FS'),
('SENSOR-LVL-001', '液位传感器', '润和智联', 'SENSOR', 'MQTT', '超声波液位计，量程0-15米，支持RS485通信'),
('SENSOR-VIB-001', '振动传感器', '润和智联', 'SENSOR', 'MQTT', '三轴振动传感器，频率范围0.5-5000Hz，支持FFT分析'),
('SENSOR-LUX-001', '光照传感器', '润和智联', 'SENSOR', 'MQTT', '数字光照度传感器，量程0-200000Lux，I2C接口'),
('SENSOR-NSE-001', '噪声传感器', '润和智联', 'SENSOR', 'MQTT', '环境噪声监测传感器，量程30-130dB，A/C计权'),
('SENSOR-PM25-001', 'PM2.5传感器', '润和智联', 'SENSOR', 'MQTT', '激光散射式PM2.5/PM10传感器，精度±10μg/m³'),
('SENSOR-CO2-001', 'CO2传感器', '润和智联', 'SENSOR', 'MQTT', 'NDIR红外CO2传感器，量程400-10000ppm，精度±50ppm'),
('SENSOR-WND-001', '风速传感器', '润和智联', 'SENSOR', 'MQTT', '超声波风速风向传感器，量程0-60m/s，分辨率0.1m/s'),

-- 网关 GATEWAY (8个新产品)
('GATEWAY-EDGE-001', '边缘计算网关', '润和智联', 'GATEWAY', 'MQTT', 'ARM Cortex-A72边缘计算网关，支持Docker容器和本地数据分析'),
('GATEWAY-IND-001', '工业协议网关', '润和智联', 'GATEWAY', 'MQTT', '多协议工业网关，支持Modbus/Profibus/OPC-UA协议转换'),
('GATEWAY-LORA-001', 'LoRa网关', '润和智联', 'GATEWAY', 'MQTT', '8通道LoRaWAN网关，覆盖半径10km，支持2000+节点接入'),
('GATEWAY-ZIGB-001', 'ZigBee网关', '润和智联', 'GATEWAY', 'MQTT', 'ZigBee 3.0协调器网关，支持ZHA/ZLL协议，可接入100+子设备'),
('GATEWAY-5G-001', '5G智能网关', '润和智联', 'GATEWAY', 'MQTT', '5G NR Sub-6GHz工业网关，下行速率2Gbps，支持网络切片'),
('GATEWAY-WF6-001', 'WiFi6网关', '润和智联', 'GATEWAY', 'MQTT', 'WiFi6 (802.11ax)接入网关，双频并发3000Mbps，OFDMA+MU-MIMO'),
('GATEWAY-BLE-001', '蓝牙Mesh网关', '润和智联', 'GATEWAY', 'MQTT', '蓝牙5.0 Mesh网关，支持SIG Mesh协议，覆盖范围300米'),
('GATEWAY-NB-001', 'NB-IoT网关', '润和智联', 'GATEWAY', 'MQTT', 'NB-IoT Cat-NB2数据采集网关，PSM/eDRX省电模式，电池续航5年'),

-- 控制器 CONTROLLER (10个新产品)
('CTRL-PLC-001', 'PLC可编程控制器', '润和智联', 'CONTROLLER', 'MQTT', '工业PLC控制器，支持梯形图/结构化文本编程，IO点数可扩展至256点'),
('CTRL-DDC-001', 'DDC楼宇控制器', '润和智联', 'CONTROLLER', 'MQTT', '楼宇自动化DDC控制器，支持BACnet协议，32个通用IO点'),
('CTRL-LGT-001', '智能灯光控制器', '润和智联', 'CONTROLLER', 'MQTT', 'DALI-2灯光控制器，支持64个镇流器分组调光和场景控制'),
('CTRL-HVAC-001', '空调控制器', '润和智联', 'CONTROLLER', 'MQTT', '暖通空调智能控制器，支持Modbus RTU，PID自适应调节'),
('CTRL-MOT-001', '电机变频器', '润和智联', 'CONTROLLER', 'MQTT', '矢量变频器，功率0.75-630kW，支持V/F和矢量控制模式'),
('CTRL-VAL-001', '阀门执行器', '润和智联', 'CONTROLLER', 'MQTT', '智能电动阀门执行器，扭矩50-3000Nm，支持Modbus/Profibus'),
('CTRL-ELV-001', '电梯控制器', '润和智联', 'CONTROLLER', 'MQTT', '电梯群控控制器，支持8台电梯联动调度，CAN总线通信'),
('CTRL-PMP-001', '水泵控制器', '润和智联', 'CONTROLLER', 'MQTT', '智能水泵变频控制器，支持恒压供水，PID自动调节'),
('CTRL-FAN-001', '风机控制器', '润和智联', 'CONTROLLER', 'MQTT', '风机智能控制器，支持温度和CO2浓度联动控制，节能率30%+'),
('CTRL-CMP-001', '压缩机控制器', '润和智联', 'CONTROLLER', 'MQTT', '螺杆压缩机控制器，支持加载/卸载/比例调节，Modbus通信'),

-- 消费终端 CONSUMER (11个新产品)
('CONS-LOCK-001', '智能门锁', '润和智联', 'CONSUMER', 'MQTT', 'NB-IoT智能门锁，支持指纹/密码/IC卡/远程开锁，电池续航12个月'),
('CONS-LAMP-001', '智能灯具', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能LED灯，1600万色RGB调色，支持语音控制和定时开关'),
('CONS-PLUG-001', '智能插座', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能插座，支持电量计量/过载保护/定时开关，最大功率2500W'),
('CONS-AC-001', '智能空调', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能空调控制器，支持温度/模式/风速远程控制和能耗统计'),
('CONS-CURT-001', '智能窗帘', '润和智联', 'CONSUMER', 'MQTT', 'ZigBee智能窗帘电机，支持百分比开合控制和光照联动场景'),
('CONS-SPK-001', '智能音箱', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能音箱，支持语音助手和多房间音乐同步播放'),
('CONS-CAM-001', '智能摄像头', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能安防摄像头，1080P/红外夜视/人形检测/云存储'),
('CONS-BELL-001', '智能门铃', '润和智联', 'CONSUMER', 'MQTT', 'WiFi可视门铃，支持PIR人体感应/远程对讲/录像回放'),
('CONS-FRIDGE-001', '智能冰箱', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能冰箱控制器，支持温度监控/食材管理/故障预警'),
('CONS-WASH-001', '智能洗衣机', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能洗衣机控制器，支持远程启动/模式选择/洗涤进度通知'),
('CONS-SWEEP-001', '智能扫地机', '润和智联', 'CONSUMER', 'MQTT', 'WiFi智能扫地机器人，支持SLAM导航/分区清扫/自动回充'),

-- 新增类型设备 (4个新产品)
('SENSOR-RAIN-001', '雨量传感器', '润和智联', 'SENSOR', 'MQTT', '翻斗式雨量计，分辨率0.2mm，支持脉冲输出和RS485'),
('GATEWAY-ETH-001', '以太网工业网关', '润和智联', 'GATEWAY', 'MQTT', '工业以太网网关，支持PROFINET/EtherNet/IP/EtherCAT协议'),
('CTRL-SOL-001', '光伏逆变器', '润和智联', 'CONTROLLER', 'MQTT', '组串式光伏逆变器，MPPT效率99.9%，支持防孤岛保护和功率调节'),
('CONS-THERM-001', '智能温控器', '润和智联', 'CONSUMER', 'MQTT', 'ZigBee智能温控器，支持地暖/空调双模式，周编程自动控制');

-- ============================================
-- Part 2: 清理旧MOCK设备 + 批量插入20000台设备
-- ============================================
DELETE FROM t_device_shadow WHERE device_id IN (SELECT device_id FROM (SELECT device_id FROM t_device WHERE device_sn LIKE 'MOCK-%') AS tmp);
DELETE FROM t_device_telemetry WHERE device_id LIKE 'MOCK-%';
DELETE FROM t_device WHERE device_sn LIKE 'MOCK-%';

INSERT INTO t_device (device_id, device_name, product_key, device_sn, device_secret, firmware_version, online_status, register_time, last_online_time, heartbeat_time, heartbeat_miss_count, status)
SELECT
  LOWER(CONCAT('dev', LPAD(t.n, 12, '0'))) AS device_id,
  CONCAT('Mock-', ELT(FLOOR((t.n-1)/400)+1,
    '温湿度传感器','烟感探测器','水浸探测器','门磁传感器','红外探测器','气体探测器','压力传感器','液位传感器','振动传感器','光照传感器',
    '噪声传感器','PM2.5传感器','CO2传感器','风速传感器','雨量传感器',
    '消费电子网关','边缘计算网关','工业协议网关','LoRa网关','ZigBee网关','5G网关','WiFi6网关','蓝牙Mesh网关','NB-IoT网关','以太网网关',
    '智能能源采集器','PLC控制器','DDC楼宇控制器','灯光控制器','空调控制器','电机变频器','阀门执行器','电梯控制器','水泵控制器','风机控制器','压缩机控制器','光伏逆变器',
    '金融自助终端','智能门锁','智能灯','智能插座','智能空调','智能窗帘','智能音箱','智能摄像头','智能门铃','智能冰箱','智能洗衣机','智能扫地机','智能温控器'
  ), '-', LPAD(ROW_NUMBER() OVER(PARTITION BY FLOOR((t.n-1)/400) ORDER BY t.n), 4, '0')) AS device_name,
  ELT(FLOOR((t.n-1)/400)+1,
    'SENSOR-TH-001','SENSOR-SMK-001','SENSOR-WLK-001','SENSOR-DOR-001','SENSOR-PIR-001','SENSOR-GAS-001','SENSOR-PRS-001','SENSOR-LVL-001','SENSOR-VIB-001','SENSOR-LUX-001',
    'SENSOR-NSE-001','SENSOR-PM25-001','SENSOR-CO2-001','SENSOR-WND-001','SENSOR-RAIN-001',
    'CONSUMER-GW-001','GATEWAY-EDGE-001','GATEWAY-IND-001','GATEWAY-LORA-001','GATEWAY-ZIGB-001','GATEWAY-5G-001','GATEWAY-WF6-001','GATEWAY-BLE-001','GATEWAY-NB-001','GATEWAY-ETH-001',
    'ENERGY-MTR-001','CTRL-PLC-001','CTRL-DDC-001','CTRL-LGT-001','CTRL-HVAC-001','CTRL-MOT-001','CTRL-VAL-001','CTRL-ELV-001','CTRL-PMP-001','CTRL-FAN-001','CTRL-CMP-001','CTRL-SOL-001',
    'FIN-ATM-001','CONS-LOCK-001','CONS-LAMP-001','CONS-PLUG-001','CONS-AC-001','CONS-CURT-001','CONS-SPK-001','CONS-CAM-001','CONS-BELL-001','CONS-FRIDGE-001','CONS-WASH-001','CONS-SWEEP-001','CONS-THERM-001'
  ) AS product_key,
  CONCAT('MOCK-', LPAD(t.n, 5, '0')) AS device_sn,
  SUBSTRING(MD5(RAND()), 1, 16) AS device_secret,
  '1.0.0' AS firmware_version,
  1 AS online_status,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*365) DAY) AS register_time,
  NOW() AS last_online_time,
  NOW() AS heartbeat_time,
  0 AS heartbeat_miss_count,
  'ACTIVE' AS status
FROM (
  SELECT (@row:=@row+1) AS n FROM
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) c,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) d,
  (SELECT 0 UNION SELECT 1) e,
  (SELECT @row:=0) r
  LIMIT 20000
) t;

SELECT CONCAT('[bulk_data] 设备插入完成: ', COUNT(*), ' 台') FROM t_device WHERE device_sn LIKE 'MOCK-%';

-- ============================================
-- Part 3: 批量插入200个固件（50产品 × 4个版本）
-- ============================================
INSERT INTO t_firmware (firmware_id, product_key, version, file_name, file_path, file_size, md5, description, upload_time, status)
SELECT
  CONCAT('FW-', UPPER(SUBSTRING(MD5(CONCAT(p.product_key, v.ver)), 1, 8))) AS firmware_id,
  p.product_key,
  CONCAT('V', v.major, '.', v.minor, '.', v.patch) AS version,
  CONCAT(REPLACE(p.product_key, '-', '_'), '_V', v.major, '.', v.minor, '.', v.patch, '.bin') AS file_name,
  CONCAT('/firmware/', CONCAT('FW-', UPPER(SUBSTRING(MD5(CONCAT(p.product_key, v.ver)), 1, 8))), '/', REPLACE(p.product_key, '-', '_'), '_V', v.major, '.', v.minor, '.', v.patch, '.bin') AS file_path,
  FLOOR(256000 + RAND() * 2000000) AS file_size,
  SUBSTRING(MD5(CONCAT(p.product_key, v.ver, RAND())), 1, 32) AS md5,
  CASE v.ver
    WHEN 1 THEN '初始版本'
    WHEN 2 THEN CONCAT('V', v.major, '.', v.minor, '.', v.patch, ' 修复已知问题，提升稳定性')
    WHEN 3 THEN CONCAT('V', v.major, '.', v.minor, '.', v.patch, ' 新增功能模块，优化性能')
    ELSE CONCAT('V', v.major, '.', v.minor, '.', v.patch, ' 安全更新，修复漏洞，增加新特性')
  END AS description,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*180) DAY) AS upload_time,
  IF(RAND() > 0.15, 'ACTIVE', 'ARCHIVED') AS status
FROM
  (SELECT product_key FROM t_product ORDER BY product_key LIMIT 50) p,
  (SELECT 1 AS ver, 1 AS major, 0 AS minor, 0 AS patch
   UNION SELECT 2, 1, 1, 0
   UNION SELECT 3, 2, 0, 0
   UNION SELECT 4, 2, 1, 0) v
WHERE NOT EXISTS (SELECT 1 FROM t_firmware f WHERE f.product_key = p.product_key AND f.version = CONCAT('V', v.major, '.', v.minor, '.', v.patch));

SELECT CONCAT('[bulk_data] 固件插入完成: ', COUNT(*), ' 个') FROM t_firmware;

-- ============================================
-- Part 4: 批量插入400个OTA升级任务
-- ============================================
INSERT INTO t_ota_task (task_id, task_name, firmware_id, product_key, target_type, target_value, gray_percent, current_percent, task_status, time_window_start, time_window_end, create_time, update_time)
SELECT
  CONCAT('OTA-', UPPER(SUBSTRING(MD5(CONCAT(f.product_key, t.n)), 1, 8))) AS task_id,
  CONCAT('升级', p.product_name, '至', f.version) AS task_name,
  f.firmware_id,
  f.product_key,
  'ALL' AS target_type,
  NULL AS target_value,
  CASE FLOOR(RAND()*4)
    WHEN 0 THEN 10 WHEN 1 THEN 25 WHEN 2 THEN 50 ELSE 100
  END AS gray_percent,
  CASE FLOOR(RAND()*4)
    WHEN 0 THEN 0 WHEN 1 THEN 10 WHEN 2 THEN 25 ELSE 50
  END AS current_percent,
  ELT(FLOOR(RAND()*5)+1, 'CREATED', 'CREATED', 'RUNNING', 'RUNNING', 'COMPLETED') AS task_status,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*30) DAY) AS time_window_start,
  DATE_ADD(NOW(), INTERVAL FLOOR(RAND()*14+1) DAY) AS time_window_end,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*60) DAY) AS create_time,
  NOW() AS update_time
FROM
  (SELECT firmware_id, product_key, version FROM t_firmware ORDER BY RAND() LIMIT 400) f
  INNER JOIN t_product p ON f.product_key = p.product_key,
  (SELECT (@row2:=@row2+1) AS n FROM
    (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
    (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
    (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) c,
    (SELECT @row2:=0) r
    LIMIT 400
  ) t;

SELECT CONCAT('[bulk_data] OTA任务插入完成: ', COUNT(*), ' 个') FROM t_ota_task;

-- ============================================
-- Part 5: 批量插入1000条告警记录
-- ============================================
INSERT INTO t_alert_record (alert_id, device_id, rule_id, alert_level, alert_type, alert_content, trigger_value, handle_status, handler_id, handler_name, handle_comment, handle_time, alert_time, create_time, update_time)
SELECT
  CONCAT('ALERT-', UPPER(SUBSTRING(MD5(CONCAT(d.device_id, t.n)), 1, 10))) AS alert_id,
  d.device_id,
  r.id AS rule_id,
  r.alert_level,
  CASE r.rule_type
    WHEN 'THRESHOLD' THEN '阈值超限'
    WHEN 'STATUS' THEN '状态异常'
    WHEN 'EVENT' THEN '事件触发'
    ELSE '未知'
  END AS alert_type,
  CASE r.rule_type
    WHEN 'THRESHOLD' THEN CONCAT(d.device_name, ' ', r.rule_name, '，当前值超出阈值范围')
    WHEN 'STATUS' THEN CONCAT(d.device_name, ' 状态异常：', r.rule_name)
    WHEN 'EVENT' THEN CONCAT(d.device_name, ' 上报', r.rule_name, '事件')
    ELSE CONCAT(d.device_name, ' ', r.rule_name)
  END AS alert_content,
  CASE
    WHEN r.condition_json LIKE '%temperature%' THEN CONCAT(FLOOR(60 + RAND()*30), '℃')
    WHEN r.condition_json LIKE '%battery%' THEN CONCAT(FLOOR(RAND()*10), '%')
    WHEN r.condition_json LIKE '%power%' THEN CONCAT(FLOOR(10000 + RAND()*5000), 'W')
    WHEN r.condition_json LIKE '%cpu_usage%' THEN CONCAT(FLOOR(90 + RAND()*10), '%')
    WHEN r.condition_json LIKE '%status%' THEN 'FAULT'
    ELSE NULL
  END AS trigger_value,
  ELT(FLOOR(RAND()*5)+1, 'PENDING', 'PENDING', 'PENDING', 'CONFIRMED', 'RESOLVED') AS handle_status,
  IF(FLOOR(RAND()*3)=0, 'admin', NULL) AS handler_id,
  IF(FLOOR(RAND()*3)=0, '管理员', NULL) AS handler_name,
  IF(FLOOR(RAND()*3)=0, CONCAT('已处理，', ELT(FLOOR(RAND()*3)+1, '设备已恢复正常', '已通知运维人员', '等待进一步观察')), NULL) AS handle_comment,
  IF(FLOOR(RAND()*3)=0, DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*7) DAY), NULL) AS handle_time,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*30) DAY) AS alert_time,
  DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*30) DAY) AS create_time,
  NOW() AS update_time
FROM
  (SELECT device_id, device_name, product_key FROM t_device WHERE device_sn LIKE 'MOCK-%' ORDER BY RAND() LIMIT 600) d
  INNER JOIN t_alert_rule r ON (r.product_key IS NULL OR r.product_key = d.product_key),
  (SELECT (@row3:=@row3+1) AS n FROM
    (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
    (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
    (SELECT 0 UNION SELECT 1) c,
    (SELECT @row3:=0) r
    LIMIT 1000
  ) t;

SELECT CONCAT('[bulk_data] 告警记录插入完成: ', COUNT(*), ' 条') FROM t_alert_record WHERE alert_id LIKE 'ALERT-%';

-- ============================================
-- Part 6: 为新产品批量插入能力模型（属性+事件+指令）
-- ============================================

-- 6.1 为所有产品的SENSOR类型补充属性
INSERT INTO t_product_property (product_key, property_name, property_alias, data_type, unit, min_value, max_value, default_value, description, sort_order)
SELECT p.product_key, def.prop_name, def.prop_alias, def.data_type, def.unit, def.min_val, def.max_val, def.default_val, def.desc, def.sort
FROM t_product p
CROSS JOIN (
  SELECT 'temperature' AS prop_name, '温度' AS prop_alias, 'FLOAT' AS data_type, '℃' AS unit, -40 AS min_val, 125 AS max_val, '25' AS default_val, '环境温度' AS `desc`, 1 AS sort
  UNION ALL SELECT 'humidity', '湿度', 'FLOAT', '%RH', 0, 100, '50', '环境相对湿度', 2
  UNION ALL SELECT 'battery', '电池电量', 'INTEGER', '%', 0, 100, '100', '电池剩余电量百分比', 3
  UNION ALL SELECT 'signal_strength', '信号强度', 'INTEGER', 'dBm', -120, 0, '-50', '无线信号强度', 4
  UNION ALL SELECT 'report_interval', '上报间隔', 'INTEGER', '秒', 5, 3600, '60', '数据上报间隔', 5
) def
WHERE p.device_type = 'SENSOR'
AND NOT EXISTS (SELECT 1 FROM t_product_property pp WHERE pp.product_key = p.product_key AND pp.property_name = def.prop_name);

-- 6.2 为所有产品的GATEWAY类型补充属性
INSERT INTO t_product_property (product_key, property_name, property_alias, data_type, unit, min_value, max_value, default_value, description, sort_order)
SELECT p.product_key, def.prop_name, def.prop_alias, def.data_type, def.unit, def.min_val, def.max_val, def.default_val, def.desc, def.sort
FROM t_product p
CROSS JOIN (
  SELECT 'cpu_usage' AS prop_name, 'CPU使用率' AS prop_alias, 'FLOAT' AS data_type, '%' AS unit, 0 AS min_val, 100 AS max_val, '25' AS default_val, 'CPU使用率' AS `desc`, 1 AS sort
  UNION ALL SELECT 'memory_usage', '内存使用率', 'FLOAT', '%', 0, 100, '40', '内存使用率', 2
  UNION ALL SELECT 'connected_devices', '接入设备数', 'INTEGER', '台', 0, 500, '50', '当前接入的子设备数量', 3
  UNION ALL SELECT 'uplink_traffic', '上行流量', 'FLOAT', 'Mbps', 0, 10000, '10', '上行网络流量', 4
  UNION ALL SELECT 'downlink_traffic', '下行流量', 'FLOAT', 'Mbps', 0, 10000, '25', '下行网络流量', 5
) def
WHERE p.device_type = 'GATEWAY'
AND NOT EXISTS (SELECT 1 FROM t_product_property pp WHERE pp.product_key = p.product_key AND pp.property_name = def.prop_name);

-- 6.3 为所有产品的CONTROLLER类型补充属性
INSERT INTO t_product_property (product_key, property_name, property_alias, data_type, unit, min_value, max_value, default_value, description, sort_order)
SELECT p.product_key, def.prop_name, def.prop_alias, def.data_type, def.unit, def.min_val, def.max_val, def.default_val, def.desc, def.sort
FROM t_product p
CROSS JOIN (
  SELECT 'voltage' AS prop_name, '电压' AS prop_alias, 'FLOAT' AS data_type, 'V' AS unit, 0 AS min_val, 500 AS max_val, '220' AS default_val, '输入电压' AS `desc`, 1 AS sort
  UNION ALL SELECT 'current', '电流', 'FLOAT', 'A', 0, 100, '15', '工作电流', 2
  UNION ALL SELECT 'power', '功率', 'FLOAT', 'kW', 0, 1000, '5', '实时功率', 3
  UNION ALL SELECT 'total_energy', '累计能耗', 'FLOAT', 'kWh', 0, 999999, '0', '累计能耗统计', 4
  UNION ALL SELECT 'status', '运行状态', 'STRING', '', 0, 0, 'NORMAL', '设备运行状态: NORMAL/STOP/FAULT', 5
) def
WHERE p.device_type = 'CONTROLLER'
AND NOT EXISTS (SELECT 1 FROM t_product_property pp WHERE pp.product_key = p.product_key AND pp.property_name = def.prop_name);

-- 6.4 为所有产品的CONSUMER类型补充属性
INSERT INTO t_product_property (product_key, property_name, property_alias, data_type, unit, min_value, max_value, default_value, description, sort_order)
SELECT p.product_key, def.prop_name, def.prop_alias, def.data_type, def.unit, def.min_val, def.max_val, def.default_val, def.desc, def.sort
FROM t_product p
CROSS JOIN (
  SELECT 'status' AS prop_name, '设备状态' AS prop_alias, 'STRING' AS data_type, '' AS unit, 0 AS min_val, 0 AS max_val, 'ONLINE' AS default_val, '设备运行状态' AS `desc`, 1 AS sort
  UNION ALL SELECT 'error_count', '故障次数', 'INTEGER', '次', 0, 9999, '0', '累计故障次数', 2
  UNION ALL SELECT 'network_strength', '网络信号', 'INTEGER', '级', 1, 5, '4', 'WiFi信号强度等级', 3
  UNION ALL SELECT 'cpu_usage', 'CPU使用率', 'FLOAT', '%', 0, 100, '20', 'CPU使用率', 4
  UNION ALL SELECT 'memory_usage', '内存使用率', 'FLOAT', '%', 0, 100, '35', '内存使用率', 5
) def
WHERE p.device_type = 'CONSUMER'
AND NOT EXISTS (SELECT 1 FROM t_product_property pp WHERE pp.product_key = p.product_key AND pp.property_name = def.prop_name);

-- 6.5 为新产品批量插入事件定义
INSERT INTO t_product_event (product_key, event_name, event_alias, event_type, description)
SELECT p.product_key, def.evt_name, def.evt_alias, def.evt_type, def.desc
FROM t_product p
CROSS JOIN (
  SELECT 'device_fault' AS evt_name, '设备故障' AS evt_alias, 'ERROR' AS evt_type, '设备硬件或软件故障告警' AS `desc`
  UNION ALL SELECT 'threshold_alarm', '超阈值告警', 'WARN', '监测值超过设定阈值'
  UNION ALL SELECT 'status_change', '状态变更', 'INFO', '设备运行状态发生变化'
) def
WHERE NOT EXISTS (SELECT 1 FROM t_product_event pe WHERE pe.product_key = p.product_key AND pe.event_name = def.evt_name);

-- 6.6 为新产品批量插入指令定义
INSERT INTO t_product_command (product_key, command_name, command_alias, async, description)
SELECT p.product_key, def.cmd_name, def.cmd_alias, def.async, def.desc
FROM t_product p
CROSS JOIN (
  SELECT 'reboot' AS cmd_name, '重启设备' AS cmd_alias, 1 AS async, '远程重启设备' AS `desc`
  UNION ALL SELECT 'reset', '恢复出厂设置', 1, '将设备恢复至出厂默认配置'
) def
WHERE NOT EXISTS (SELECT 1 FROM t_product_command pc WHERE pc.product_key = p.product_key AND pc.command_name = def.cmd_name);

-- ============================================
-- 完成
-- ============================================
SELECT '============================================' AS '';
SELECT '  数据批量扩充完成！' AS '';
SELECT '  产品: 50个 | 设备: 20000台' AS '';
SELECT '  固件: 200个 | OTA任务: 400个' AS '';
SELECT '  告警: 1000条 | 能力模型已补齐' AS '';
SELECT '============================================' AS '';
