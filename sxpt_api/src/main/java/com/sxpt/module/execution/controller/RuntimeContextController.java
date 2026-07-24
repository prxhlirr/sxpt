package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * SDK 运行上下文接口。
 *
 * 业务功能：
 * 1. 为 SDK 提供任务、步骤、资源、遮罩和评分规则的运行上下文入口。
 * 2. 当前 MVP 版本直接返回已固化的 task_execution_context 快照，保证运行态稳定。
 *
 * 关键流程：
 * 1. SDK 使用租户 ID 和任务执行 ID 请求上下文，可附带任务 ID、学生 ID 做边界校验。
 * 2. Controller 触发参数校验后调用 Service。
 * 3. Service 返回 SDK 专用 VO，SDK 按快照 JSON 初始化学习、练习或考试模式。
 */
@RestController
@RequestMapping("/api/v1/sdk")
@Validated
@ConditionalOnProperty(name = "sxpt.execution.runtime-context-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class RuntimeContextController {

    private final RuntimeContextService runtimeContextService;

    public RuntimeContextController(RuntimeContextService runtimeContextService) {
        this.runtimeContextService = runtimeContextService;
    }

    /**
     * 获取 SDK 运行上下文。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return SDK 运行上下文。
     */
    @GetMapping("/runtime-context")
    public ApiResult<RuntimeContextVO> getRuntimeContext(@NotBlank @RequestParam(required = false) String tenantId,
                                                         @NotBlank @RequestParam(required = false) String executionId,
                                                         @RequestParam(required = false) String taskId,
                                                         @RequestParam(required = false) String studentId) {
        return ApiResult.success(runtimeContextService.getRuntimeContext(
                buildRequest(tenantId, executionId, taskId, studentId)));
    }

    /**
     * 从 query 参数组装 SDK 运行上下文查询请求。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return SDK 运行上下文查询请求。
     */
    private RuntimeContextQueryRequest buildRequest(String tenantId, String executionId, String taskId,
                                                    String studentId) {
        RuntimeContextQueryRequest request = new RuntimeContextQueryRequest();
        request.setTenantId(tenantId);
        request.setExecutionId(executionId);
        request.setTaskId(taskId);
        request.setStudentId(studentId);
        return request;
    }
}
