package com.iot.platform.protocol;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 协议适配器抽象基类
 *
 * 提供通用的报文校验逻辑和JSON解析能力，
 * 子类只需关注具体的字段映射规则。
 *
 * 子类需要实现的方法：
 * - getSupportedProductKeys: 声明支持的productKey
 * - decode: 设备原始报文 → 标准遥测数据
 * - encode: 平台指令 → 设备私有协议格式
 *
 * @author 王恒
 */
@Slf4j
public abstract class AbstractProtocolAdapter implements ProtocolAdapter {

    /** 支持的productKey集合 */
    protected final Set<String> supportedProductKeys = new HashSet<>();

    /**
     * 构造适配器并注册支持的productKey
     *
     * @param productKeys 此适配器支持的产品标识列表
     */
    public AbstractProtocolAdapter(String... productKeys) {
        for (String pk : productKeys) {
            supportedProductKeys.add(pk);
        }
    }

    /**
     * 通用的报文校验逻辑
     *
     * 1. 检查报文是否为有效的JSON
     * 2. 检查JSON是否为空
     * 3. 调用子类的doValidate进行扩展校验
     */
    @Override
    public boolean validate(String deviceId, String payload) {
        if (payload == null || payload.isEmpty()) {
            log.warn("协议适配器校验失败：空报文, deviceId={}", deviceId);
            return false;
        }
        try {
            JSONObject json = JSONUtil.parseObj(payload);
            if (json.isEmpty()) {
                log.warn("协议适配器校验失败：空JSON, deviceId={}", deviceId);
                return false;
            }
            return doValidate(deviceId, json);
        } catch (Exception e) {
            log.error("协议适配器校验异常：deviceId={}", deviceId, e);
            return false;
        }
    }

    @Override
    public Set<String> getSupportedProductKeys() {
        return supportedProductKeys;
    }

    /**
     * 子类可重写此方法扩展校验逻辑
     *
     * @param deviceId 设备ID
     * @param json 解析后的JSON对象
     * @return true=合法
     */
    protected boolean doValidate(String deviceId, JSONObject json) {
        return true;
    }

    /**
     * 将JSON对象转换为标准Map，过滤null值
     *
     * @param json 待转换的JSON对象
     * @return 非null字段的Map
     */
    protected Map<String, Object> toStandardMap(JSONObject json) {
        Map<String, Object> result = new HashMap<>();
        for (String key : json.keySet()) {
            Object value = json.get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }
}
