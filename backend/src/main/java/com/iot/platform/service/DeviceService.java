package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.entity.Device;

import java.util.Map;

public interface DeviceService {
    /** 自动注册设备（MQTT首次连接时调用） */
    Device autoRegister(String deviceSn, String productKey, String firmwareVersion);

    /** 手动注册 */
    Device manualRegister(Map<String, Object> deviceInfo);

    /** 更新在线状态 */
    void updateOnlineStatus(String deviceId, boolean online);

    /** 查询设备列表 */
    Page<Device> listPage(int pageNum, int pageSize, String productKey, Integer onlineStatus, String keyword);

    /** 根据deviceId查询 */
    Device getByDeviceId(String deviceId);

    /** 心跳处理 */
    void handleHeartbeat(String deviceId);

    /** 检测离线设备 */
    void checkOfflineDevices();
}
