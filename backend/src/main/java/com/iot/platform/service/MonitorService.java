package com.iot.platform.service;

import com.iot.platform.entity.DeviceTelemetry;

import java.util.List;
import java.util.Map;

public interface MonitorService {
    /** 获取统计大盘数据 */
    Map<String, Object> getDashboard();

    /** 获取设备遥测历史数据 */
    List<DeviceTelemetry> getTelemetry(String deviceId, String propertyName, int limit);
}
