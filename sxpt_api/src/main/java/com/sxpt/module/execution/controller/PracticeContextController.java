package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.execution.dto.PracticeContextQueryRequest;
import com.sxpt.module.execution.service.PracticeContextService;
import com.sxpt.module.execution.vo.PracticeContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * 练习模式上下文接口。
 *
 * 业务功能：
 * 1. 为 SDK 练习模式提供弱提示、资源定位、练习上下文和重开策略。
 * 2. 基于已固化 runtime-context 返回练习视图，保证练习过程不受后续任务配置修改影响。
 *
 * 关键流程：
 * 1. SDK 使用租户 ID 和任务执行 ID 请求练习上下文。
 * 2. Service 校验 runtime-context 的 sdkMode 必须为 PRACTICE。
 * 3. SDK 根据返回的练习上下文执行弱提示、错误记录和重开练习流程。
 */
@RestController
@RequestMapping("/api/v1/sdk/practice")
@Validated
@ConditionalOnProperty(name = "sxpt.execution.practice-context-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class PracticeContextController {

    private final PracticeContextService practiceContextService;

    public PracticeContextController(PracticeContextService practiceContextService) {
        this.practiceContextService = practiceContextService;
    }

    /**
     * 获取练习模式上下文。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 练习模式上下文。
     */
    @GetMapping("/context")
    public ApiResult<PracticeContextVO> getPracticeContext(@NotBlank @RequestParam(required = false) String tenantId,
                                                           @NotBlank @RequestParam(required = false) String executionId,
                                                           @RequestParam(required = false) String taskId,
                                                           @RequestParam(required = false) String studentId) {
        return ApiResult.success(practiceContextService.getPracticeContext(
                buildRequest(tenantId, executionId, taskId, studentId)));
    }

    /**
     * 从 query 参数组装练习模式上下文查询请求。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 练习模式上下文查询请求。
     */
    private PracticeContextQueryRequest buildRequest(String tenantId, String executionId, String taskId,
                                                     String studentId) {
        PracticeContextQueryRequest request = new PracticeContextQueryRequest();
        request.setTenantId(tenantId);
        request.setExecutionId(executionId);
        request.setTaskId(taskId);
        request.setStudentId(studentId);
        return request;
    }
}
