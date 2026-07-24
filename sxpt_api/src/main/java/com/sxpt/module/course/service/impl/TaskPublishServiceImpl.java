package com.sxpt.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.course.entity.Task;
import com.sxpt.module.course.entity.TaskTeachingPoint;
import com.sxpt.module.course.mapper.TaskMapper;
import com.sxpt.module.course.mapper.TaskTeachingPointMapper;
import com.sxpt.module.course.service.TaskPublishService;
import com.sxpt.module.teaching.service.TeachingAssetPublishCheckService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务发布服务实现。
 *
 * 业务功能：
 * 1. 保存学习、练习或考试任务，形成学生侧任务入口。
 * 2. 保存任务教学点关联，形成任务执行需要加载的教学内容范围。
 *
 * 关键流程：
 * 1. 创建任务时补齐版本、草稿状态、通用状态和软删除字段。
 * 2. 创建任务教学点关联时补齐必学标记、通用状态和软删除字段。
 * 3. 显式发布任务教学点资产前，统一执行教学资产完整性校验。
 */
@Service
@Profile("!test")
public class TaskPublishServiceImpl implements TaskPublishService {

    private static final long DEFAULT_VERSION_NO = 1L;

    private static final String DEFAULT_TASK_CREATE_STATUS = "DRAFT";

    private static final String PUBLISHED_TASK_STATUS = "PUBLISHED";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final TaskMapper taskMapper;

    private final TaskTeachingPointMapper taskTeachingPointMapper;

    private final TeachingAssetPublishCheckService teachingAssetPublishCheckService;

    public TaskPublishServiceImpl(TaskMapper taskMapper,
                                  TaskTeachingPointMapper taskTeachingPointMapper,
                                  TeachingAssetPublishCheckService teachingAssetPublishCheckService) {
        this.taskMapper = taskMapper;
        this.taskTeachingPointMapper = taskTeachingPointMapper;
        this.teachingAssetPublishCheckService = teachingAssetPublishCheckService;
    }

