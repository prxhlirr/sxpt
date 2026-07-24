package com.sxpt.module.practice.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.practice.dto.ReportPracticeStepResultRequest;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.service.PracticeStepResultService;
import com.sxpt.module.practice.vo.PracticeStepResultVO;
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
 * 练习步骤结果接口。
 *
 * 业务功能：
 * 1. 提供 SDK 或评分流程上报单个练习步骤结果的入口。
 * 2. 提供前端按练习次数查看步骤结果的入口，用于定位卡点和展示练习过程。
 *
 * 关键流程：
 * 1. Controller 生成应用层主键并把 DTO 转换为实体。
 * 2. Service 负责幂等写入和排序查询，Controller 只返回对外 VO。
 */
@RestController
@RequestMapping("/api/v1/practice/step-results")
@ConditionalOnProperty(name = "sxpt.practice.step-result-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class PracticeStepResultController {

    private final PracticeStepResultService practiceStepResultService;

    public PracticeStepResultController(PracticeStepResultService practiceStepResultService) {
        this.practiceStepResultService = practiceStepResultService;
    }

    /**
     * 上报单个练习步骤结果。
     *
     * @param request 步骤结果上报请求。
     * @return 已保存或已更新的步骤结果。
     */
    @PostMapping("/report")
    public ApiResult<PracticeStepResultVO> report(@Valid @RequestBody ReportPracticeStepResultRequest request) {
        PracticeStepResult saved = practiceStepResultService.reportStepResult(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 按练习次数查询步骤结果。
     *
     * @param tenantId 租户 ID。
     * @param attemptId 练习次数 ID。
     * @return 步骤结果列表。
     */
    @GetMapping
    public ApiResult<List<PracticeStepResultVO>> listByAttempt(@RequestParam String tenantId,
                                                               @RequestParam String attemptId) {
        return ApiResult.success(toVOList(practiceStepResultService.listByAttempt(tenantId, attemptId)));
    }

    /**
     * 将上报请求转换为数据库实体。
     *
     * @param request 步骤结果上报请求。
     * @return 练习步骤结果实体。
     */
    private PracticeStepResult toEntity(ReportPracticeStepResultRequest request) {
        PracticeStepResult stepResult = new PracticeStepResult();
        stepResult.setId(generateId());
        stepResult.setTenantId(request.getTenantId());
        stepResult.setAttemptId(request.getAttemptId());
        stepResult.setExecutionId(request.getExecutionId());
        stepResult.setStudentId(request.getStudentId());
        stepResult.setTaskId(request.getTaskId());
        stepResult.setTaskStepId(request.getTaskStepId());
        stepResult.setStepCode(request.getStepCode());
        stepResult.setSequenceNo(request.getSequenceNo());
        stepResult.setResultStatus(request.getResultStatus());
        stepResult.setScore(request.getScore());
        stepResult.setMaxScore(request.getMaxScore());
        stepResult.setPassFlag(request.getPassFlag());
        stepResult.setStartTime(request.getStartTime());
        stepResult.setEndTime(request.getEndTime());
        stepResult.setErrorCount(request.getErrorCount());
        stepResult.setHintCount(request.getHintCount());
        stepResult.setRetryCount(request.getRetryCount());
        stepResult.setEvidenceJson(request.getEvidenceJson());
        stepResult.setFeedback(request.getFeedback());
        stepResult.setCreateBy(request.getCreateBy());
        return stepResult;
    }

    /**
     * 将实体列表转换为返回对象列表。
     *
     * @param stepResults 练习步骤结果实体列表。
     * @return 练习步骤结果返回对象列表。
     */
    private List<PracticeStepResultVO> toVOList(List<PracticeStepResult> stepResults) {
        List<PracticeStepResultVO> result = new ArrayList<>();
        for (PracticeStepResult stepResult : stepResults) {
            result.add(toVO(stepResult));
        }
        return result;
    }

    /**
     * 将实体转换为返回对象。
     *
     * @param stepResult 练习步骤结果实体。
     * @return 练习步骤结果返回对象。
     */
    private PracticeStepResultVO toVO(PracticeStepResult stepResult) {
        PracticeStepResultVO vo = new PracticeStepResultVO();
        vo.setId(stepResult.getId());
        vo.setTenantId(stepResult.getTenantId());
        vo.setAttemptId(stepResult.getAttemptId());
        vo.setExecutionId(stepResult.getExecutionId());
        vo.setStudentId(stepResult.getStudentId());
        vo.setTaskId(stepResult.getTaskId());
        vo.setTaskStepId(stepResult.getTaskStepId());
        vo.setStepCode(stepResult.getStepCode());
        vo.setSequenceNo(stepResult.getSequenceNo());
        vo.setResultStatus(stepResult.getResultStatus());
        vo.setScore(stepResult.getScore());
        vo.setMaxScore(stepResult.getMaxScore());
        vo.setPassFlag(stepResult.getPassFlag());
        vo.setStartTime(stepResult.getStartTime());
        vo.setEndTime(stepResult.getEndTime());
        vo.setDurationSeconds(stepResult.getDurationSeconds());
        vo.setErrorCount(stepResult.getErrorCount());
        vo.setHintCount(stepResult.getHintCount());
        vo.setRetryCount(stepResult.getRetryCount());
        vo.setEvidenceJson(stepResult.getEvidenceJson());
        vo.setFeedback(stepResult.getFeedback());
        vo.setStatus(stepResult.getStatus());
        vo.setCreateTime(stepResult.getCreateTime());
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
