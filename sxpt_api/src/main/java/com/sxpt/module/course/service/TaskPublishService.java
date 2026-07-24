package com.sxpt.module.course.service;

import com.sxpt.module.course.entity.Task;
import com.sxpt.module.course.entity.TaskTeachingPoint;

import java.util.List;

/**
 * 任务发布服务。
 *
 * 业务功能：
 * 1. 创建学习、练习或考试任务，使课程班级学生可以看到任务。
 * 2. 维护任务与教学点关联，使任务能加载教学点、步骤和评分配置。
 *
 * 关键流程：
 * 1. 创建任务时校验课程、发布班级、任务编码、任务类型和任务目标。
 * 2. 关联教学点时校验任务、教学点和排序号。
 */
public interface TaskPublishService {

    /**
     * 创建教学任务。
     *
     * @param task 教学任务实体。
     * @return 已保存的教学任务。
     */
    Task createTask(Task task);

    /**
     * 查询教学任务。
     *
     * @param tenantId 租户 ID。
     * @param courseId 课程 ID。
     * @param publishOrgId 发布班级 ID。
     * @return 教学任务列表。
     */
    List<Task> listTasks(String tenantId, String courseId, String publishOrgId);

    /**
     * 创建任务教学点关联。
     *
     * @param taskTeachingPoint 任务教学点关联实体。
     * @return 已保存的任务教学点关联。
     */
    TaskTeachingPoint createTaskTeachingPoint(TaskTeachingPoint taskTeachingPoint);

    /**
     * 查询任务下的教学点关联。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @return 任务教学点关联列表。
     */
    List<TaskTeachingPoint> listTeachingPointsByTask(String tenantId, String taskId);

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
    Task publishTaskTeachingPointAssets(String tenantId, String taskId, String teachingPointId,
                                        String evaluationRuleId, String operatorId);
}