    /**
     * 创建教学任务。
     *
     * @param task 教学任务实体。
     * @return 已保存的教学任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task createTask(Task task) {
        validateTaskCreateFields(task);
        fillTaskCreateDefaults(task);
        taskMapper.insert(task);
        return task;
    }

    /**
     * 查询教学任务。
     *
     * @param tenantId 租户 ID。
     * @param courseId 课程 ID。
     * @param publishOrgId 发布班级 ID。
     * @return 教学任务列表。
     */
    @Override
    public List<Task> listTasks(String tenantId, String courseId, String publishOrgId) {
        requireText(tenantId);
        QueryWrapper<Task> wrapper = new QueryWrapper<Task>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE);
        if (StringUtils.hasText(courseId)) {
            wrapper.eq("course_id", courseId);
        }
        if (StringUtils.hasText(publishOrgId)) {
            wrapper.eq("publish_org_id", publishOrgId);
        }
        return taskMapper.selectList(wrapper.orderByAsc("task_code", "version_no"));
    }

    /**
     * 创建任务教学点关联。
     *
     * @param taskTeachingPoint 任务教学点关联实体。
     * @return 已保存的任务教学点关联。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskTeachingPoint createTaskTeachingPoint(TaskTeachingPoint taskTeachingPoint) {
        validateTaskTeachingPointCreateFields(taskTeachingPoint);
        fillTaskTeachingPointCreateDefaults(taskTeachingPoint);
        taskTeachingPointMapper.insert(taskTeachingPoint);
        return taskTeachingPoint;
    }

    /**
     * 查询任务下的教学点关联。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @return 任务教学点关联列表。
     */
    @Override
    public List<TaskTeachingPoint> listTeachingPointsByTask(String tenantId, String taskId) {
        requireText(tenantId);
        requireText(taskId);
        return taskTeachingPointMapper.selectList(new QueryWrapper<TaskTeachingPoint>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "create_time"));
    }

    /**
     * 发布指定任务教学点资产。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param operatorId 发布操作人 ID。
     * @return 已发布的教学任务。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Task publishTaskTeachingPointAssets(String tenantId, String taskId, String teachingPointId,
                                               String evaluationRuleId, String operatorId) {
        requireText(tenantId);
        requireText(taskId);
        requireText(teachingPointId);
        requireText(evaluationRuleId);
        requireText(operatorId);
        teachingAssetPublishCheckService.validateReadyToPublish(tenantId, taskId, teachingPointId, evaluationRuleId);
        Task task = loadTask(tenantId, taskId);
        task.setTaskStatus(PUBLISHED_TASK_STATUS);
        task.setUpdateBy(operatorId);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
        return task;
    }

    /**
     * 校验创建任务所需的最小字段。
     *
     * @param task 教学任务实体。
     */
    private void validateTaskCreateFields(Task task) {
        if (task == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(task.getId());
        requireText(task.getTenantId());
        requireText(task.getCourseId());
        requireText(task.getPublishOrgId());
        requireText(task.getTaskCode());
        requireText(task.getTaskName());
        requireText(task.getTaskType());
        requireText(task.getTaskGoal());
    }

    /**
     * 校验创建任务教学点关联所需的最小字段。
     *
     * @param taskTeachingPoint 任务教学点关联实体。
     */
    private void validateTaskTeachingPointCreateFields(TaskTeachingPoint taskTeachingPoint) {
        if (taskTeachingPoint == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(taskTeachingPoint.getId());
        requireText(taskTeachingPoint.getTenantId());
        requireText(taskTeachingPoint.getTaskId());
        requireText(taskTeachingPoint.getTeachingPointId());
        if (taskTeachingPoint.getSequenceNo() == null) {
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
     * 加载待发布教学任务。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @return 教学任务实体。
     */
    private Task loadTask(String tenantId, String taskId) {
        Task task = taskMapper.selectOne(new QueryWrapper<Task>()
                .eq("tenant_id", tenantId)
                .eq("id", taskId)
                .eq("deleted", Boolean.FALSE));
        if (task == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return task;
    }

    /**
     * 补齐创建任务时的默认字段。
     *
     * @param task 教学任务实体。
     */
    private void fillTaskCreateDefaults(Task task) {
        LocalDateTime now = LocalDateTime.now();
        if (task.getVersionNo() == null) {
            task.setVersionNo(DEFAULT_VERSION_NO);
        }
        if (task.getCreateTime() == null) {
            task.setCreateTime(now);
        }
        if (task.getUpdateTime() == null) {
            task.setUpdateTime(now);
        }
        if (!StringUtils.hasText(task.getTaskStatus())) {
            task.setTaskStatus(DEFAULT_TASK_CREATE_STATUS);
        }
        if (!StringUtils.hasText(task.getStatus())) {
            task.setStatus(DEFAULT_STATUS);
        }
        if (task.getDeleted() == null) {
            task.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 补齐创建任务教学点关联时的默认字段。
     *
     * @param taskTeachingPoint 任务教学点关联实体。
     */
    private void fillTaskTeachingPointCreateDefaults(TaskTeachingPoint taskTeachingPoint) {
        LocalDateTime now = LocalDateTime.now();
        if (taskTeachingPoint.getCreateTime() == null) {
            taskTeachingPoint.setCreateTime(now);
        }
        if (taskTeachingPoint.getUpdateTime() == null) {
            taskTeachingPoint.setUpdateTime(now);
        }
        if (taskTeachingPoint.getRequiredFlag() == null) {
            taskTeachingPoint.setRequiredFlag(Boolean.TRUE);
        }
        if (!StringUtils.hasText(taskTeachingPoint.getStatus())) {
            taskTeachingPoint.setStatus(DEFAULT_STATUS);
        }
        if (taskTeachingPoint.getDeleted() == null) {
            taskTeachingPoint.setDeleted(Boolean.FALSE);
        }
    }
}

