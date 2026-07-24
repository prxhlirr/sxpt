package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.execution.dto.LearningContextQueryRequest;
import com.sxpt.module.execution.service.LearningContextService;
import com.sxpt.module.execution.vo.LearningContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * 学习模式上下文接口。
 *
 * 业务功能：
 * 1. 为 SDK 学习模式提供步骤说明、遮罩策略、教学点和资源高亮上下文。
 * 2. 基于已固化 runtime-context 返回学习视图，保证学习过程不受后续任务配置修改影响。
 *
 * 关键流程：
 * 1. SDK 使用租户 ID 和任务执行 ID 请求学习上下文。
 * 2. Service 校验 runtime-context 的 sdkMode 必须为 LEARNING。
 * 3. SDK 根据返回的学习上下文展示引导、遮罩和资源高亮。
 */
@RestController
@RequestMapping("/api/v1/sdk/learning")
@Validated
@ConditionalOnProperty(name = "sxpt.execution.learning-context-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class LearningContextController {

    private final LearningContextService learningContextService;

    public LearningContextController(LearningContextService learningContextService) {
        this.learningContextService = learningContextService;
    }

    /**
     * 获取学习模式上下文。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 学习模式上下文。
     */
    @GetMapping("/context")
    public ApiResult<LearningContextVO> getLearningContext(@NotBlank @RequestParam(required = false) String tenantId,
                                                           @NotBlank @RequestParam(required = false) String executionId,
                                                           @RequestParam(required = false) String taskId,
                                                           @RequestParam(required = false) String studentId) {
        return ApiResult.success(learningContextService.getLearningContext(
                buildRequest(tenantId, executionId, taskId, studentId)));
    }

    /**
     * 从 query 参数组装学习模式上下文查询请求。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 学习模式上下文查询请求。
     */
    private LearningContextQueryRequest buildRequest(String tenantId, String executionId, String taskId,
                                                     String studentId) {
        LearningContextQueryRequest request = new LearningContextQueryRequest();
        request.setTenantId(tenantId);
        request.setExecutionId(executionId);
        request.setTaskId(taskId);
        request.setStudentId(studentId);
        return request;
    }
}
