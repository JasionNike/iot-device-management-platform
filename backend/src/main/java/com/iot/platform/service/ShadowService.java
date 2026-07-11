package com.iot.platform.service;

import java.util.Map;

/**
 * 设备影子服务 — 简历核心亮点
 *
 * 功能：
 * 1. 维护云端 JSON 影子文档（reported + desired 双状态模型）
 * 2. Redis 做第一层缓存（7天TTL + 主动刷新），MySQL 持久化兜底
 * 3. Redisson 分布式锁保证并发写入安全
 * 4. 乐观锁 version 号双重保障
 *
 * @author 王恒
 */
public interface ShadowService {

    /** 获取设备影子（优先从Redis读取） */
    Map<String, Object> getShadow(String deviceId);

    /** 更新设备上报状态 reported */
    Map<String, Object> updateReported(String deviceId, Map<String, Object> reported);

    /** 设置平台期望状态 desired（指令下发） */
    Map<String, Object> setDesired(String deviceId, Map<String, Object> desired);

    /** 设备上线后拉取 desired 并清除 */
    Map<String, Object> pullAndClearDesired(String deviceId);

    /** 删除影子 */
    void deleteShadow(String deviceId);
}
