package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.entity.OtaRecord;
import com.iot.platform.entity.OtaTask;
import com.iot.platform.mapper.OtaRecordMapper;
import com.iot.platform.mapper.OtaTaskMapper;
import com.iot.platform.service.OtaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * OTA升级服务实现类
 *
 * 负责固件升级任务的创建、灰度发布和进度管理。
 *
 * 业务场景：
 * 物联网设备数量庞大、分布广泛，远程OTA升级是核心运维能力。
 * 本服务支持灰度升级策略，避免一次性全量升级导致的大面积故障。
 *
 * 升级流程：
 * 1. 管理员上传固件 → 创建OTA任务（选择产品、灰度比例）
 * 2. 设备端定时拉取待执行任务 → 下载固件 → 安装 → 重启
 * 3. 设备上报升级进度，服务端记录
 * 4. 运维确认稳定后推进灰度比例
 * 5. 灰度到100%自动完成
 *
 * 设计亮点：
 * - 灰度发布：支持百分比灰度控制，逐步放量降低风险
 * - 失败自动重试：升级失败自动重试最多3次，提高成功率
 * - 完整轨迹记录：每次升级操作可追溯
 *
 * @author 王恒
 */
@Service
@RequiredArgsConstructor
public class OtaServiceImpl implements OtaService {

    private final OtaTaskMapper otaTaskMapper;
    private final OtaRecordMapper otaRecordMapper;

    /**
     * 创建OTA升级任务
     * <p>
     * 生成唯一任务ID，设置默认值后持久化到数据库。
     * 
     * 任务ID生成规则：OTA-前缀 + UUID前8位，确保唯一性且可读性良好
     * 默认值策略：
     * - grayPercent（灰度比例）：默认为0，表示暂不启动灰度
     * - currentPercent（当前进度）：默认为0，表示任务刚创建
     * - taskStatus（任务状态）：默认为CREATED，表示任务已创建待执行
     *
     * @param task OTA任务实体，包含固件信息、产品ID、灰度比例等
     * @return 创建成功后的OTA任务实体（已包含自动生成的taskId）
     */
    @Override
    @Transactional
    public OtaTask createTask(OtaTask task) {
        task.setTaskId("OTA-" + UUID.randomUUID().toString().substring(0, 8));
        if (task.getGrayPercent() == null) task.setGrayPercent(0);
        if (task.getCurrentPercent() == null) task.setCurrentPercent(0);
        if (task.getTaskStatus() == null) task.setTaskStatus("CREATED");
        otaTaskMapper.insert(task);
        return task;
    }

    @Override
    @Transactional
    public void advanceGrayPercent(String taskId) {
        OtaTask task = otaTaskMapper.selectOne(
                new LambdaQueryWrapper<OtaTask>().eq(OtaTask::getTaskId, taskId));
        if (task == null) throw new RuntimeException("OTA任务不存在: " + taskId);

        int newPercent = Math.min(task.getGrayPercent() + 10, 100);
        otaTaskMapper.update(null,
                new LambdaUpdateWrapper<OtaTask>()
                        .eq(OtaTask::getTaskId, taskId)
                        .set(OtaTask::getGrayPercent, newPercent)
                        .set(OtaTask::getCurrentPercent, newPercent)
                        .set(OtaTask::getTaskStatus, newPercent >= 100 ? "COMPLETED" : "RUNNING"));
    }

    /**
     * 更新设备OTA升级进度记录
     * <p>
     * 该方法负责更新指定设备在指定OTA任务中的升级进度。采用"存在则更新，不存在则插入"的策略。
     * 支持失败自动重试机制：当升级失败且重试次数未达到上限(3次)时，自动将状态重置为PENDING以便重试。
     *
     * @param deviceId   设备ID
     * @param taskId     OTA任务ID
     * @param status     升级状态（如PENDING/RUNNING/COMPLETED/FAILED）
     * @param progress   升级进度百分比(0-100)
     * @param failReason 失败原因描述（成功时可为null）
     * @return 更新或新增的OTA记录实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OtaRecord updateProgress(String deviceId, String taskId, String status, int progress, String failReason) {
        // 1. 查询设备在该任务下的现有OTA记录
        OtaRecord existing = otaRecordMapper.selectOne(
                new LambdaQueryWrapper<OtaRecord>()
                        .eq(OtaRecord::getDeviceId, deviceId)
                        .eq(OtaRecord::getTaskId, taskId));

        if (existing != null) {
            // 2. 存在记录→更新进度（单行UPDATE，通过ID主键确保DB行锁保护，防止并发冲突）
            otaRecordMapper.update(null,
                    new LambdaUpdateWrapper<OtaRecord>()
                            .eq(OtaRecord::getId, existing.getId())
                            .set(OtaRecord::getOtaStatus, status)
                            .set(OtaRecord::getProgress, progress)
                            .set(OtaRecord::getFailReason, failReason));

            // 3. 失败自动重试机制：状态为FAILED且重试次数<3时，重置为PENDING状态等待重试
            if ("FAILED".equals(status) && existing.getRetryCount() < 3) {
                otaRecordMapper.update(null,
                        new LambdaUpdateWrapper<OtaRecord>()
                                .eq(OtaRecord::getId, existing.getId())
                                .set(OtaRecord::getRetryCount, existing.getRetryCount() + 1)
                                .set(OtaRecord::getOtaStatus, "PENDING"));
            }
            return existing;
        } else {
            // 4. 不存在记录→创建新的OTA进度记录
            OtaRecord record = new OtaRecord();
            record.setDeviceId(deviceId);
            record.setTaskId(taskId);
            record.setOtaStatus(status);
            record.setProgress(progress);
            record.setFailReason(failReason);
            record.setRetryCount(0);
            otaRecordMapper.insert(record);
            return record;
        }
    }

    @Override
    public List<OtaRecord> getDeviceOtaRecords(String deviceId) {
        return otaRecordMapper.selectList(
                new LambdaQueryWrapper<OtaRecord>()
                        .eq(OtaRecord::getDeviceId, deviceId)
                        .orderByDesc(OtaRecord::getCreateTime));
    }

    @Override
    public List<OtaTask> listTasks() {
        return otaTaskMapper.selectList(
                new LambdaQueryWrapper<OtaTask>()
                        .orderByDesc(OtaTask::getCreateTime));
    }

    @Override
    public Map<String, Object> listTasks(int pageNum, int pageSize) {
        Page<OtaTask> page = new Page<>(pageNum, pageSize);
        Page<OtaTask> result = otaTaskMapper.selectPage(page,
                new LambdaQueryWrapper<OtaTask>()
                        .orderByDesc(OtaTask::getCreateTime));
        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("total", result.getTotal());
        return map;
    }
}
