package com.sxpt.module.execution.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.entity.ExecutionTrace;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.mapper.ExecutionTraceMapper;
import com.sxpt.module.execution.mapper.TaskExecutionMapper;
import com.sxpt.module.execution.service.ExecutionTraceService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.mapper.TaskStepMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生执行轨迹服务实现。
 *
 * 业务功能：
 * 1. 保存 SDK 上报的学生运行操作轨迹，为练习过程分和自动评分提供事实来源。
 * 2. 基于 clientTraceId 处理 SDK 重试幂等，避免网络重试造成重复轨迹。
 *
 * 关键流程：
 * 1. 先校验轨迹所属租户、执行记录、轨迹类型、轨迹时间和顺序号。
 * 2. 当 clientTraceId 已存在时直接返回原轨迹；不存在时补齐默认生命周期字段后插入。
 */
@Service
@Profile("!test")
public class ExecutionTraceServiceImpl implements ExecutionTraceService {

    private static final String RUNNING_STATUS = "RUNNING";

    private static final String DEFAULT_ARCHIVE_STATUS = "NONE";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final List<String> SENSITIVE_KEYWORDS = java.util.Arrays.asList(
            "password", "pwd", "secret", "token", "idcard", "id_card", "identitycard",
            "mobile", "phone", "密码", "口令", "令牌", "身份证", "手机号", "手机号码");

    private final ExecutionTraceMapper executionTraceMapper;

    private final TaskExecutionMapper taskExecutionMapper;

    private final TaskStepMapper taskStepMapper;

    public ExecutionTraceServiceImpl(ExecutionTraceMapper executionTraceMapper,
                                     TaskExecutionMapper taskExecutionMapper,
                                     TaskStepMapper taskStepMapper) {
        this.executionTraceMapper = executionTraceMapper;
        this.taskExecutionMapper = taskExecutionMapper;
        this.taskStepMapper = taskStepMapper;
    }

    /**
     * 上报单条学生执行轨迹。
     *
     * @param executionTrace 学生执行轨迹。
     * @return 已保存或幂等命中的学生执行轨迹。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExecutionTrace reportExecutionTrace(ExecutionTrace executionTrace) {
        validateReportFields(executionTrace);
        TaskExecution execution = validateExecutionWritable(executionTrace);
        validateTraceScope(executionTrace, execution);
        ExecutionTrace existed = findExistedClientTrace(executionTrace);
        if (existed != null) {
            return existed;
        }
        fillCreateDefaults(executionTrace);
        executionTraceMapper.insert(executionTrace);
        return executionTrace;
    }

    /**
     * 查询指定任务执行下的学生执行轨迹。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @return 按轨迹顺序排序的学生执行轨迹列表。
     */
    @Override
    public List<ExecutionTrace> listByExecution(String tenantId, String executionId) {
        requireText(tenantId);
        requireText(executionId);
        return executionTraceMapper.selectList(new QueryWrapper<ExecutionTrace>()
                .eq("tenant_id", tenantId)
                .eq("execution_id", executionId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "trace_time"));
    }

