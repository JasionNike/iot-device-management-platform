package com.iot.platform.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.entity.AlertRecord;
import com.iot.platform.entity.AlertRule;
import com.iot.platform.feign.NotificationClient;
import com.iot.platform.mapper.AlertRecordMapper;
import com.iot.platform.mapper.AlertRuleMapper;
import com.iot.platform.service.AlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 告警中心服务实现类
 *
 * 负责设备事件的告警规则匹配、告警记录生成、告警收敛和超时升级。
 *
 * 核心业务流程：
 * 1. 设备上报事件 → 遍历启用规则 → 条件匹配 → 收敛检查 → 生成告警记录
 * 2. 告警处理：运维人员查看告警列表 → 确认/忽略/修复
 * 3. 告警升级：P2级别告警超时30分钟未处理 → 自动升级为P1
 *
 * 设计亮点：
 * - 规则引擎轻量化：通过condition_json灵活配置告警条件，无需硬编码规则
 * - 告警收敛：30分钟窗口内同设备+同规则不重复创建，防止告警风暴
 * - 超时升级：P2→P1自动升级，确保关键问题不被遗漏
 * - 双通道通知：MQ异步广播 + Feign调用通知服务发送短信/邮件
 * - 异步解耦：通知发送不阻塞告警主流程（CompletableFuture异步）
 *
 * @author 王恒
 */
