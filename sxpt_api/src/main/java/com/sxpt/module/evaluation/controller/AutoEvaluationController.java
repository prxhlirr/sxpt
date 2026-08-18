package com.sxpt.module.evaluation.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.evaluation.dto.GenerateAutoEvaluationRequest;
import com.sxpt.module.evaluation.dto.ReviewEvaluationResultRequest;
import com.sxpt.module.evaluation.entity.EvaluationResult;
import com.sxpt.module.evaluation.service.AutoEvaluationService;
import com.sxpt.module.evaluation.vo.EvaluationResultVO;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.service.TaskExecutionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

/**
 * 自动评分接口。
 *
 * 业务功能：
 * 1. 提供提交后或教师手动触发自动评分的入口。
 * 2. 提供教师端查询自动评分结果的入口。
 *
 * 关键流程：
 * 1. Controller 生成应用层主键并转换 DTO。
 * 2. Service 根据评分项和执行轨迹计算自动分并返回评价结果。
 */
@RestController
@RequestMapping("/api/v1/evaluation/results")
@ConditionalOnProperty(name = "sxpt.evaluation.auto-evaluation-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class AutoEvaluationController {

    private final AutoEvaluationService autoEvaluationService;
    private final TaskExecutionService taskExecutionService;

    public AutoEvaluationController(AutoEvaluationService autoEvaluationService,
                                    TaskExecutionService taskExecutionService) {
        this.autoEvaluationService = autoEvaluationService;
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * 生成或重算自动评分结果。
     *
     * @param request 自动评分生成请求。
     * @return 已保存的评分结果。
     */
    @PostMapping("/auto-generate")
    public ApiResult<EvaluationResultVO> generate(@Valid @RequestBody GenerateAutoEvaluationRequest request) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        requireExecutionAccess(user, request.getExecutionId());
        EvaluationResult saved = autoEvaluationService.generateAutoEvaluation(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询自动评分结果。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @return 评分结果。
     */
    @GetMapping
    public ApiResult<EvaluationResultVO> getResult(@RequestParam String tenantId,
                                                   @RequestParam String executionId,
                                                   @RequestParam String evaluationRuleId) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        requireExecutionAccess(user, executionId);
        return ApiResult.success(toVO(autoEvaluationService.getEvaluationResult(
                user.getTenantId(), executionId, evaluationRuleId)));
    }

    /**
     * 提交教师复核结果。
     *
     * @param request 教师复核请求。
     * @return 已复核的评分结果。
     */
    @PostMapping("/review")
    public ApiResult<EvaluationResultVO> review(@Valid @RequestBody ReviewEvaluationResultRequest request) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        if (!user.hasAnyRole("ADMIN", "TEACHER")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        EvaluationResult saved = autoEvaluationService.reviewEvaluationResult(toReviewEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 将请求转换为评分结果范围实体。
     *
     * @param request 自动评分生成请求。
     * @return 评分结果实体。
     */
    private EvaluationResult toEntity(GenerateAutoEvaluationRequest request) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        EvaluationResult result = new EvaluationResult();
        result.setId(generateId());
        result.setTenantId(user.getTenantId());
        result.setExecutionId(request.getExecutionId());
        result.setEvaluationRuleId(request.getEvaluationRuleId());
        result.setCreateBy(user.getUserId());
        return result;
    }

    private void requireExecutionAccess(CurrentUserContext.CurrentUser user, String executionId) {
        if (user.hasAnyRole("ADMIN", "TEACHER")) {
            return;
        }
        TaskExecution execution = taskExecutionService.getExecution(user.getTenantId(), executionId);
        if (!user.getUserId().equals(execution.getStudentId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
    }

    /**
     * 将复核请求转换为评分结果实体。
     *
     * @param request 教师复核请求。
     * @return 仅包含复核范围和人工分的实体。
     */
    private EvaluationResult toReviewEntity(ReviewEvaluationResultRequest request) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        EvaluationResult result = new EvaluationResult();
        result.setTenantId(user.getTenantId());
        result.setExecutionId(request.getExecutionId());
        result.setEvaluationRuleId(request.getEvaluationRuleId());
        result.setManualScore(request.getManualScore());
        result.setReviewReason(request.getReviewReason().trim());
        result.setReviewedBy(user.getUserId());
        return result;
    }

    /**
     * 将实体转换为返回对象。
     *
     * @param result 评分结果实体。
     * @return 评分结果返回对象。
     */
    private EvaluationResultVO toVO(EvaluationResult result) {
        EvaluationResultVO vo = new EvaluationResultVO();
        vo.setId(result.getId());
        vo.setTenantId(result.getTenantId());
        vo.setExecutionId(result.getExecutionId());
        vo.setEvaluationRuleId(result.getEvaluationRuleId());
        vo.setAutoScore(result.getAutoScore());
        vo.setManualScore(result.getManualScore());
        vo.setFinalScore(result.getFinalScore());
        vo.setEvaluationSummary(result.getEvaluationSummary());
        vo.setEvidenceJson(result.getEvidenceJson());
        vo.setReviewedBy(result.getReviewedBy());
        vo.setReviewedTime(result.getReviewedTime());
        vo.setReviewStatus(result.getReviewStatus());
        vo.setReviewReason(result.getReviewReason());
        vo.setArchiveStatus(result.getArchiveStatus());
        vo.setArchiveTime(result.getArchiveTime());
        vo.setExpireTime(result.getExpireTime());
        vo.setEvaluationStatus(result.getEvaluationStatus());
        vo.setStatus(result.getStatus());
        vo.setCreateTime(result.getCreateTime());
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
