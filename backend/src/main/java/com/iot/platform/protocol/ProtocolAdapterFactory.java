package com.iot.platform.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协议适配器工厂 — 多厂商协议适配的核心调度器
 *
 * 工厂根据设备的productKey自动选择合适的协议适配器进行数据解析。
 * 新增厂商协议时，只需实现ProtocolAdapter接口并注册到工厂即可，
 * 无需修改现有代码（策略模式 + 工厂模式）。
 *
 * 设计模式：策略模式 + 工厂模式
 * - ProtocolAdapter：策略接口
 * - AbstractProtocolAdapter：策略基类
 * - SensorAdapter/EnergyAdapter等：具体策略
 * - ProtocolAdapterFactory：策略工厂，按productKey自动选择策略
 *
 * 使用方式：
 *   ProtocolAdapter adapter = adapterFactory.getAdapter(productKey);
 *   Map<String, Object> data = adapter.decode(deviceId, rawPayload);
 *
 * 业务收益：
 * - 新增厂商协议只需新增Adapter类，无需修改现有代码
 * - 各厂商协议实现独立，便于测试和维护
 * - 平台层统一使用标准化数据，业务逻辑与厂商解耦
 *
 * @author 王恒
 */
@Slf4j
@Component
public class ProtocolAdapterFactory {

    /** 协议适配器注册表：productKey → adapter */
    private final Map<String, ProtocolAdapter> adapterRegistry = new ConcurrentHashMap<>();

    /** 所有可用的适配器实例（Spring自动注入） */
    private final List<ProtocolAdapter> adapters;

    public ProtocolAdapterFactory(List<ProtocolAdapter> adapters) {
        this.adapters = adapters;
    }

    /**
     * 初始化：将所有ProtocolAdapter实现注册到工厂
     *
     * 扫描所有ProtocolAdapter Bean，按productKey建立映射关系。
     * 如果多个适配器声明支持同一个productKey，后注册的会覆盖先注册的，
     * 并记录WARN日志提醒配置冲突。
     */
    @PostConstruct
    public void init() {
        for (ProtocolAdapter adapter : adapters) {
            for (String productKey : adapter.getSupportedProductKeys()) {
                ProtocolAdapter existing = adapterRegistry.put(productKey, adapter);
                if (existing != null) {
                    log.warn("协议适配器冲突，productKey={} 被覆盖：{} → {}",
                            productKey, existing.getClass().getSimpleName(),
                            adapter.getClass().getSimpleName());
                }
                log.info("协议适配器注册：productKey={}, adapter={}",
                        productKey, adapter.getClass().getSimpleName());
            }
        }
        log.info("协议适配器工厂初始化完成，共 {} 个适配器，覆盖 {} 个产品",
                adapters.size(), adapterRegistry.size());
    }

    /**
     * 根据产品标识获取对应的协议适配器
     *
     * @param productKey 产品标识（如SENSOR-TH-001）
     * @return 协议适配器实例
     */
    public ProtocolAdapter getAdapter(String productKey) {
        ProtocolAdapter adapter = adapterRegistry.get(productKey);
        if (adapter == null) {
            log.warn("未找到产品 {} 的协议适配器，使用默认适配器", productKey);
            adapter = adapterRegistry.get("DEFAULT");
        }
        return adapter;
    }

    /**
     * 判断指定产品是否有对应的协议适配器
     *
     * @param productKey 产品标识
     * @return true=有对应的适配器
     */
    public boolean hasAdapter(String productKey) {
        return adapterRegistry.containsKey(productKey);
    }

    /**
     * 注册自定义适配器（用于动态扩展）
     *
     * @param adapter 自定义适配器实例
     */
    public void registerAdapter(ProtocolAdapter adapter) {
        for (String productKey : adapter.getSupportedProductKeys()) {
            adapterRegistry.put(productKey, adapter);
            log.info("动态注册协议适配器：productKey={}, adapter={}",
                    productKey, adapter.getClass().getSimpleName());
        }
    }

    /**
     * 获取所有已注册的产品标识
     *
     * @return 不可修改的productKey集合
     */
    public Set<String> getSupportedProductKeys() {
        return Collections.unmodifiableSet(adapterRegistry.keySet());
    }
}