    /**
     * 校验轨迹所属执行仍允许写入。
     *
     * @param executionTrace 学生执行轨迹。
     */
    private TaskExecution validateExecutionWritable(ExecutionTrace executionTrace) {
        TaskExecution execution = taskExecutionMapper.selectOne(new QueryWrapper<TaskExecution>()
                .eq("tenant_id", executionTrace.getTenantId())
                .eq("id", executionTrace.getExecutionId())
                .eq("deleted", Boolean.FALSE));
        if (execution == null || !RUNNING_STATUS.equals(execution.getExecutionStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return execution;
    }

    /**
     * 校验步骤型轨迹确实属于当前执行任务。
     *
     * 练习完成事实由浏览器采集后上报，服务端仍需以已发布 task_step 为边界，
     * 防止客户端把其它任务的步骤 ID 或教学点 ID 写入当前执行并误命中评分项。
     * 不携带 taskStepId 的会话、页面状态类轨迹继续按原规则接收。
     *
     * @param executionTrace 待上报轨迹。
     * @param execution 当前运行中的任务执行。
     */
    private void validateTraceScope(ExecutionTrace executionTrace, TaskExecution execution) {
        if (!StringUtils.hasText(executionTrace.getTaskStepId())) {
            return;
        }
        TaskStep taskStep = taskStepMapper.selectOne(new QueryWrapper<TaskStep>()
                .eq("tenant_id", executionTrace.getTenantId())
                .eq("id", executionTrace.getTaskStepId())
                .eq("task_id", execution.getTaskId())
                .eq("deleted", Boolean.FALSE));
        if (taskStep == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (StringUtils.hasText(executionTrace.getTeachingPointId())
                && !executionTrace.getTeachingPointId().equals(taskStep.getTeachingPointId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验 SDK 执行轨迹上报所需的最小字段。
     *
     * @param executionTrace 学生执行轨迹。
     */
    private void validateReportFields(ExecutionTrace executionTrace) {
        if (executionTrace == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(executionTrace.getId());
        requireText(executionTrace.getTenantId());
        requireText(executionTrace.getExecutionId());
        requireText(executionTrace.getClientTraceId());
        requireText(executionTrace.getTraceType());
        if (executionTrace.getTraceTime() == null || executionTrace.getSequenceNo() == null
                || executionTrace.getSequenceNo() <= 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        rejectSensitivePayload(executionTrace.getInputDataJson());
        rejectSensitivePayload(executionTrace.getOutputDataJson());
        rejectSensitivePayload(executionTrace.getBeforeStateJson());
        rejectSensitivePayload(executionTrace.getAfterStateJson());
        rejectSensitivePayload(executionTrace.getEvidenceJson());
    }

    /**
     * 拒绝明显包含敏感字段名的轨迹摘要。
     *
     * @param payloadJson SDK 上报的轨迹摘要 JSON。
     */
    private void rejectSensitivePayload(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return;
        }
        String normalizedPayload = payloadJson.toLowerCase();
        for (String sensitiveKeyword : SENSITIVE_KEYWORDS) {
            if (normalizedPayload.contains(sensitiveKeyword)) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
        }
    }

    /**
     * 按客户端轨迹 ID 查找已入库轨迹。
     *
     * @param executionTrace 学生执行轨迹。
     * @return 已存在轨迹；未命中时返回 null。
     */
    private ExecutionTrace findExistedClientTrace(ExecutionTrace executionTrace) {
        return executionTraceMapper.selectOne(new QueryWrapper<ExecutionTrace>()
                .eq("tenant_id", executionTrace.getTenantId())
                .eq("execution_id", executionTrace.getExecutionId())
                .eq("client_trace_id", executionTrace.getClientTraceId())
                .eq("deleted", Boolean.FALSE));
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
     * 补齐轨迹创建时的默认字段。
     *
     * @param executionTrace 学生执行轨迹。
     */
    private void fillCreateDefaults(ExecutionTrace executionTrace) {
        LocalDateTime now = LocalDateTime.now();
        if (executionTrace.getRetryCount() == null) {
            executionTrace.setRetryCount(0L);
        }
        if (!StringUtils.hasText(executionTrace.getArchiveStatus())) {
            executionTrace.setArchiveStatus(DEFAULT_ARCHIVE_STATUS);
        }
        if (executionTrace.getCreateTime() == null) {
            executionTrace.setCreateTime(now);
        }
        if (executionTrace.getUpdateTime() == null) {
            executionTrace.setUpdateTime(now);
        }
        if (!StringUtils.hasText(executionTrace.getStatus())) {
            executionTrace.setStatus(DEFAULT_STATUS);
        }
        if (executionTrace.getDeleted() == null) {
            executionTrace.setDeleted(Boolean.FALSE);
        }
    }
}

