package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.entity.Device;
import com.iot.platform.mapper.DeviceMapper;
import com.iot.platform.service.DeviceService;
import com.iot.platform.service.ShadowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 设备管理服务实现类
 *
 * 负责设备的全生命周期管理，包括：
 * 1. 自动注册：设备首次MQTT连接时调用，无感接入（零配置）
 * 2. 手动注册：管理员通过平台录入设备信息
 * 3. 在线状态管理：心跳更新 + 定时离线检测
 * 4. 设备列表查询：分页 + 多条件筛选
 *
 * 业务场景：
 * - 消费电子设备出厂时烧录SN，首次上电自动注册到平台
 * - 金融终端/工业设备由运维人员在平台手动录入
 * - 定时检测心跳超时的设备，自动标记离线
 *
 * @author 王恒
 */
@Service
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private ShadowService shadowService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 自动注册设备
     *
     * 设备首次通过MQTT连接时，发送register消息触发此方法。
     * 流程：查SN是否已注册 → 生成deviceId/deviceSecret → 写数据库 → 初始化影子
     *
     * 设计考量：
     * - SN是设备的物理唯一标识（出厂条码），deviceId是平台的逻辑ID
     * - 使用UUID作为deviceId，避免SN泄露导致设备伪造
     * - 16位deviceSecret作为设备接入密钥
     * - 注册同时初始化空影子，确保影子文档始终存在
     * - 幂等性：同一SN重复调用直接返回已有记录，不报错
     *
     * @param deviceSn 设备SN（出厂唯一编码）
     * @param productKey 产品标识（如SENSOR-TH-001）
     * @param firmwareVersion 固件版本号
     * @return 设备信息（新注册或已存在的）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device autoRegister(String deviceSn, String productKey, String firmwareVersion) {
        // ★ 幂等性保障：Redis分布式锁防并发重复注册
        // 同一SN同时到达多个注册请求时，只有第一个获得锁的请求执行注册逻辑
        String idempotentKey = "device:register:" + deviceSn;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(idempotentKey, "1", 30, TimeUnit.SECONDS);

        if (locked == null || !locked) {
            // 未获得锁 → 其他请求正在注册此SN → 等待后查询已有记录
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            LambdaQueryWrapper<Device> query = new LambdaQueryWrapper<>();
            query.eq(Device::getDeviceSn, deviceSn);
            Device existing = deviceMapper.selectOne(query);
            if (existing != null) return existing;
            // 极端情况：锁已过期但记录还未写入 → 降级重试一次
        }

        try {
            // 先查是否存在该SN的设备（DB唯一约束兜底）
            LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Device::getDeviceSn, deviceSn);
            Device existing = deviceMapper.selectOne(queryWrapper);
            if (existing != null) {
                return existing; // 已注册则直接返回，不重复创建
            }

            // 生成设备ID和密钥
            String deviceId = UUID.randomUUID().toString().replace("-", "");
            String deviceSecret = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            Device device = new Device();
            device.setDeviceId(deviceId);
            device.setDeviceSn(deviceSn);
            device.setProductKey(productKey);
            device.setFirmwareVersion(firmwareVersion);
            device.setDeviceSecret(deviceSecret);
            device.setOnlineStatus(1);
            device.setRegisterTime(LocalDateTime.now());
            device.setLastOnlineTime(LocalDateTime.now());
            device.setHeartbeatTime(LocalDateTime.now());
            device.setHeartbeatMissCount(0);

            deviceMapper.insert(device);

            // 初始化空设备影子
            shadowService.getShadow(deviceId);

            return device;
        } finally {
            // 注册完成后释放幂等锁（比TTL提前释放）
            redisTemplate.delete(idempotentKey);
        }
    }

    /**
     * 手动注册设备
     *
     * 管理员通过平台界面录入设备信息（SN、产品型号、位置等）。
     * 与自动注册的区别：
     * - 手动注册的设备默认离线，等待设备上线
     * - 可录入地理位置信息（经纬度/地址）
     * - SN非必填（某些场景设备无SN）
     * - 校验SN唯一性，重复则报错
     *
     * @param deviceInfo 设备信息Map，包含deviceSn, productKey, deviceName, firmwareVersion, latitude, longitude, address
     * @return 创建设备信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Device manualRegister(Map<String, Object> deviceInfo) {
        String deviceSn = (String) deviceInfo.get("deviceSn");

        // 校验SN唯一性
        if (StringUtils.hasText(deviceSn)) {
            LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Device::getDeviceSn, deviceSn);
            Device existing = deviceMapper.selectOne(queryWrapper);
            if (existing != null) {
                throw new RuntimeException("设备SN已存在: " + deviceSn);
            }
        }

        String deviceId = UUID.randomUUID().toString().replace("-", "");
        String deviceSecret = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceSn(deviceSn);
        device.setDeviceSecret(deviceSecret);
        device.setProductKey((String) deviceInfo.get("productKey"));
        device.setFirmwareVersion((String) deviceInfo.get("firmwareVersion"));
        device.setDeviceName((String) deviceInfo.get("deviceName"));
        device.setOnlineStatus(0); // 手动注册默认离线，等待设备上线
        device.setRegisterTime(LocalDateTime.now());
        device.setHeartbeatMissCount(0);

        // 地理位置信息（用于设备分布地图展示）
        if (deviceInfo.containsKey("latitude") && deviceInfo.containsKey("longitude")) {
            device.setLatitude((Double) deviceInfo.get("latitude"));
            device.setLongitude((Double) deviceInfo.get("longitude"));
        }
        if (deviceInfo.containsKey("address")) {
            device.setAddress((String) deviceInfo.get("address"));
        }

        deviceMapper.insert(device);

        // 初始化空设备影子
        shadowService.getShadow(deviceId);

        return device;
    }

    /**
     * 更新设备在线状态
     *
     * 由MQTT连接/断开事件触发。
     * 设备上线时记录上线时间，便于统计在线时长。
     *
     * @param deviceId 设备ID
     * @param online true=上线, false=下线
     */
    @Override
    public void updateOnlineStatus(String deviceId, boolean online) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setOnlineStatus(online ? 1 : 0);
        if (online) {
            device.setLastOnlineTime(LocalDateTime.now());
        }
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getDeviceId, deviceId);
        deviceMapper.update(device, queryWrapper);
    }

    /**
     * 分页查询设备列表
     *
     * 支持按产品型号、在线状态、关键字（SN/名称/ID）筛选。
     * 使用MyBatis-Plus分页插件，自动处理LIMIT和COUNT。
     * 排序规则：按注册时间倒序（最新注册的设备在前）。
     *
     * 性能说明：product_key和online_status字段建议加组合索引，
     * 关键字like查询在数据量大时建议使用全文索引或ES。
     *
     * @param pageNum 页码，从1开始
     * @param pageSize 每页条数
     * @param productKey 产品标识（可选筛选条件）
     * @param onlineStatus 在线状态（可选筛选条件）
     * @param keyword 关键字搜索（SN/名称/ID模糊匹配）
     * @return 分页结果
     */
    @Override
    public Page<Device> listPage(int pageNum, int pageSize, String productKey, Integer onlineStatus, String keyword) {
        Page<Device> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(productKey)) {
            queryWrapper.eq(Device::getProductKey, productKey);
        }
        if (onlineStatus != null) {
            queryWrapper.eq(Device::getOnlineStatus, onlineStatus);
        }
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(Device::getDeviceSn, keyword)
                    .or().like(Device::getDeviceName, keyword)
                    .or().like(Device::getDeviceId, keyword));
        }

        queryWrapper.orderByDesc(Device::getRegisterTime);
        return deviceMapper.selectPage(page, queryWrapper);
    }

    /**
     * 根据设备ID查询设备详情
     *
     * @param deviceId 设备唯一标识
     * @return 设备信息，不存在返回null
     */
    @Override
    public Device getByDeviceId(String deviceId) {
        // 先按设备序列号(deviceSn)查询——这是MQTT/API常用的外部标识
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getDeviceSn, deviceId);
        Device device = deviceMapper.selectOne(queryWrapper);
        if (device != null) {
            return device;
        }
        // 降级：按内部UUID(deviceId)查询
        queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getDeviceId, deviceId);
        return deviceMapper.selectOne(queryWrapper);
    }

    /**
     * 处理设备心跳
     *
     * 设备定时发送心跳消息（默认30秒一次），服务端更新心跳时间并重置丢失计数。
     * 心跳机制设计：
     * - 每次心跳重置heartbeatMissCount=0
     * - 如果定时器检测到设备未按时心跳，heartbeatMissCount会递增
     * - 连续丢失3次（默认90秒无心跳），设备被判离线
     *
     * @param deviceId 设备ID
     */
    @Override
    public void handleHeartbeat(String deviceId) {
        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setHeartbeatTime(LocalDateTime.now());
        device.setHeartbeatMissCount(0);
        device.setOnlineStatus(1);

        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Device::getDeviceId, deviceId);
        deviceMapper.update(device, queryWrapper);
    }

    /**
     * 定时检测离线设备（每60秒执行一次）
     *
     * 业务逻辑：
     * 1. 查询heartbeatMissCount >= 3 且 onlineStatus = 1 的设备
     * 2. 将这些设备标记为离线（onlineStatus = 0）
     * 3. 触发设备离线告警（扩展点：TODO）
     *
     * 这个定时任务替代了"被动等待设备上报离线"的方式，
     * 是一种"主动检测"的心跳超时机制，优于TCP长连接检测
     * 的原因是设备可能在网络闪断后自动恢复，不需要频繁建连。
     */
    @Override
    @Scheduled(fixedRate = 60000)
    public void checkOfflineDevices() {
        // 查询心跳丢失次数 >= 3 的设备
        LambdaQueryWrapper<Device> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ge(Device::getHeartbeatMissCount, 3);
        queryWrapper.eq(Device::getOnlineStatus, 1);

        java.util.List<Device> offlineDevices = deviceMapper.selectList(queryWrapper);

        for (Device device : offlineDevices) {
            // 标记为离线
            Device updateTarget = new Device();
            updateTarget.setDeviceId(device.getDeviceId());
            updateTarget.setOnlineStatus(0);

            LambdaQueryWrapper<Device> updateWrapper = new LambdaQueryWrapper<>();
            updateWrapper.eq(Device::getDeviceId, device.getDeviceId());
            deviceMapper.update(updateTarget, updateWrapper);

            // TODO: 触发设备离线告警
            // alertService.processEvent(device.getDeviceId(), "DEVICE_OFFLINE", ...);
        }
    }
}
