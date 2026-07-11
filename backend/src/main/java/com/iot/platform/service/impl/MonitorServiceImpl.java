package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iot.platform.entity.AlertRecord;
import com.iot.platform.entity.Device;
import com.iot.platform.entity.DeviceTelemetry;
import com.iot.platform.mapper.AlertRecordMapper;
import com.iot.platform.mapper.DeviceMapper;
import com.iot.platform.mapper.DeviceTelemetryMapper;
import com.iot.platform.service.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行监控服务实现类
 *
 * 提供设备运行状态的全局监控能力，包括：
 * 1. 统计大盘：设备总数/在线率/类型分布/告警/固件版本
 * 2. 遥测历史：查询设备指定属性的历史数据
 *
 * 业务场景：
 * - 运维人员通过大盘Dashboard了解整体运行状况
 * - 通过遥测历史趋势分析设备运行状态异常
 * - 为告警联动和设备运维决策提供数据支撑
 *
 * @author 王恒
 */
@Service
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceTelemetryMapper deviceTelemetryMapper;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    /**
     * 获取监控仪表盘数据
     * <p>
     * 聚合展示平台核心运营指标，供前端仪表盘展示使用。
     * 
     * 返回数据结构：
     * {
     *   "totalDevices": 设备总数,
     *   "onlineDevices": 在线设备数,
     *   "offlineDevices": 离线设备数,
     *   "onlineRate": 在线率(百分比),
     *   "deviceTypeDistribution": [{product_key, count}], 按产品类型分组统计
     *   "recentAlerts": [], 最近10条告警记录
     *   "otaProgress": [{firmware_version, count}] 按固件版本分组统计
     * }
     *
     * @return 仪表盘数据Map
     */
    @Override
    public Map<String, Object> getDashboard() {
        // 创建仪表盘数据容器
        Map<String, Object> dashboard = new HashMap<>();

        // 1. 设备总数：查询设备表记录总数
        Long totalDevices = deviceMapper.selectCount(null);
        dashboard.put("totalDevices", totalDevices);

        // 2. 在线设备数：查询online_status=1的设备数量
        LambdaQueryWrapper<Device> onlineQuery = new LambdaQueryWrapper<>();
        onlineQuery.eq(Device::getOnlineStatus, 1);
        Long onlineDevices = deviceMapper.selectCount(onlineQuery);
        dashboard.put("onlineDevices", onlineDevices);

        // 3. 离线设备数：查询online_status=0的设备数量
        LambdaQueryWrapper<Device> offlineQuery = new LambdaQueryWrapper<>();
        offlineQuery.eq(Device::getOnlineStatus, 0);
        Long offlineDevices = deviceMapper.selectCount(offlineQuery);
        dashboard.put("offlineDevices", offlineDevices);

        // 4. 在线率：在线设备数/设备总数，保留两位小数
        // 避免除零异常：设备总数为0时在线率为0
        double onlineRate = totalDevices > 0 ? (double) onlineDevices / totalDevices : 0;
        dashboard.put("onlineRate", Math.round(onlineRate * 10000) / 100.0);

        // 5. 设备类型分布：按product_key分组统计设备数量
        List<Map<String, Object>> deviceTypeDistribution = deviceMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Device>()
                        .select("product_key, COUNT(*) as count")
                        .groupBy("product_key")
        );
        dashboard.put("deviceTypeDistribution", deviceTypeDistribution);

        // 6. 最近告警：查询最近10条告警记录，按创建时间倒序
        LambdaQueryWrapper<AlertRecord> alertQuery = new LambdaQueryWrapper<>();
        alertQuery.orderByDesc(AlertRecord::getCreateTime);
        alertQuery.last("LIMIT 10");
        List<AlertRecord> recentAlerts = alertRecordMapper.selectList(alertQuery);
        dashboard.put("recentAlerts", recentAlerts);

        // 7. OTA升级进度：按固件版本分组统计，用于展示版本分布
        List<Map<String, Object>> otaProgress = deviceMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Device>()
                        .select("firmware_version, COUNT(*) as count")
                        .groupBy("firmware_version")
        );
        dashboard.put("otaProgress", otaProgress);

        // 返回仪表盘聚合数据
        return dashboard;
    }

    /**
     * 查询设备遥测数据历史记录
     * <p>
     * 根据设备ID查询设备上报的遥测数据，支持按属性名过滤，返回最新的N条记录。
     * 
     * 查询逻辑：
     * 1. 按设备ID精确匹配
     * 2. 可选按属性名过滤（如temperature、humidity）
     * 3. 按创建时间降序排列（最新数据在前）
     * 4. 限制返回数量，避免大数据量查询
     *
     * @param deviceId     设备ID，必填
     * @param propertyName 属性名称，可选（为空时查询所有属性）
     * @param limit        返回记录数量限制
     * @return 遥测数据列表，按时间倒序排列
     */
    @Override
    public List<DeviceTelemetry> getTelemetry(String deviceId, String propertyName, int limit) {
        // 构建MyBatis-Plus查询条件封装器
        LambdaQueryWrapper<DeviceTelemetry> queryWrapper = new LambdaQueryWrapper<>();
        // 条件1：设备ID精确匹配
        queryWrapper.eq(DeviceTelemetry::getDeviceId, deviceId);

        // 条件2：属性名称过滤（可选）
        if (propertyName != null && !propertyName.isEmpty()) {
            queryWrapper.eq(DeviceTelemetry::getPropertyName, propertyName);
        }

        // 按创建时间降序排列（最新数据在前）
        queryWrapper.orderByDesc(DeviceTelemetry::getCreateTime);
        // 限制返回数量
        queryWrapper.last("LIMIT " + limit);

        // 执行查询并返回结果
        return deviceTelemetryMapper.selectList(queryWrapper);
    }
}
