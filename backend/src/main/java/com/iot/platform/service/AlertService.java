package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.entity.AlertRecord;

import java.util.Map;

public interface AlertService {
    /** 处理设备事件，匹配告警规则 */
    AlertRecord processEvent(String deviceId, String eventType, Map<String, Object> eventData);

    /** 查询告警列表（分页） */
    Page<AlertRecord> listAlerts(int pageNum, int pageSize, String deviceId, String handleStatus);

    /** 更新告警处理状态 */
    AlertRecord updateHandleStatus(String alertId, String newStatus, String handlerName, String comment);

    /** 检查告警超时未处理，自动升级 */
    void checkAlertEscalation();
}
