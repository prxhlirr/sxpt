package com.sxpt.module.teaching.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.mapper.TaskStepMapper;
import com.sxpt.module.teaching.service.TaskStepService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学任务步骤服务实现。
 *
 * 业务功能：
 * 1. 保存教师发布的教学步骤，形成学习、练习和评分引用的步骤资产。
 * 2. 按任务和教学点查询步骤，保证 SDK runtime 能按顺序加载。
 *
 * 关键流程：
 * 1. 创建时校验最小发布字段，避免生成无法排序或无法归属的步骤。
 * 2. 补齐必做、不可跳过、通用状态和软删除字段。
 */
@Service
@Profile("!test")
public class TaskStepServiceImpl implements TaskStepService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final TaskStepMapper taskStepMapper;

    public TaskStepServiceImpl(TaskStepMapper taskStepMapper) {
        this.taskStepMapper = taskStepMapper;
    }

    /**
     * 创建教学任务步骤。
     *
     * @param taskStep 教学任务步骤实体。
     * @return 已保存的教学任务步骤。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskStep createTaskStep(TaskStep taskStep) {
        validateCreateFields(taskStep);
        fillCreateDefaults(taskStep);
        taskStepMapper.insert(taskStep);
        return taskStep;
    }

    /**
     * 按任务和教学点查询教学步骤。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 教学步骤列表。
     */
    @Override
    public List<TaskStep> listByTaskAndTeachingPoint(String tenantId, String taskId, String teachingPointId) {
        requireText(tenantId);
        requireText(taskId);
        requireText(teachingPointId);
        return taskStepMapper.selectList(new QueryWrapper<TaskStep>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("teaching_point_id", teachingPointId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "create_time"));
    }

    /**
     * 校验创建教学任务步骤所需的最小字段。
     *
     * @param taskStep 教学任务步骤实体。
     */
    private void validateCreateFields(TaskStep taskStep) {
        if (taskStep == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(taskStep.getId());
        requireText(taskStep.getTenantId());
        requireText(taskStep.getTaskId());
        requireText(taskStep.getTeachingPointId());
        requireText(taskStep.getStepCode());
        requireText(taskStep.getStepName());
        if (taskStep.getSequenceNo() == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
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
     * 补齐创建教学任务步骤时的默认字段。
     *
     * @param taskStep 教学任务步骤实体。
     */
    private void fillCreateDefaults(TaskStep taskStep) {
        LocalDateTime now = LocalDateTime.now();
        if (taskStep.getCreateTime() == null) {
            taskStep.setCreateTime(now);
        }
        if (taskStep.getUpdateTime() == null) {
            taskStep.setUpdateTime(now);
        }
        if (taskStep.getRequired() == null) {
            taskStep.setRequired(Boolean.TRUE);
        }
        if (taskStep.getAllowSkip() == null) {
            taskStep.setAllowSkip(Boolean.FALSE);
        }
        if (!StringUtils.hasText(taskStep.getStatus())) {
            taskStep.setStatus(DEFAULT_STATUS);
        }
        if (taskStep.getDeleted() == null) {
            taskStep.setDeleted(Boolean.FALSE);
        }
    }
}