@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    @Autowired
    private AlertRuleMapper alertRuleMapper;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    /** Feign 通知客户端 — 通过 Nacos 发现调用通知服务，required=false 保证无通知服务时也能启动 */
    @Autowired(required = false)
    private NotificationClient notificationClient;

    private static final long CONVERGE_WINDOW_MINUTES = 30;

    /**
     * 处理设备事件，匹配告警规则并生成告警记录
     * <p>
     * 核心处理流程：
     * 1. 查询所有启用的告警规则
     * 2. 遍历规则，通过condition_json匹配事件数据
     * 3. 告警收敛检查：同一设备+同一规则在收敛窗口内（默认30分钟）不重复创建
     * 4. 匹配成功且通过收敛检查 → 生成告警记录并持久化
     * 5. 双通道通知：RabbitMQ异步广播 + Feign调用通知服务发送短信/邮件/App推送
     *
     * @param deviceId  设备ID
     * @param eventType 事件类型（当前未用于规则匹配，保留扩展）
     * @param eventData 事件数据Map，包含设备上报的属性值等信息
     * @return 生成的告警记录，未匹配到规则或被收敛时返回null
     */
    @Override
    // 事务注解：异常回滚，确保告警记录和通知的一致性
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord processEvent(String deviceId, String eventType, Map<String, Object> eventData) {
        // 1. 查询所有启用的告警规则（通过condition_json匹配，不限制eventType）
        // 构建MyBatis-Plus查询条件封装器
        LambdaQueryWrapper<AlertRule> ruleQuery = new LambdaQueryWrapper<>();
        // 设置查询条件：只查询启用状态的规则（enabled=1）
        ruleQuery.eq(AlertRule::getEnabled, 1);
        // 执行查询，获取启用的告警规则列表
        List<AlertRule> rules = alertRuleMapper.selectList(ruleQuery);

        // 如果没有启用的告警规则，直接返回null
        if (rules.isEmpty()) {
            return null;
        }

        // 2. 遍历所有启用的告警规则
        for (AlertRule rule : rules) {
            // 根据规则的condition_json匹配事件数据
            // 不匹配则跳过当前规则，继续下一个
            if (!matchCondition(rule.getConditionJson(), eventData)) {
                continue;
            }

            // 3. 告警收敛检查：同一设备+同一规则在收敛窗口内不重复创建
            // 计算收敛窗口开始时间（当前时间 - 收敛窗口分钟数）
            LocalDateTime convergeStart = LocalDateTime.now().minusMinutes(CONVERGE_WINDOW_MINUTES);
            // 构建收敛查询条件封装器
            LambdaQueryWrapper<AlertRecord> convergeQuery = new LambdaQueryWrapper<>();
            // 条件1：设备ID匹配
            convergeQuery.eq(AlertRecord::getDeviceId, deviceId);
            // 条件2：规则ID匹配
            convergeQuery.eq(AlertRecord::getRuleId, rule.getId());
            // 条件3：创建时间在收敛窗口内
            convergeQuery.ge(AlertRecord::getCreateTime, convergeStart);
            // 查询收敛窗口内已存在的告警记录数量
            Long count = alertRecordMapper.selectCount(convergeQuery);

            // 如果已存在告警记录，跳过当前规则（告警收敛生效）
            if (count != null && count > 0) {
                continue;
            }

            // 4. 生成告警记录
            // 创建告警记录实体
            AlertRecord record = new AlertRecord();
            // 设置告警ID：UUID去除横杠，确保唯一性
            record.setAlertId(UUID.randomUUID().toString().replace("-", ""));
            // 设置设备ID
            record.setDeviceId(deviceId);
            // 设置告警级别（从规则中获取：P1/P2/P3）
            record.setAlertLevel(rule.getAlertLevel());
            // 设置告警类型（从规则中获取）
            record.setAlertType(rule.getRuleType());
            // 设置告警内容：规则名称 + 事件数据JSON
            record.setAlertContent(rule.getRuleName() + " - " + JSONUtil.toJsonStr(eventData));
            // 设置触发值：从事件数据中提取规则关注的属性值
            record.setTriggerValue(String.valueOf(eventData.get(
                JSONUtil.parseObj(rule.getConditionJson()).getStr("property"))));
            // 设置处理状态：PENDING（待处理）
            record.setHandleStatus("PENDING");
            // 设置告警触发时间
            record.setAlertTime(LocalDateTime.now());
            // 设置记录创建时间
            record.setCreateTime(LocalDateTime.now());

            // 持久化告警记录到MySQL
            alertRecordMapper.insert(record);

            // 5. 发送RabbitMQ通知（异步广播，用于系统间联动）
            // 检查RabbitMQ模板是否配置（required=false，允许未配置）
            if (rabbitTemplate != null) {
                try {
                    // 构建通知消息体
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("alertId", record.getAlertId());
                    notification.put("deviceId", deviceId);
                    notification.put("alertLevel", rule.getAlertLevel());
                    notification.put("alertContent", rule.getRuleName());
                    notification.put("createTime", record.getCreateTime().toString());
                    // 发送到指定交换机和路由键
                    rabbitTemplate.convertAndSend("alert.exchange", "alert.routing.key", notification);
                } catch (Exception e) {
                    // MQ发送失败仅记录日志，不影响主流程
                    log.warn("RabbitMQ通知发送失败, alertId={}, error={}", record.getAlertId(), e.getMessage());
                }
            }

            // 6. 发送Feign通知（短信/邮件/App推送，异步不阻塞）
            // @Async注解标记异步执行，不阻塞告警主流程
            sendNotification(record);

            // 返回生成的告警记录（匹配到第一条规则即返回，一条事件只触发一条告警）
            return record;
        }

        // 遍历完所有规则都未匹配，返回null
        return null;
    }

    /**
     * 通过 Feign 调用通知服务发送告警通知
     *
     * 使用 CompletableFuture 异步执行，不阻塞告警主流程。
     * 如果通知服务不可用，Sentinel 会自动熔断降级 → NotificationClientFallback
     * 将通知记录到 t_notification_retry 待补发表。
     */
    @Async
    public void sendNotification(AlertRecord alert) {
        if (notificationClient == null) {
            log.debug("通知服务未配置，跳过发送, alertId={}", alert.getAlertId());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> request = new HashMap<>();
                request.put("alertId", alert.getId() != null ? alert.getId() : 0L);
                request.put("deviceId", alert.getDeviceId());
                request.put("channel", "SMS");
                request.put("content", String.format("[IoT告警] 设备%s 触发%s级别告警: %s",
                    alert.getDeviceId(), alert.getAlertLevel(), alert.getAlertContent()));
                request.put("recipients", "");

                notificationClient.send(request);
                log.info("告警通知已发送, alertId={}", alert.getAlertId());
            } catch (Exception e) {
                // FallbackFactory 已自动将通知记录到 t_notification_retry
                log.warn("通知发送失败（已进入降级处理）, alertId={}, error={}",
                    alert.getAlertId(), e.getMessage());
            }
        });
    }

    @Override
    public Page<AlertRecord> listAlerts(int pageNum, int pageSize, String deviceId, String handleStatus) {
        LambdaQueryWrapper<AlertRecord> queryWrapper = new LambdaQueryWrapper<>();

        if (deviceId != null && !deviceId.isEmpty()) {
            queryWrapper.eq(AlertRecord::getDeviceId, deviceId);
        }
        if (handleStatus != null && !handleStatus.isEmpty()) {
            queryWrapper.eq(AlertRecord::getHandleStatus, handleStatus);
        }

        queryWrapper.orderByDesc(AlertRecord::getCreateTime);

        Page<AlertRecord> page = new Page<>(pageNum, pageSize);
        return alertRecordMapper.selectPage(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord updateHandleStatus(String alertId, String newStatus, String handlerName, String comment) {
        LambdaUpdateWrapper<AlertRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AlertRecord::getAlertId, alertId);
        updateWrapper.set(AlertRecord::getHandleStatus, newStatus);
        updateWrapper.set(AlertRecord::getHandlerName, handlerName);
        updateWrapper.set(AlertRecord::getHandleComment, comment);
        updateWrapper.set(AlertRecord::getHandleTime, LocalDateTime.now());
        alertRecordMapper.update(null, updateWrapper);

        LambdaQueryWrapper<AlertRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AlertRecord::getAlertId, alertId);
        return alertRecordMapper.selectOne(queryWrapper);
    }

    @Override
    @Scheduled(fixedRate = 300000)
    public void checkAlertEscalation() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        LambdaQueryWrapper<AlertRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AlertRecord::getAlertLevel, "P2");
        queryWrapper.eq(AlertRecord::getHandleStatus, "PENDING");
        queryWrapper.le(AlertRecord::getCreateTime, threshold);

        List<AlertRecord> pendingAlerts = alertRecordMapper.selectList(queryWrapper);

        for (AlertRecord alert : pendingAlerts) {
            LambdaUpdateWrapper<AlertRecord> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(AlertRecord::getAlertId, alert.getAlertId());
            updateWrapper.set(AlertRecord::getAlertLevel, "P1");
            updateWrapper.set(AlertRecord::getHandleComment, "超时未处理，自动升级");
            alertRecordMapper.update(null, updateWrapper);

            // 升级后重新发送通知
            sendNotification(alert);
        }
    }

    /**
     * 根据规则条件匹配事件数据
     * <p>
     * 条件JSON格式: {"property":"temperature","operator":">","threshold":60}
     * 
     * @param conditionJson 规则条件JSON字符串
     * @param eventData 设备上报的事件数据Map
     * @return 条件匹配返回true，否则返回false
     */
    private boolean matchCondition(String conditionJson, Map<String, Object> eventData) {
        // 条件为空时默认匹配成功（允许创建无条件规则）
        if (conditionJson == null || conditionJson.isEmpty()) {
            return true;
        }
        try {
            // 解析条件JSON为JSONObject对象
            JSONObject cond = JSONUtil.parseObj(conditionJson);
            // 提取要匹配的属性名称（如"temperature"）
            String property = cond.getStr("property");
            // 提取比较运算符，默认"=="
            String operator = cond.getStr("operator", "==");
            // 提取阈值（期望值）
            Object threshold = cond.get("threshold");

            // 属性名或阈值为空，匹配失败
            if (property == null || threshold == null) return false;

            // 从事件数据中获取实际属性值
            Object actualValue = eventData.get(property);
            // 事件数据中不存在该属性，匹配失败
            if (actualValue == null) return false;

            // 数值类型比较：实际值和阈值都是数字类型
            if (actualValue instanceof Number && threshold instanceof Number) {
                // 转换为double进行精确比较
                double actual = ((Number) actualValue).doubleValue();
                double expected = ((Number) threshold).doubleValue();
                // 根据运算符执行比较
                switch (operator) {
                    case ">":  return actual > expected;   // 大于
                    case ">=": return actual >= expected;  // 大于等于
                    case "<":  return actual < expected;   // 小于
                    case "<=": return actual <= expected;  // 小于等于
                    case "!=": return actual != expected;  // 不等于
                    default:   return actual == expected;  // 默认相等比较
                }
            }

            // 字符串类型比较：转换为字符串进行比较
            String actualStr = actualValue.toString();
            String expectedStr = threshold.toString();
            // 相等比较（支持"=="和"="两种写法）
            if ("==".equals(operator) || "=".equals(operator)) {
                return actualStr.equals(expectedStr);
            }
            // 不等比较
            if ("!=".equals(operator)) {
                return !actualStr.equals(expectedStr);
            }
            // 默认执行相等比较
            return actualStr.equals(expectedStr);
        } catch (Exception e) {
            // 任何异常（JSON解析失败、类型转换失败等）都返回匹配失败
            return false;
        }
    }
}
