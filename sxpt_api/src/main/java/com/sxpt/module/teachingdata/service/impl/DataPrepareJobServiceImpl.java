package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.mapper.DataPrepareJobMapper;
import com.sxpt.module.teachingdata.service.DataPrepareJobService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据准备任务服务实现。
 *
 * 业务功能：
 * 1. 创建 data_prepare_job 记录，保存原平台数据准备任务的幂等和审计信息。
 * 2. 查询任务列表和待重试任务，支撑后台任务管理与失败补偿。
 *
 * 关键流程：
 * 1. 创建时校验租户、原平台、业务模块、场景、任务类型、幂等键、请求批次和触发方式。
 * 2. 补齐任务状态、计数、重试次数、审计时间和软删除默认值。
 * 3. 查询待重试任务时只返回失败且已到达 nextRetryTime 的有效任务。
 */
@Service
@Profile("!test")
public class DataPrepareJobServiceImpl implements DataPrepareJobService {

    private final DataPrepareJobMapper dataPrepareJobMapper;

    public DataPrepareJobServiceImpl(DataPrepareJobMapper dataPrepareJobMapper) {
        this.dataPrepareJobMapper = dataPrepareJobMapper;
    }

    /**
     * 创建数据准备任务。
     *
     * @param job 数据准备任务实体。
     * @return 已保存的数据准备任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataPrepareJob createDataPrepareJob(DataPrepareJob job) {
        validateCreateFields(job);
        fillCreateDefaults(job);
        dataPrepareJobMapper.insert(job);
        return job;
    }

    /**
     * 按租户和幂等键查询数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param idempotencyKey 幂等键。
     * @return 命中的数据准备任务；不存在时返回 null。
     */
    @Override
    public DataPrepareJob getByIdempotencyKey(String tenantId, String idempotencyKey) {
        requireText(tenantId);
        requireText(idempotencyKey);
        return dataPrepareJobMapper.selectOne(new QueryWrapper<DataPrepareJob>()
                .eq("tenant_id", tenantId)
                .eq("idempotency_key", idempotencyKey)
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
    }

    /**
     * 按租户和任务 ID 查询未删除的数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param jobId 数据准备任务 ID。
     * @return 命中的数据准备任务；不存在时返回 null。
     */
    @Override
    public DataPrepareJob getByTenantAndId(String tenantId, String jobId) {
        requireText(tenantId);
        requireText(jobId);
        return dataPrepareJobMapper.selectOne(new QueryWrapper<DataPrepareJob>()
                .eq("tenant_id", tenantId)
                .eq("id", jobId)
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
    }

    /**
     * 标记失败任务进入人工重试。
     *
     * @param tenantId 租户 ID。
     * @param jobId 数据准备任务 ID。
     * @param updateBy 操作人。
     * @return 已更新的数据准备任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataPrepareJob markRetrying(String tenantId, String jobId, String updateBy) {
        DataPrepareJob job = getByTenantAndId(tenantId, jobId);
        if (job == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        PrepareJobStatus status = PrepareJobStatus.fromValue(job.getJobStatus());
        if (status == null || !status.isRetryable()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        job.setJobStatus(PrepareJobStatus.CREATED.getValue());
        job.setRetryCount(defaultLong(job.getRetryCount()) + 1L);
        job.setNextRetryTime(null);
        job.setUpdateBy(updateBy);
        job.setUpdateTime(LocalDateTime.now());
        dataPrepareJobMapper.updateById(job);
        return job;
    }

    /**
     * 查询指定任务和场景下的数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param sceneType 教学场景。
     * @return 数据准备任务列表。
     */
    @Override
    public List<DataPrepareJob> listByTaskAndScene(String tenantId, String taskId, String sceneType) {
        requireText(tenantId);
        requireText(taskId);
        requireText(sceneType);
        return dataPrepareJobMapper.selectList(new QueryWrapper<DataPrepareJob>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("scene_type", sceneType)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询到达重试时间的数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param now 当前时间。
     * @return 待重试数据准备任务列表。
     */
    @Override
    public List<DataPrepareJob> listRetryableJobs(String tenantId, LocalDateTime now) {
        requireText(tenantId);
        if (now == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        return dataPrepareJobMapper.selectList(new QueryWrapper<DataPrepareJob>()
                .eq("tenant_id", tenantId)
                .in("job_status",
                        PrepareJobStatus.FAILED.getValue(),
                        PrepareJobStatus.PARTIAL_FAILED.getValue())
                .le("next_retry_time", now)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("next_retry_time")
                .orderByAsc("create_time"));
    }

    /**
     * 校验创建数据准备任务所需的最小字段。
     *
     * @param job 数据准备任务实体。
     */
    private void validateCreateFields(DataPrepareJob job) {
        if (job == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(job.getId());
        requireText(job.getTenantId());
        requireText(job.getConnectorSystemId());
        requireText(job.getModuleCode());
        requireText(job.getSceneType());
        requireText(job.getJobType());
        requireText(job.getRequestBatchId());
        requireText(job.getIdempotencyKey());
        requireText(job.getTriggerType());
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建时默认字段。
     *
     * @param job 数据准备任务实体。
     */
    private void fillCreateDefaults(DataPrepareJob job) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(job.getJobStatus())) {
            job.setJobStatus(PrepareJobStatus.CREATED.getValue());
        }
        if (job.getExpectedCount() == null) {
            job.setExpectedCount(0L);
        }
        if (job.getSuccessCount() == null) {
            job.setSuccessCount(0L);
        }
        if (job.getFailedCount() == null) {
            job.setFailedCount(0L);
        }
        if (job.getRetryCount() == null) {
            job.setRetryCount(0L);
        }
        if (job.getCreateTime() == null) {
            job.setCreateTime(now);
        }
        if (job.getUpdateTime() == null) {
            job.setUpdateTime(now);
        }
        if (!StringUtils.hasText(job.getStatus())) {
            job.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (job.getDeleted() == null) {
            job.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 将空计数字段按 0 处理，避免历史数据重试时出现空指针。
     *
     * @param value 原始计数。
     * @return 非空计数。
     */
    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
