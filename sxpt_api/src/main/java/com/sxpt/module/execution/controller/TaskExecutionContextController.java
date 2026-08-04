package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.execution.dto.CreateTaskExecutionContextRequest;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.service.TaskExecutionContextService;
import com.sxpt.module.execution.vo.TaskExecutionContextVO;
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
 * 任务执行上下文快照接口。
 *
 * 业务功能：
 * 1. 提供学生进入任务时创建版本快照的入口。
 * 2. 提供按执行记录、任务或学生查询快照的入口，供 SDK 初始化和历史排查使用。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截缺少执行记录或上下文 JSON 的无效请求。
 * 2. 将 DTO 转换为实体并生成应用层主键。
 * 3. 调用 Service 写入快照，再转换为 VO 返回调用方。
 */
@RestController
@RequestMapping("/api/v1/tasks/execution-contexts")
@ConditionalOnProperty(name = "sxpt.execution.context-controller.enabled", havingValue = "true", matchIfMissing = true)
public class TaskExecutionContextController {

    private final TaskExecutionContextService taskExecutionContextService;

    public TaskExecutionContextController(TaskExecutionContextService taskExecutionContextService) {
        this.taskExecutionContextService = taskExecutionContextService;
    }

    /**
     * 创建任务执行上下文快照。
     *
     * @param request 创建任务执行上下文快照请求。
     * @return 已保存的任务执行上下文快照。
     */
    @PostMapping("/create")
    public ApiResult<TaskExecutionContextVO> createContext(
            @Valid @RequestBody CreateTaskExecutionContextRequest request) {
        return ApiResult.success(toVO(taskExecutionContextService.createContext(toEntity(request))));
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
    @GetMapping
    public ApiResult<List<TaskExecutionContextVO>> listContexts(@RequestParam String tenantId,
                                                                @RequestParam(required = false) String executionId,
                                                                @RequestParam(required = false) String taskId,
                                                                @RequestParam(required = false) String studentId) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        String scopedStudentId = user.hasAnyRole("STUDENT") ? user.getUserId() : studentId;
        return ApiResult.success(toVOList(taskExecutionContextService.listContexts(
                user.getTenantId(), executionId, taskId, scopedStudentId)));
    }

    /**
     * 将创建请求转换为实体。
     *
     * @param request 创建任务执行上下文快照请求。
     * @return 任务执行上下文快照实体。
     */
    private TaskExecutionContext toEntity(CreateTaskExecutionContextRequest request) {
        TaskExecutionContext context = new TaskExecutionContext();
        context.setId(generateId());
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        context.setTenantId(user.getTenantId());
        context.setExecutionId(request.getExecutionId());
        context.setTaskId(request.getTaskId());
        context.setStudentId(user.hasAnyRole("STUDENT") ? user.getUserId() : request.getStudentId());
        context.setSdkMode(request.getSdkMode());
        context.setContextJson(request.getContextJson());
        context.setOverlayPolicyJson(request.getOverlayPolicyJson());
        context.setTeachingPointSnapshotJson(request.getTeachingPointSnapshotJson());
        context.setResourceSnapshotJson(request.getResourceSnapshotJson());
        context.setEvaluationSnapshotJson(request.getEvaluationSnapshotJson());
        context.setExpireTime(request.getExpireTime());
        context.setCreateBy(user.getUserId());
        context.setUpdateBy(user.getUserId());
        return context;
    }

    /**
     * 将实体列表转换为 VO 列表。
     *
     * @param contexts 任务执行上下文快照实体列表。
     * @return 任务执行上下文快照 VO 列表。
     */
    private List<TaskExecutionContextVO> toVOList(List<TaskExecutionContext> contexts) {
        List<TaskExecutionContextVO> result = new ArrayList<>();
        for (TaskExecutionContext context : contexts) {
            result.add(toVO(context));
        }
        return result;
    }

    /**
     * 将实体转换为 VO。
     *
     * @param context 任务执行上下文快照实体。
     * @return 任务执行上下文快照 VO。
     */
    private TaskExecutionContextVO toVO(TaskExecutionContext context) {
        TaskExecutionContextVO vo = new TaskExecutionContextVO();
        vo.setId(context.getId());
        vo.setTenantId(context.getTenantId());
        vo.setExecutionId(context.getExecutionId());
        vo.setTaskId(context.getTaskId());
        vo.setStudentId(context.getStudentId());
        vo.setSdkMode(context.getSdkMode());
        vo.setContextJson(context.getContextJson());
        vo.setOverlayPolicyJson(context.getOverlayPolicyJson());
        vo.setTeachingPointSnapshotJson(context.getTeachingPointSnapshotJson());
        vo.setResourceSnapshotJson(context.getResourceSnapshotJson());
        vo.setEvaluationSnapshotJson(context.getEvaluationSnapshotJson());
        vo.setArchiveStatus(context.getArchiveStatus());
        vo.setArchiveTime(context.getArchiveTime());
        vo.setExpireTime(context.getExpireTime());
        vo.setStatus(context.getStatus());
        vo.setCreateTime(context.getCreateTime());
        vo.setUpdateTime(context.getUpdateTime());
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
