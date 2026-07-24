package com.sxpt.module.execution.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.mapper.TaskExecutionContextMapper;
import com.sxpt.module.execution.service.TaskExecutionContextService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务执行上下文快照服务实现。
 *
 * 业务功能：
 * 1. 保存学生进入任务时的版本快照，确保历史执行不受后续配置变更影响。
 * 2. 提供按执行记录、任务和学生维度查询快照的能力。
 *
 * 关键流程：
 * 1. 创建前校验任务执行 ID、任务 ID、学生 ID、SDK 模式和完整上下文 JSON。
 * 2. 创建时补齐 archiveStatus、status、deleted、createTime 和 updateTime。
 */
@Service
@Profile("!test")
public class TaskExecutionContextServiceImpl implements TaskExecutionContextService {

    private static final String DEFAULT_ARCHIVE_STATUS = "NONE";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final TaskExecutionContextMapper taskExecutionContextMapper;

    public TaskExecutionContextServiceImpl(TaskExecutionContextMapper taskExecutionContextMapper) {
        this.taskExecutionContextMapper = taskExecutionContextMapper;
    }

    /**
     * 创建任务执行上下文快照。
     *
     * @param context 任务执行上下文快照实体。
     * @return 已保存的任务执行上下文快照。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskExecutionContext createContext(TaskExecutionContext context) {
        validateCreateFields(context);
        fillCreateDefaults(context);
        taskExecutionContextMapper.insert(context);
        return context;
    }

    /**
     * 查询任务执行上下文快照。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID，可为空。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 任务执行上下文快照列表。
     */
    @Override
    public List<TaskExecutionContext> listContexts(String tenantId, String executionId, String taskId,
                                                   String studentId) {
        requireText(tenantId);
        QueryWrapper<TaskExecutionContext> wrapper = new QueryWrapper<TaskExecutionContext>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE);
        if (StringUtils.hasText(executionId)) {
            wrapper.eq("execution_id", executionId);
        }
        if (StringUtils.hasText(taskId)) {
            wrapper.eq("task_id", taskId);
        }
        if (StringUtils.hasText(studentId)) {
            wrapper.eq("student_id", studentId);
        }
        return taskExecutionContextMapper.selectList(wrapper.orderByDesc("create_time"));
    }

    /**
     * 校验创建上下文快照所需的最小字段。
     *
     * @param context 任务执行上下文快照实体。
     */
    private void validateCreateFields(TaskExecutionContext context) {
        if (context == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(context.getId());
        requireText(context.getTenantId());
        requireText(context.getExecutionId());
        requireText(context.getTaskId());
        requireText(context.getStudentId());
        requireText(context.getSdkMode());
        requireText(context.getContextJson());
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
     * 补齐创建上下文快照时的默认字段。
     *
     * @param context 任务执行上下文快照实体。
     */
    private void fillCreateDefaults(TaskExecutionContext context) {
        LocalDateTime now = LocalDateTime.now();
        if (context.getArchiveStatus() == null) {
            context.setArchiveStatus(DEFAULT_ARCHIVE_STATUS);
        }
        if (context.getCreateTime() == null) {
            context.setCreateTime(now);
        }
        if (context.getUpdateTime() == null) {
            context.setUpdateTime(now);
        }
        if (context.getStatus() == null) {
            context.setStatus(DEFAULT_STATUS);
        }
        if (context.getDeleted() == null) {
            context.setDeleted(Boolean.FALSE);
        }
    }
}

