package com.sxpt.module.practice.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.practice.dto.GeneratePracticeScoreSummaryRequest;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import com.sxpt.module.practice.service.PracticeScoreSummaryService;
import com.sxpt.module.practice.vo.PracticeScoreSummaryVO;
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
 * 练习过程分汇总接口。
 *
 * 业务功能：
 * 1. 提供教师手动触发练习过程分重算的接口。
 * 2. 提供教师端查询学生练习过程分汇总的接口。
 *
 * 关键流程：
 * 1. Controller 生成汇总记录主键并转换 DTO。
 * 2. Service 负责读取练习过程数据、计算指标并幂等写入汇总。
 */
@RestController
@RequestMapping("/api/v1/practice/score-summaries")
@ConditionalOnProperty(name = "sxpt.practice.score-summary-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class PracticeScoreSummaryController {

    private final PracticeScoreSummaryService practiceScoreSummaryService;

    public PracticeScoreSummaryController(PracticeScoreSummaryService practiceScoreSummaryService) {
        this.practiceScoreSummaryService = practiceScoreSummaryService;
    }

    /**
     * 生成或重算练习过程分汇总。
     *
     * @param request 汇总生成请求。
     * @return 已保存的过程分汇总。
     */
    @PostMapping("/generate")
    public ApiResult<PracticeScoreSummaryVO> generate(@Valid @RequestBody GeneratePracticeScoreSummaryRequest request) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        if (!user.hasAnyRole("ADMIN", "TEACHER")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        PracticeScoreSummary saved = practiceScoreSummaryService.generateSummary(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询练习过程分汇总。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 练习过程分汇总。
     */
    @GetMapping
    public ApiResult<PracticeScoreSummaryVO> getSummary(@RequestParam String tenantId,
                                                        @RequestParam String studentId,
                                                        @RequestParam String taskId,
                                                        @RequestParam(required = false) String teachingPointId) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        String scopedStudentId = user.hasAnyRole("STUDENT") ? user.getUserId() : studentId;
        return ApiResult.success(toVO(practiceScoreSummaryService.getSummary(
                user.getTenantId(), scopedStudentId, taskId, teachingPointId)));
    }

    /**
     * 将请求转换为汇总范围实体。
     *
     * @param request 汇总生成请求。
     * @return 练习过程分汇总实体。
     */
    private PracticeScoreSummary toEntity(GeneratePracticeScoreSummaryRequest request) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        PracticeScoreSummary summary = new PracticeScoreSummary();
        summary.setId(generateId());
        summary.setTenantId(user.getTenantId());
        summary.setStudentId(request.getStudentId());
        summary.setClassId(request.getClassId());
        summary.setCourseId(request.getCourseId());
        summary.setTaskId(request.getTaskId());
        summary.setTeachingPointId(request.getTeachingPointId());
        summary.setCreateBy(user.getUserId());
        return summary;
    }

    /**
     * 将实体转换为返回对象。
     *
     * @param summary 练习过程分汇总实体。
     * @return 练习过程分汇总返回对象。
     */
    private PracticeScoreSummaryVO toVO(PracticeScoreSummary summary) {
        PracticeScoreSummaryVO vo = new PracticeScoreSummaryVO();
        vo.setId(summary.getId());
        vo.setTenantId(summary.getTenantId());
        vo.setStudentId(summary.getStudentId());
        vo.setClassId(summary.getClassId());
        vo.setCourseId(summary.getCourseId());
        vo.setTaskId(summary.getTaskId());
        vo.setTeachingPointId(summary.getTeachingPointId());
        vo.setPracticeCount(summary.getPracticeCount());
        vo.setCompleteCount(summary.getCompleteCount());
        vo.setBestScore(summary.getBestScore());
        vo.setLastScore(summary.getLastScore());
        vo.setAvgScore(summary.getAvgScore());
        vo.setCompletionRate(summary.getCompletionRate());
        vo.setFinalPracticeScore(summary.getFinalPracticeScore());
        vo.setScorePolicy(summary.getScorePolicy());
        vo.setWeakStepJson(summary.getWeakStepJson());
        vo.setSummaryTime(summary.getSummaryTime());
        vo.setStatus(summary.getStatus());
        vo.setCreateTime(summary.getCreateTime());
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
