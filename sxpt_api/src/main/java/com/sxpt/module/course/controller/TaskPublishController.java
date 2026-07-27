package com.sxpt.module.course.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.course.dto.CreateTaskRequest;
import com.sxpt.module.course.dto.CreateTaskTeachingPointRequest;
import com.sxpt.module.course.entity.Task;
import com.sxpt.module.course.entity.TaskTeachingPoint;
import com.sxpt.module.course.service.TaskPublishService;
import com.sxpt.module.course.vo.TaskTeachingPointVO;
import com.sxpt.module.course.vo.TaskVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 任务发布接口。
 *
 * 业务功能：
 * 1. 提供学习、练习或考试任务创建入口。
 * 2. 提供任务关联教学点入口，使班级学生能够加载任务教学内容。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截缺少任务目标或排序号的无效配置。
 * 2. 将 DTO 转换为实体并生成应用层主键。
 * 3. 调用 Service 写入任务和关联关系，再转换为 VO 返回前端。
 */
@RestController
@RequestMapping("/api/v1/tasks")
@ConditionalOnProperty(name = "sxpt.task.publish-controller.enabled", havingValue = "true", matchIfMissing = true)
public class TaskPublishController {

    private final TaskPublishService taskPublishService;

    public TaskPublishController(TaskPublishService taskPublishService) {
        this.taskPublishService = taskPublishService;
    }

    /**
     * 创建教学任务。
     *
     * @param request 创建教学任务请求。
     * @return 已保存的教学任务。
     */
    @PostMapping("/create")
    public ApiResult<TaskVO> createTask(@Valid @RequestBody CreateTaskRequest request) {
        String currentUserId = CurrentUserContext.getRequiredUser().getUserId();
        return ApiResult.success(toTaskVO(taskPublishService.createTask(toTaskEntity(request, currentUserId))));
    }

    /**
     * 查询教学任务。
     *
     * @param tenantId 租户 ID。
     * @param courseId 课程 ID，可为空。
     * @param publishOrgId 发布班级 ID，可为空。
     * @return 教学任务列表。
     */
    @GetMapping
    public ApiResult<List<TaskVO>> listTasks(@RequestParam String tenantId,
                                             @RequestParam(required = false) String courseId,
                                             @RequestParam(required = false) String publishOrgId) {
        return ApiResult.success(toTaskVOList(taskPublishService.listTasks(tenantId, courseId, publishOrgId)));
    }

    /**
     * 创建任务教学点关联。
     *
     * @param request 创建任务教学点关联请求。
     * @return 已保存的任务教学点关联。
     */
    @PostMapping("/teaching-points/create")
    public ApiResult<TaskTeachingPointVO> createTaskTeachingPoint(
            @Valid @RequestBody CreateTaskTeachingPointRequest request) {
        String currentUserId = CurrentUserContext.getRequiredUser().getUserId();
        return ApiResult.success(toTaskTeachingPointVO(
                taskPublishService.createTaskTeachingPoint(toTaskTeachingPointEntity(request, currentUserId))));
    }

    /**
     * 查询任务下的教学点关联。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @return 任务教学点关联列表。
     */
    @GetMapping("/teaching-points")
    public ApiResult<List<TaskTeachingPointVO>> listTaskTeachingPoints(@RequestParam String tenantId,
                                                                       @RequestParam String taskId) {
        return ApiResult.success(toTaskTeachingPointVOList(
                taskPublishService.listTeachingPointsByTask(tenantId, taskId)));
    }

    /**
     * 发布任务教学点资产。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param operatorId 发布操作人 ID。
     * @return 已发布的教学任务。
     */
    @PostMapping("/teaching-points/publish")
    public ApiResult<TaskVO> publishTaskTeachingPointAssets(@RequestParam String tenantId,
                                                            @RequestParam String taskId,
                                                            @RequestParam String teachingPointId,
                                                            @RequestParam String evaluationRuleId,
                                                            @RequestParam(required = false) String operatorId) {
        String currentUserId = CurrentUserContext.getRequiredUser().getUserId();
        return ApiResult.success(toTaskVO(taskPublishService.publishTaskTeachingPointAssets(
                tenantId, taskId, teachingPointId, evaluationRuleId, currentUserId)));
    }

