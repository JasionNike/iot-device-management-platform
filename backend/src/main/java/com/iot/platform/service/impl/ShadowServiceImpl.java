package com.iot.platform.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.common.BusinessException;
import com.iot.platform.entity.DeviceShadow;
import com.iot.platform.mapper.DeviceShadowMapper;
import com.iot.platform.service.ShadowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 设备影子服务实现 — 简历核心亮点
 *
 * 技术要点：
 * 1. Redis String 缓存影子 JSON，7天TTL + 主动刷新
 * 2. Redisson 分布式锁（设备ID粒度），保证并发写入安全
 * 3. 乐观锁 version 号，更新时 CAS 校验
 * 4. reported/desired 双状态模型 + metadata 时间戳
 *
 * 影子JSON结构：
 * {
 *   "deviceId": "xxx",
 *   "version": 15,
 *   "timestamp": 1728000000000,
 *   "state": { "reported": {...}, "desired": {...} },
 *   "metadata": { "reported": {...}, "desired": {...} }
 * }
 *
 * 业务场景说明：
 * 设备影子是物联网平台的核心机制，解决"端云状态同步"问题。
 * 当设备离线时，平台仍然可以通过影子文档"知道"设备的最新状态；
 * 当设备在线时，平台可以设置期望状态(desired)，设备主动拉取并执行。
 *
 * 设计考量：
 * - Redis作为一级缓存：影子文档被高频读取（设备状态查询），
 *   直接查MySQL会带来巨大压力，用Redis缓存大幅降低DB负载
 * - 7天TTL：物联网设备通常7天内会上报数据，冷数据自动淘汰
 * - 分布式锁：同设备并发上报时，避免版本号CAS冲突
 * - MySQL兜底：Redis宕机时从MySQL恢复，保证数据不丢
 *
 * @author 王恒
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShadowServiceImpl implements ShadowService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final DeviceShadowMapper deviceShadowMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SHADOW_KEY_PREFIX = "device:shadow:";
    private static final String LOCK_KEY_PREFIX = "device:lock:";
    private static final long SHADOW_TTL_DAYS = 7;

    @Override
    public Map<String, Object> getShadow(String deviceId) {
        // 1. 先从Redis缓存读取
        String redisKey = SHADOW_KEY_PREFIX + deviceId;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (cached != null) {
            log.debug("影子缓存命中：deviceId={}", deviceId);
            return (Map<String, Object>) cached;
        }

        // 2. Redis未命中，查MySQL
        log.info("影子缓存未命中，查询MySQL：deviceId={}", deviceId);
        DeviceShadow shadow = deviceShadowMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeviceShadow>()
                        .eq(DeviceShadow::getDeviceId, deviceId));

        if (shadow == null || StrUtil.isEmpty(shadow.getShadowJson())) {
            // 无影子记录，返回空结构
            return buildEmptyShadow(deviceId);
        }

        try {
            Map<String, Object> shadowMap = objectMapper.readValue(
                    shadow.getShadowJson(), new TypeReference<Map<String, Object>>() {});

            // 3. 回写Redis缓存
            redisTemplate.opsForValue().set(redisKey, shadowMap, SHADOW_TTL_DAYS, TimeUnit.DAYS);
            log.debug("影子数据已回写Redis缓存：deviceId={}", deviceId);

            return shadowMap;
        } catch (Exception e) {
            throw new BusinessException("影子JSON解析失败：" + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> updateReported(String deviceId, Map<String, Object> reported) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + deviceId);
        try {
            // 尝试加锁，最多等待3秒。不指定leaseTime，启用Watchdog每10秒自动续期
            // 
            // Redisson tryLock API 区别：
            // 1. lock.tryLock()                    - 立即尝试获取锁，成功返回true，失败返回false，无阻塞
            // 2. lock.tryLock(time, unit)          - 最多等待time时间尝试获取锁，期间可被中断抛出InterruptedException
            //                                        不指定leaseTime时，启用Watchdog机制自动续期（默认30秒续期一次）
            // 3. lock.tryLock(waitTime, leaseTime, unit) - 等待waitTime获取锁，获取成功后持有leaseTime时间自动释放
            //                                              不启用Watchdog，需手动unlock或等待leaseTime到期
            // 
            // 选型说明：
            // - 使用tryLock(3, SECONDS)而非lock()：避免无限等待，快速失败返回错误信息
            // - 不指定leaseTime：业务操作时间不确定，由Watchdog自动续期防止锁提前释放
            // - 捕获InterruptedException：响应线程中断信号，保证优雅退出
            
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new BusinessException("获取分布式锁失败，设备影子正在被其他操作更新");
            }

            log.info("===== 更新设备影子reported：deviceId={} =====", deviceId);

            // 1. 读取当前影子
            Map<String, Object> shadow = getShadow(deviceId);

            // 2. 更新 reported 字段和 metadata 时间戳
            Map<String, Object> state = getOrCreateMap(shadow, "state");
            state.put("reported", reported);

            Map<String, Object> metadata = getOrCreateMap(shadow, "metadata");
            Map<String, Object> reportedMeta = getOrCreateMap(metadata, "reported");
            for (String key : reported.keySet()) {
                Map<String, Object> propMeta = new HashMap<>();
                propMeta.put("timestamp", System.currentTimeMillis());
                reportedMeta.put(key, propMeta);
            }

            // 3. 递增版本号（乐观锁）
            Integer version = (Integer) shadow.getOrDefault("version", 0);
            shadow.put("version", version + 1);
            shadow.put("timestamp", System.currentTimeMillis());

            // 4. 持久化到MySQL
            saveShadowToDb(deviceId, shadow, version);

            // 5. 更新Redis缓存
            String redisKey = SHADOW_KEY_PREFIX + deviceId;
            redisTemplate.opsForValue().set(redisKey, shadow, SHADOW_TTL_DAYS, TimeUnit.DAYS);

            log.info("设备影子reported更新完成：deviceId={}, version={}", deviceId, version + 1);
            return shadow;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取分布式锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Map<String, Object> setDesired(String deviceId, Map<String, Object> desired) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + deviceId);
        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new BusinessException("获取分布式锁失败");
            }

            log.info("===== 设置设备影子desired：deviceId={} =====", deviceId);

            Map<String, Object> shadow = getShadow(deviceId);
            Map<String, Object> state = getOrCreateMap(shadow, "state");
            state.put("desired", desired);

            Map<String, Object> metadata = getOrCreateMap(shadow, "metadata");
            Map<String, Object> desiredMeta = getOrCreateMap(metadata, "desired");
            for (String key : desired.keySet()) {
                Map<String, Object> propMeta = new HashMap<>();
                propMeta.put("timestamp", System.currentTimeMillis());
                desiredMeta.put(key, propMeta);
            }

            Integer version = (Integer) shadow.getOrDefault("version", 0);
            shadow.put("version", version + 1);
            shadow.put("timestamp", System.currentTimeMillis());

            saveShadowToDb(deviceId, shadow, version);

            String redisKey = SHADOW_KEY_PREFIX + deviceId;
            redisTemplate.opsForValue().set(redisKey, shadow, SHADOW_TTL_DAYS, TimeUnit.DAYS);

            log.info("设备影子desired设置完成：deviceId={}", deviceId);
            return shadow;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取分布式锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Map<String, Object> pullAndClearDesired(String deviceId) {
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + deviceId);
        try {
            if (!lock.tryLock(3, TimeUnit.SECONDS)) {
                throw new BusinessException("获取分布式锁失败");
            }

            Map<String, Object> shadow = getShadow(deviceId);
            if (shadow == null) return Collections.emptyMap();

            // 提取 desired 并清除
            Map<String, Object> state = (Map<String, Object>) shadow.get("state");
            Map<String, Object> desired = state != null
                    ? (Map<String, Object>) state.get("desired") : null;

            if (desired != null && !desired.isEmpty()) {
                state.put("desired", new HashMap<>());
                Integer version = (Integer) shadow.getOrDefault("version", 0);
                shadow.put("version", version + 1);
                saveShadowToDb(deviceId, shadow, version);
            }

            return desired != null ? desired : Collections.emptyMap();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("获取分布式锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void deleteShadow(String deviceId) {
        String redisKey = SHADOW_KEY_PREFIX + deviceId;
        redisTemplate.delete(redisKey);

        DeviceShadow shadow = deviceShadowMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeviceShadow>()
                        .eq(DeviceShadow::getDeviceId, deviceId));
        if (shadow != null) {
            deviceShadowMapper.deleteById(shadow.getId());
        }
        log.info("设备影子已删除：deviceId={}", deviceId);
    }

    // ==================== 私有方法 ====================

    private Map<String, Object> getOrCreateMap(Map<String, Object> parent, String key) {
        Map<String, Object> child = (Map<String, Object>) parent.get(key);
        if (child == null) {
            child = new HashMap<>();
            parent.put(key, child);
        }
        return child;
    }

    private Map<String, Object> buildEmptyShadow(String deviceId) {
        Map<String, Object> shadow = new HashMap<>();
        shadow.put("deviceId", deviceId);
        shadow.put("version", 0);
        shadow.put("timestamp", System.currentTimeMillis());

        Map<String, Object> state = new HashMap<>();
        state.put("reported", new HashMap<>());
        state.put("desired", new HashMap<>());
        shadow.put("state", state);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("reported", new HashMap<>());
        metadata.put("desired", new HashMap<>());
        shadow.put("metadata", metadata);

        return shadow;
    }

    private void saveShadowToDb(String deviceId, Map<String, Object> shadow, Integer currentVersion) {
        try {
            String shadowJson = objectMapper.writeValueAsString(shadow);

            DeviceShadow exist = deviceShadowMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DeviceShadow>()
                            .eq(DeviceShadow::getDeviceId, deviceId));

            if (exist != null) {
                exist.setShadowJson(shadowJson);
                exist.setVersion((Integer) shadow.get("version"));
                exist.setUpdateTime(LocalDateTime.now());
                deviceShadowMapper.updateById(exist);
            } else {
                DeviceShadow ds = new DeviceShadow();
                ds.setDeviceId(deviceId);
                ds.setShadowJson(shadowJson);
                ds.setVersion((Integer) shadow.get("version"));
                deviceShadowMapper.insert(ds);
            }
        } catch (Exception e) {
            log.error("影子持久化MySQL失败：deviceId={}", deviceId, e);
            throw new BusinessException("影子保存失败：" + e.getMessage());
        }
    }
}
