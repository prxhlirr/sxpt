package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.execution.dto.StartTaskExecutionRequest;
import com.sxpt.module.execution.dto.SubmitTaskExecutionRequest;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.service.TaskExecutionService;
import com.sxpt.module.execution.vo.TaskExecutionVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 学生任务执行主入口接口。
 *
 * 业务功能：
 * 1. 为学生端提供开始任务、提交任务和查询执行记录的产品级入口。
 * 2. 隐藏 task_execution 数据表细节，让前端围绕 executionId 驱动后续上下文、轨迹和评分流程。
 *
 * 关键流程：
 * 1. 开始任务时由 Controller 生成执行主键并交给 Service 创建 RUNNING 主记录。
 * 2. 提交任务时由 Service 校验状态并流转为 SUBMITTED。
 */
@RestController
@RequestMapping("/api/v1/student/task-executions")
@ConditionalOnProperty(name = "sxpt.execution.task-execution-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class TaskExecutionController {

    private final TaskExecutionService taskExecutionService;

    public TaskExecutionController(TaskExecutionService taskExecutionService) {
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * 学生开始任务执行。
     *
     * @param request 学生开始任务执行请求。
     * @return 已创建的学生任务执行主记录。
     */
    @PostMapping("/start")
    public ApiResult<TaskExecutionVO> start(@Valid @RequestBody StartTaskExecutionRequest request) {
        TaskExecution saved = taskExecutionService.startExecution(toStartEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 学生提交任务执行。
     *
     * @param request 学生提交任务执行请求。
     * @return 已提交的学生任务执行主记录。
     */
    @PostMapping("/submit")
    public ApiResult<TaskExecutionVO> submit(@Valid @RequestBody SubmitTaskExecutionRequest request) {
        TaskExecution saved = taskExecutionService.submitExecution(
                request.getTenantId(), request.getExecutionId(), request.getOperatorId());
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询单次学生任务执行。
     *
     * @param executionId 执行记录 ID。
     * @param tenantId 租户 ID。
     * @return 学生任务执行主记录。
     */
    @GetMapping("/{executionId}")
    public ApiResult<TaskExecutionVO> getExecution(@PathVariable String executionId,
                                                   @RequestParam String tenantId) {
        return ApiResult.success(toVO(taskExecutionService.getExecution(tenantId, executionId)));
    }

    /**
     * 按学生和任务查询执行历史。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @return 学生任务执行主记录列表。
     */
    @GetMapping
    public ApiResult<List<TaskExecutionVO>> listByStudentAndTask(@RequestParam String tenantId,
                                                                 @RequestParam String studentId,
                                                                 @RequestParam String taskId) {
        return ApiResult.success(toVOList(
                taskExecutionService.listByStudentAndTask(tenantId, studentId, taskId)));
    }

    /**
     * 将开始请求转换为学生任务执行实体。
     *
     * @param request 学生开始任务执行请求。
     * @return 学生任务执行实体。
     */
    private TaskExecution toStartEntity(StartTaskExecutionRequest request) {
        TaskExecution execution = new TaskExecution();
        execution.setId(generateId());
        execution.setTenantId(request.getTenantId());
        execution.setTaskId(request.getTaskId());
        execution.setStudentId(request.getStudentId());
        execution.setConnectorSystemId(request.getConnectorSystemId());
        execution.setExecutionMode(request.getExecutionMode());
        execution.setSdkMode(request.getSdkMode());
        execution.setExecutionIdentityJson(request.getExecutionIdentityJson());
        execution.setCreateBy(request.getCreateBy());
        execution.setUpdateBy(request.getCreateBy());
        return execution;
    }

    /**
     * 将实体列表转换为前端返回对象列表。
     *
     * @param executions 学生任务执行实体列表。
     * @return 学生任务执行返回对象列表。
     */
    private List<TaskExecutionVO> toVOList(List<TaskExecution> executions) {
        List<TaskExecutionVO> result = new ArrayList<>();
        for (TaskExecution execution : executions) {
            result.add(toVO(execution));
        }
        return result;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param execution 学生任务执行实体。
     * @return 学生任务执行返回对象。
     */
    private TaskExecutionVO toVO(TaskExecution execution) {
        TaskExecutionVO vo = new TaskExecutionVO();
        vo.setId(execution.getId());
        vo.setTenantId(execution.getTenantId());
        vo.setTaskId(execution.getTaskId());
        vo.setStudentId(execution.getStudentId());
        vo.setConnectorSystemId(execution.getConnectorSystemId());
        vo.setExecutionMode(execution.getExecutionMode());
        vo.setSdkMode(execution.getSdkMode());
        vo.setExecutionIdentityStatus(execution.getExecutionIdentityStatus());
        vo.setStartTime(execution.getStartTime());
        vo.setEndTime(execution.getEndTime());
        vo.setExecutionStatus(execution.getExecutionStatus());
        vo.setScore(execution.getScore());
        vo.setResultSummary(execution.getResultSummary());
        vo.setStatus(execution.getStatus());
        vo.setCreateTime(execution.getCreateTime());
        vo.setUpdateTime(execution.getUpdateTime());
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
