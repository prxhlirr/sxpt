package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.execution.dto.ExamContextQueryRequest;
import com.sxpt.module.execution.service.ExamContextService;
import com.sxpt.module.execution.vo.ExamContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;

/**
 * 考试模式上下文接口。
 *
 * 业务功能：
 * 1. 为 SDK 考试模式提供考试上下文、评分快照、资源定位和静默采集策略。
 * 2. 基于已固化 runtime-context 返回考试视图，防止考试过程出现提示和强引导。
 *
 * 关键流程：
 * 1. SDK 使用租户 ID 和任务执行 ID 请求考试上下文。
 * 2. Service 校验 runtime-context 的 sdkMode 必须为 EXAM。
 * 3. SDK 根据返回的考试上下文执行无提示、静默采集和后续评分证据记录流程。
 */
@RestController
@RequestMapping("/api/v1/sdk/exam")
@Validated
@ConditionalOnProperty(name = "sxpt.execution.exam-context-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class ExamContextController {

    private final ExamContextService examContextService;

    public ExamContextController(ExamContextService examContextService) {
        this.examContextService = examContextService;
    }

    /**
     * 获取考试模式上下文。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 考试模式上下文。
     */
    @GetMapping("/context")
    public ApiResult<ExamContextVO> getExamContext(@NotBlank @RequestParam(required = false) String tenantId,
                                                   @NotBlank @RequestParam(required = false) String executionId,
                                                   @RequestParam(required = false) String taskId,
                                                   @RequestParam(required = false) String studentId) {
        return ApiResult.success(examContextService.getExamContext(
                buildRequest(tenantId, executionId, taskId, studentId)));
    }

    /**
     * 从 query 参数组装考试模式上下文查询请求。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @param taskId 任务 ID，可为空。
     * @param studentId 学生 ID，可为空。
     * @return 考试模式上下文查询请求。
     */
    private ExamContextQueryRequest buildRequest(String tenantId, String executionId, String taskId,
                                                 String studentId) {
        ExamContextQueryRequest request = new ExamContextQueryRequest();
        request.setTenantId(tenantId);
        request.setExecutionId(executionId);
        request.setTaskId(taskId);
        request.setStudentId(studentId);
        return request;
    }
}
