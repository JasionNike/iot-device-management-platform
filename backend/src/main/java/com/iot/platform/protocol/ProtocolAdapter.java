package com.iot.platform.protocol;

import java.util.Map;
import java.util.Set;

/**
 * 协议适配器接口 — 多厂商设备统一接入的核心抽象
 *
 * 不同厂商的物联网设备使用不同的数据格式和通信协议。
 * 本接口定义了统一的协议适配抽象，各厂商设备实现各自的适配器，
 * 将私有协议数据转换为平台标准格式。
 *
 * 设计模式：策略模式
 * - decode(): 设备→平台，私有协议转标准数据
 * - encode(): 平台→设备，标准指令转私有协议
 * - validate(): 校验设备上报数据的合法性和完整性
 *
 * 业务场景：
 * - 传感器厂商A上报temperature，厂商B上报temp → 适配器统一转为temperature
 * - 平台下发setThreshold指令 → 适配器转为设备私有协议的格式
 *
 * @author 王恒
 */
public interface ProtocolAdapter {

    /**
     * 将设备原始报文解码为标准遥测数据
     *
     * @param deviceId   设备ID
     * @param rawPayload 设备原始报文
     * @return 标准化的遥测数据Map，key=属性名，value=属性值
     */
    Map<String, Object> decode(String deviceId, String rawPayload);

    /**
     * 将平台指令编码为设备私有协议格式
     *
     * @param deviceId  设备ID
     * @param command   指令名称
     * @param params    指令参数
     * @return 编码后的设备协议报文
     */
    String encode(String deviceId, String command, Map<String, Object> params);

    /**
     * 校验设备上报数据的合法性和完整性
     *
     * @param deviceId  设备ID
     * @param payload   原始报文
     * @return true=合法, false=非法
     */
    boolean validate(String deviceId, String payload);

    /**
     * 获取此适配器支持的产品标识列表
     *
     * @return 支持的productKey集合
     */
    Set<String> getSupportedProductKeys();
}