    /**
     * 将创建任务请求转换为实体。
     *
     * @param request 创建任务请求。
     * @return 教学任务实体。
     */
    private Task toTaskEntity(CreateTaskRequest request, String currentUserId) {
        Task task = new Task();
        task.setId(generateId());
        task.setTenantId(request.getTenantId());
        task.setCourseId(request.getCourseId());
        task.setPublishOrgId(request.getPublishOrgId());
        task.setTaskCode(request.getTaskCode());
        task.setTaskName(request.getTaskName());
        task.setTaskType(request.getTaskType());
        task.setTaskGoal(request.getTaskGoal());
        task.setTaskDescription(request.getTaskDescription());
        task.setStartTime(request.getStartTime());
        task.setEndTime(request.getEndTime());
        task.setTimeLimitMinutes(request.getTimeLimitMinutes());
        task.setOverlayPolicyJson(request.getOverlayPolicyJson());
        task.setCreateBy(currentUserId);
        task.setUpdateBy(currentUserId);
        return task;
    }

    /**
     * 将创建任务教学点关联请求转换为实体。
     *
     * @param request 创建任务教学点关联请求。
     * @return 任务教学点关联实体。
     */
    private TaskTeachingPoint toTaskTeachingPointEntity(CreateTaskTeachingPointRequest request, String currentUserId) {
        TaskTeachingPoint taskTeachingPoint = new TaskTeachingPoint();
        taskTeachingPoint.setId(generateId());
        taskTeachingPoint.setTenantId(request.getTenantId());
        taskTeachingPoint.setTaskId(request.getTaskId());
        taskTeachingPoint.setTeachingPointId(request.getTeachingPointId());
        taskTeachingPoint.setRequiredFlag(request.getRequiredFlag());
        taskTeachingPoint.setSequenceNo(request.getSequenceNo());
        taskTeachingPoint.setCreateBy(currentUserId);
        taskTeachingPoint.setUpdateBy(currentUserId);
        return taskTeachingPoint;
    }

    /**
     * 将教学任务实体列表转换为 VO 列表。
     *
     * @param tasks 教学任务实体列表。
     * @return 教学任务 VO 列表。
     */
    private List<TaskVO> toTaskVOList(List<Task> tasks) {
        List<TaskVO> result = new ArrayList<>();
        for (Task task : tasks) {
            result.add(toTaskVO(task));
        }
        return result;
    }

    /**
     * 将任务教学点关联实体列表转换为 VO 列表。
     *
     * @param taskTeachingPoints 任务教学点关联实体列表。
     * @return 任务教学点关联 VO 列表。
     */
    private List<TaskTeachingPointVO> toTaskTeachingPointVOList(List<TaskTeachingPoint> taskTeachingPoints) {
        List<TaskTeachingPointVO> result = new ArrayList<>();
        for (TaskTeachingPoint taskTeachingPoint : taskTeachingPoints) {
            result.add(toTaskTeachingPointVO(taskTeachingPoint));
        }
        return result;
    }

    /**
     * 将教学任务实体转换为 VO。
     *
     * @param task 教学任务实体。
     * @return 教学任务 VO。
     */
    private TaskVO toTaskVO(Task task) {
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTenantId(task.getTenantId());
        vo.setCourseId(task.getCourseId());
        vo.setPublishOrgId(task.getPublishOrgId());
        vo.setTaskCode(task.getTaskCode());
        vo.setTaskName(task.getTaskName());
        vo.setVersionNo(task.getVersionNo());
        vo.setTaskType(task.getTaskType());
        vo.setTaskGoal(task.getTaskGoal());
        vo.setTaskDescription(task.getTaskDescription());
        vo.setStartTime(task.getStartTime());
        vo.setEndTime(task.getEndTime());
        vo.setTimeLimitMinutes(task.getTimeLimitMinutes());
        vo.setOverlayPolicyJson(task.getOverlayPolicyJson());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setStatus(task.getStatus());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    /**
     * 将任务教学点关联实体转换为 VO。
     *
     * @param taskTeachingPoint 任务教学点关联实体。
     * @return 任务教学点关联 VO。
     */
    private TaskTeachingPointVO toTaskTeachingPointVO(TaskTeachingPoint taskTeachingPoint) {
        TaskTeachingPointVO vo = new TaskTeachingPointVO();
        vo.setId(taskTeachingPoint.getId());
        vo.setTenantId(taskTeachingPoint.getTenantId());
        vo.setTaskId(taskTeachingPoint.getTaskId());
        vo.setTeachingPointId(taskTeachingPoint.getTeachingPointId());
        vo.setRequiredFlag(taskTeachingPoint.getRequiredFlag());
        vo.setSequenceNo(taskTeachingPoint.getSequenceNo());
        vo.setStatus(taskTeachingPoint.getStatus());
        vo.setCreateTime(taskTeachingPoint.getCreateTime());
        vo.setUpdateTime(taskTeachingPoint.getUpdateTime());
        return vo;
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
