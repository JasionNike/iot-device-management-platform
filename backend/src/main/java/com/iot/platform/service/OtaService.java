package com.iot.platform.service;

import com.iot.platform.entity.OtaTask;
import com.iot.platform.entity.OtaRecord;

import java.util.List;

public interface OtaService {
    /** 创建OTA升级任务 */
    OtaTask createTask(OtaTask task);

    /** 启动灰度下一批 */
    void advanceGrayPercent(String taskId);

    /** 设备上报OTA进度 */
    OtaRecord updateProgress(String deviceId, String taskId, String status, int progress, String failReason);

    /** 查询设备的OTA记录 */
    List<OtaRecord> getDeviceOtaRecords(String deviceId);

    /** 查询任务列表 */
    List<OtaTask> listTasks();
}
