-- 修复 OTA 任务和告警数据
USE iot_platform;

-- 清理失败数据
DELETE FROM t_ota_task;
DELETE FROM t_ota_record;
DELETE FROM t_alert_record WHERE alert_id LIKE 'ALERT-%';

-- ============================================
-- 重新插入400个OTA任务（使用序号保证唯一ID）
-- ============================================
INSERT INTO t_ota_task (task_id, task_name, firmware_id, product_key, target_type, gray_percent, current_percent, task_status, time_window_start, time_window_end, create_time, update_time)
SELECT
  CONCAT('OTA-', LPAD(t.n, 6, '0')) AS task_id,
  CONCAT('批量升级任务-', LPAD(t.n, 4, '0')) AS task_name,
  f.firmware_id,
  f.product_key,
  'ALL' AS target_type,
  ELT(MOD(t.n, 4)+1, 10, 25, 50, 100) AS gray_percent,
  ELT(MOD(t.n, 4)+1, 0, 10, 25, 50) AS current_percent,
  ELT(MOD(t.n, 5)+1, 'CREATED', 'CREATED', 'RUNNING', 'RUNNING', 'COMPLETED') AS task_status,
  DATE_SUB(NOW(), INTERVAL MOD(t.n, 30) DAY) AS time_window_start,
  DATE_ADD(NOW(), INTERVAL (MOD(t.n, 14)+1) DAY) AS time_window_end,
  DATE_SUB(NOW(), INTERVAL MOD(t.n, 60) DAY) AS create_time,
  NOW() AS update_time
FROM (
  SELECT (@row1:=@row1+1) AS n FROM
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) c,
  (SELECT @row1:=0) r
  LIMIT 400
) t
JOIN (
  SELECT f.firmware_id, f.product_key, ROW_NUMBER() OVER (ORDER BY f.product_key) AS rn
  FROM t_firmware f
) f ON MOD(t.n, 200)+1 = f.rn;

-- ============================================
-- 重新插入1000条告警记录
-- ============================================
INSERT INTO t_alert_record (alert_id, device_id, rule_id, alert_level, alert_type, alert_content, trigger_value, handle_status, handler_id, handler_name, handle_comment, handle_time, alert_time, create_time, update_time)
SELECT
  CONCAT('ALERT-', LPAD(t.n, 8, '0')) AS alert_id,
  d.device_id,
  r.id AS rule_id,
  r.alert_level,
  CASE r.rule_type
    WHEN 'THRESHOLD' THEN '阈值超限'
    WHEN 'STATUS' THEN '状态异常'
    WHEN 'EVENT' THEN '事件触发'
    ELSE '未知'
  END AS alert_type,
  CONCAT(d.device_name, ' ', r.rule_name, '告警') AS alert_content,
  CASE
    WHEN r.condition_json LIKE '%temperature%' THEN CONCAT(FLOOR(60 + RAND()*30), '℃')
    WHEN r.condition_json LIKE '%battery%' THEN CONCAT(FLOOR(RAND()*10), '%')
    WHEN r.condition_json LIKE '%power%' THEN CONCAT(FLOOR(10000 + RAND()*5000), 'W')
    WHEN r.condition_json LIKE '%cpu_usage%' THEN CONCAT(FLOOR(90 + RAND()*10), '%')
    WHEN r.condition_json LIKE '%status%' THEN 'FAULT'
    ELSE CONCAT(FLOOR(RAND()*100), '')
  END AS trigger_value,
  ELT(MOD(t.n, 5)+1, 'PENDING', 'PENDING', 'PENDING', 'CONFIRMED', 'RESOLVED') AS handle_status,
  IF(MOD(t.n, 3)=0, 'admin', NULL) AS handler_id,
  IF(MOD(t.n, 3)=0, '管理员', NULL) AS handler_name,
  IF(MOD(t.n, 3)=0, CONCAT('已处理-批次', MOD(t.n, 100)), NULL) AS handle_comment,
  IF(MOD(t.n, 3)=0, DATE_SUB(NOW(), INTERVAL MOD(t.n, 7) DAY), NULL) AS handle_time,
  DATE_SUB(NOW(), INTERVAL MOD(t.n, 30) DAY) AS alert_time,
  DATE_SUB(NOW(), INTERVAL MOD(t.n, 30) DAY) AS create_time,
  NOW() AS update_time
FROM (
  SELECT (@row2:=@row2+1) AS n FROM
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
  (SELECT 0 UNION SELECT 1) c,
  (SELECT @row2:=0) r
  LIMIT 1000
) t
JOIN (
  SELECT device_id, device_name, product_key, ROW_NUMBER() OVER (ORDER BY RAND()) AS rn
  FROM t_device WHERE device_sn LIKE 'MOCK-%' LIMIT 200
) d ON MOD(t.n, 200)+1 = d.rn
JOIN t_alert_rule r ON (r.product_key IS NULL OR r.product_key = d.product_key)
  AND r.id = (MOD(t.n, 6)+1)
WHERE r.id IS NOT NULL;

SELECT '============================================' AS '';
SELECT '修复完成！' AS '';
SELECT CONCAT('OTA任务: ', COUNT(*)) FROM t_ota_task;
SELECT CONCAT('告警记录: ', COUNT(*)) FROM t_alert_record WHERE alert_id LIKE 'ALERT-%';
