-- 批量MOCK设备数据（200台，每类50台）
INSERT INTO t_device (device_id, device_name, product_key, device_sn, device_secret, firmware_version, online_status, register_time, last_online_time, heartbeat_time, heartbeat_miss_count, status)
SELECT
  LOWER(CONCAT(REPLACE(UUID(), '-', ''), LPAD(t.n, 4, '0'))) AS device_id,
  CASE
    WHEN t.n <= 50 THEN CONCAT('Mock-Sensor-', LPAD(t.n, 4, '0'))
    WHEN t.n <= 100 THEN CONCAT('Mock-Energy-', LPAD(t.n-50, 4, '0'))
    WHEN t.n <= 150 THEN CONCAT('Mock-FinTerm-', LPAD(t.n-100, 4, '0'))
    ELSE CONCAT('Mock-Gateway-', LPAD(t.n-150, 4, '0'))
  END AS device_name,
  CASE
    WHEN t.n <= 50 THEN 'SENSOR-TH-001'
    WHEN t.n <= 100 THEN 'ENERGY-MTR-001'
    WHEN t.n <= 150 THEN 'FIN-ATM-001'
    ELSE 'CONSUMER-GW-001'
  END AS product_key,
  CASE
    WHEN t.n <= 50 THEN CONCAT('MOCK-SENSOR-', LPAD(t.n, 4, '0'))
    WHEN t.n <= 100 THEN CONCAT('MOCK-ENERGY-', LPAD(t.n-50, 4, '0'))
    WHEN t.n <= 150 THEN CONCAT('MOCK-FIN-', LPAD(t.n-100, 4, '0'))
    ELSE CONCAT('MOCK-GW-', LPAD(t.n-150, 4, '0'))
  END AS device_sn,
  SUBSTRING(MD5(RAND()), 1, 16) AS device_secret,
  '1.0.0' AS firmware_version,
  1 AS online_status,
  NOW() AS register_time,
  NOW() AS last_online_time,
  NOW() AS heartbeat_time,
  0 AS heartbeat_miss_count,
  'ACTIVE' AS status
FROM (
  SELECT (@row:=@row+1) AS n FROM
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) a,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) b,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) c,
  (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3) d,
  (SELECT @row:=0) r
  LIMIT 200
) t
WHERE NOT EXISTS (SELECT 1 FROM t_device WHERE device_sn LIKE 'MOCK-%');

-- 固件数据
INSERT INTO t_firmware (firmware_id, product_key, version, file_name, file_path, file_size, md5, description, upload_time, status)
SELECT 'FW-0001', 'SENSOR-TH-001', 'V2.1.0', 'SENSOR-TH_V2.1.0.bin', '/firmware/FW-0001/SENSOR-TH_V2.1.0.bin', 524288, 'a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6', '优化温湿度采集精度，修复低功耗模式bug', NOW(), 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM t_firmware);

INSERT INTO t_firmware (firmware_id, product_key, version, file_name, file_path, file_size, md5, description, upload_time, status)
SELECT 'FW-0002', 'ENERGY-MTR-001', 'V1.5.0', 'ENERGY-MTR_V1.5.0.bin', '/firmware/FW-0002/ENERGY-MTR_V1.5.0.bin', 786432, 'b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7', '新增三相不平衡检测，支持Modbus扩展', NOW(), 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM t_firmware WHERE firmware_id='FW-0002');

INSERT INTO t_firmware (firmware_id, product_key, version, file_name, file_path, file_size, md5, description, upload_time, status)
SELECT 'FW-0003', 'FIN-ATM-001', 'V3.0.1', 'FIN-ATM_V3.0.1.bin', '/firmware/FW-0003/FIN-ATM_V3.0.1.bin', 1048576, 'c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8', '安全性增强，新增国密SM4加密支持', NOW(), 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM t_firmware WHERE firmware_id='FW-0003');

INSERT INTO t_firmware (firmware_id, product_key, version, file_name, file_path, file_size, md5, description, upload_time, status)
SELECT 'FW-0004', 'CONSUMER-GW-001', 'V2.0.0', 'CONSUMER-GW_V2.0.0.bin', '/firmware/FW-0004/CONSUMER-GW_V2.0.0.bin', 2097152, 'd4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9', '支持WiFi6协议栈，新增设备漫游功能', NOW(), 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM t_firmware WHERE firmware_id='FW-0004');
