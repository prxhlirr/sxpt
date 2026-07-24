package com.sxpt.module.practice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.mapper.PracticeAttemptMapper;
import com.sxpt.module.practice.mapper.PracticeScoreSummaryMapper;
import com.sxpt.module.practice.mapper.PracticeStepResultMapper;
import com.sxpt.module.practice.service.PracticeScoreSummaryService;
import com.sxpt.module.practice.service.impl.PracticeScoreSummaryServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 练习过程分汇总服务测试。
 *
 * 业务功能：
 * 1. 验证过程分汇总按练习次数和步骤结果计算。
 * 2. 验证同一学生任务教学点重复汇总时更新已有记录。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，隔离数据库。
 * 2. 直接调用 Service，覆盖空练习、加权计算、幂等更新和查询异常。
 */
class PracticeScoreSummaryServiceImplTests {

    private final PracticeScoreSummaryMapper summaryMapper = mock(PracticeScoreSummaryMapper.class);

    private final PracticeAttemptMapper attemptMapper = mock(PracticeAttemptMapper.class);

    private final PracticeStepResultMapper stepResultMapper = mock(PracticeStepResultMapper.class);

    private final PracticeScoreSummaryService service = new PracticeScoreSummaryServiceImpl(
            summaryMapper, attemptMapper, stepResultMapper, new ObjectMapper());

    /**
     * 验证有历史练习时按 MVP 加权策略生成汇总。
     */
    @Test
    void generateSummaryShouldCalculateWeightedScoreAndWeakSteps() {
        when(attemptMapper.selectList(any())).thenReturn(Arrays.asList(
                buildAttempt("attempt_001", 1L, "COMPLETED", "70.00"),
                buildAttempt("attempt_002", 2L, "RUNNING", "90.00")));
        when(stepResultMapper.selectList(any())).thenReturn(Collections.singletonList(buildFailedStep()));
        PracticeScoreSummary scope = buildScope();

        PracticeScoreSummary result = service.generateSummary(scope);

        assertEquals(scope.getId(), result.getId());
        assertEquals(Long.valueOf(2), result.getPracticeCount());
        assertEquals(Long.valueOf(1), result.getCompleteCount());
        assertEquals(new BigDecimal("90.00"), result.getBestScore());
        assertEquals(new BigDecimal("90.00"), result.getLastScore());
        assertEquals(new BigDecimal("80.00"), result.getAvgScore());
        assertEquals(new BigDecimal("0.5000"), result.getCompletionRate());
        assertEquals(new BigDecimal("82.00"), result.getFinalPracticeScore());
        assertEquals("WEIGHTED", result.getScorePolicy());
        assertTrue(result.getWeakStepJson().contains("step_001"));
        assertNotNull(result.getSummaryTime());
        assertEquals("ACTIVE", result.getStatus());
        verify(summaryMapper).insert(result);
    }

    /**
     * 验证没有练习记录时仍生成零值汇总。
     */
    @Test
    void generateSummaryShouldCreateZeroSummaryWhenNoAttempt() {
        when(attemptMapper.selectList(any())).thenReturn(Collections.emptyList());
        PracticeScoreSummary scope = buildScope();

        PracticeScoreSummary result = service.generateSummary(scope);

        assertEquals(Long.valueOf(0), result.getPracticeCount());
        assertEquals(Long.valueOf(0), result.getCompleteCount());
        assertEquals(new BigDecimal("0.0000"), result.getCompletionRate());
        assertEquals(new BigDecimal("0.00"), result.getFinalPracticeScore());
        assertEquals("[]", result.getWeakStepJson());
        verify(stepResultMapper, times(0)).selectList(any());
        verify(summaryMapper).insert(result);
    }

    /**
     * 验证已有汇总时更新原记录而不是插入新记录。
     */
    @Test
    void generateSummaryShouldUpdateExistingSummary() {
        PracticeScoreSummary existed = buildScope();
        existed.setId("summary_old");
        when(attemptMapper.selectList(any())).thenReturn(Collections.singletonList(
                buildAttempt("attempt_001", 1L, "COMPLETED", "100.00")));
        when(stepResultMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(summaryMapper.selectOne(any())).thenReturn(existed);

        PracticeScoreSummary result = service.generateSummary(buildScope());

        assertSame(existed, result);
        assertEquals(new BigDecimal("100.00"), result.getFinalPracticeScore());
        verify(summaryMapper).updateById(existed);
        verify(summaryMapper, times(0)).insert(any());
    }

    /**
     * 验证缺少学生 ID 时拒绝生成汇总。
     */
    @Test
    void generateSummaryShouldRejectMissingStudentId() {
        PracticeScoreSummary scope = buildScope();
        scope.setStudentId(null);

        assertThrows(BusinessException.class, () -> service.generateSummary(scope));
        verify(attemptMapper, times(0)).selectList(any());
    }

    /**
     * 验证查询不到汇总时抛出业务异常。
     */
    @Test
    void getSummaryShouldRejectMissingData() {
        assertThrows(BusinessException.class,
                () -> service.getSummary("tenant_001", "student_001", "task_001", "tp_001"));
    }

    /**
     * 构造汇总范围。
     *
     * @return 练习过程分汇总范围。
     */
    private PracticeScoreSummary buildScope() {
        PracticeScoreSummary summary = new PracticeScoreSummary();
        summary.setId("summary_001");
        summary.setTenantId("tenant_001");
        summary.setStudentId("student_001");
        summary.setClassId("class_001");
        summary.setCourseId("course_001");
        summary.setTaskId("task_001");
        summary.setTeachingPointId("tp_001");
        summary.setCreateBy("teacher_001");
        return summary;
    }

    /**
     * 构造练习次数。
     *
     * @param id 练习次数 ID。
     * @param attemptNo 练习序号。
     * @param status 练习状态。
     * @param score 得分。
     * @return 练习次数实体。
     */
    private PracticeAttempt buildAttempt(String id, Long attemptNo, String status, String score) {
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId(id);
        attempt.setTenantId("tenant_001");
        attempt.setStudentId("student_001");
        attempt.setTaskId("task_001");
        attempt.setTeachingPointId("tp_001");
        attempt.setAttemptNo(attemptNo);
        attempt.setAttemptStatus(status);
        attempt.setScore(new BigDecimal(score));
        attempt.setCreateTime(LocalDateTime.now());
        return attempt;
    }

    /**
     * 构造失败步骤结果。
     *
     * @return 练习步骤结果实体。
     */
    private PracticeStepResult buildFailedStep() {
        PracticeStepResult stepResult = new PracticeStepResult();
        stepResult.setAttemptId("attempt_002");
        stepResult.setTaskStepId("step_001");
        stepResult.setStepCode("SUBMIT_FORM");
        stepResult.setSequenceNo(1L);
        stepResult.setResultStatus("FAILED");
        stepResult.setPassFlag(Boolean.FALSE);
        stepResult.setErrorCount(2L);
        stepResult.setDurationSeconds(120L);
        return stepResult;
    }
}
