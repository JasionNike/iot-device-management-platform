package com.iot.platform.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowItem;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Sentinel 规则初始化（商用标准）
 * <p>
 * 启动时加载全部规则，无需等待首次调用才初始化。
 * 规则类型：流控 + 熔断降级 + 热点参数 + 系统保护
 * </p>
 *
 * @author 王恒
 */
@Component
public class SentinelConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SentinelConfig.class);

    /** 设备详情流控资源名 */
    public static final String RESOURCE_DEVICE_DETAIL = "deviceDetail";
    /** Feign通知发送资源名 */
    public static final String RESOURCE_NOTIFY_SEND = "notification-send";

    @Override
    public void run(ApplicationArguments args) {
        initFlowRules();
        initDegradeRules();
        initParamFlowRules();
        initSystemRules();
        log.info("Sentinel 规则初始化完成");
    }

    /**
     * 1. 流控规则 — 接口级别 QPS 限流
     * <p>
     * 场景：设备详情查询接口是高频接口，限制 QPS 防止数据库被打爆
     * </p>
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // deviceDetail 接口：QPS 超过 100 触发限流
        FlowRule deviceDetailRule = new FlowRule(RESOURCE_DEVICE_DETAIL);
        deviceDetailRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        deviceDetailRule.setCount(100);
        deviceDetailRule.setLimitApp("default");
        rules.add(deviceDetailRule);

        FlowRuleManager.loadRules(rules);
        log.info("[Sentinel] 流控规则加载完成, 规则数={}", rules.size());
    }

    /**
     * 2. 熔断降级规则 — Feign 调用通知服务异常比例过高时熔断
     * <p>
     * 场景：通知服务不可用时，1分钟内异常比例 > 50% → 熔断 30 秒
     * 熔断期间直接走 fallback，不再发起真实调用（快速失败）
     * 30 秒后半开状态探测，恢复则关闭熔断
     * </p>
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // notification-send：慢调用比例 > 50% 且 RT > 3000ms → 熔断30s
        DegradeRule notifyRule = new DegradeRule(RESOURCE_NOTIFY_SEND);
        notifyRule.setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType());
        notifyRule.setCount(3000);            // 慢调用 RT 阈值 3000ms
        notifyRule.setSlowRatioThreshold(0.5); // 慢调用比例 > 50% 触发
        notifyRule.setMinRequestAmount(5);     // 最小请求数 5（统计窗口内需 >=5 个请求）
        notifyRule.setStatIntervalMs(60000);   // 统计窗口 60s（1分钟）
        notifyRule.setTimeWindow(30);          // 熔断时长 30s
        rules.add(notifyRule);

        DegradeRuleManager.loadRules(rules);
        log.info("[Sentinel] 降级规则加载完成, 规则数={}", rules.size());
    }

    /**
     * 3. 热点参数限流 — 对热门 deviceId 单独限流
     * <p>
     * 场景：某些"明星设备"被频繁查询（比如 CEO 办公室的设备），
     * 对第 0 个参数（deviceId）单独限制 QPS 200
     * </p>
     */
    private void initParamFlowRules() {
        ParamFlowRule rule = new ParamFlowRule(RESOURCE_DEVICE_DETAIL)
            .setParamIdx(0)        // 限流第 0 个参数（deviceId）
            .setGrade(RuleConstant.FLOW_GRADE_QPS)
            .setCount(200);        // 单个设备 QPS 200

        // 特殊热点值：可以给特定 deviceId 设置更严格的限制
        // 例如热点设备 MOCK-FIN-0001 只允许 QPS 50
        ParamFlowItem hotItem = new ParamFlowItem()
            .setObject("hot-device-001")
            .setClassType(String.class.getName())
            .setCount(50);
        rule.setParamFlowItemList(Collections.singletonList(hotItem));

        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
        log.info("[Sentinel] 热点规则加载完成");
    }

    /**
     * 4. 系统保护规则 — 防止系统过载
     * <p>
     * 场景：CPU 使用率超过 80% 或系统 Load 超过 CPU 核数 → 触发系统级限流
     * 这是最后一道防线，防止一个接口拖垮整个服务
     * </p>
     */
    private void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();

        // CPU 使用率 > 80% → 限流
        SystemRule cpuRule = new SystemRule();
        cpuRule.setHighestCpuUsage(0.8);
        rules.add(cpuRule);

        // 系统 Load > CPU 核数 → 限流
        SystemRule loadRule = new SystemRule();
        loadRule.setHighestSystemLoad(Runtime.getRuntime().availableProcessors() * 1.0);
        rules.add(loadRule);

        // 平均 RT > 2000ms → 限流
        SystemRule rtRule = new SystemRule();
        rtRule.setAvgRt(2000);
        rules.add(rtRule);

        SystemRuleManager.loadRules(rules);
        log.info("[Sentinel] 系统规则加载完成, 规则数={}", rules.size());
    }
}
