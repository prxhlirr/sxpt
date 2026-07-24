package com.sxpt.module.practice;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.mapper.PracticeStepResultMapper;
import com.sxpt.module.practice.service.PracticeStepResultService;
import com.sxpt.module.practice.service.impl.PracticeStepResultServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 练习步骤结果服务测试。
 *
 * 业务功能：
 * 1. 验证步骤结果首次上报会插入并补齐默认值。
 * 2. 验证同一练习同一步骤重复上报会更新已有结果，避免 SDK 重试造成重复数据。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，覆盖插入、更新、非法状态和查询排序入口。
 */
class PracticeStepResultServiceImplTests {

    private final PracticeStepResultMapper mapper = mock(PracticeStepResultMapper.class);

    private final PracticeStepResultService service = new PracticeStepResultServiceImpl(mapper);

    /**
     * 验证首次上报步骤结果时插入记录并补齐默认字段。
     */
    @Test
    void reportStepResultShouldInsertAndFillDefaults() {
        PracticeStepResult stepResult = buildStepResult();

        PracticeStepResult saved = service.reportStepResult(stepResult);

        assertSame(stepResult, saved);
        assertEquals(Long.valueOf(930), saved.getDurationSeconds());
        assertEquals(Long.valueOf(0), saved.getRetryCount());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        verify(mapper).selectOne(any());
        verify(mapper).insert(saved);
    }

    /**
     * 验证同一步骤重复上报时更新已有记录。
     */
    @Test
    void reportStepResultShouldUpdateExistingStepResult() {
        PracticeStepResult existed = buildStepResult();
        existed.setId("step_result_old");
        existed.setResultStatus("FAILED");
        existed.setScore(new BigDecimal("0.00"));
        when(mapper.selectOne(any())).thenReturn(existed);
        PracticeStepResult update = buildStepResult();
        update.setResultStatus("PASSED");
        update.setScore(new BigDecimal("10.00"));

        PracticeStepResult result = service.reportStepResult(update);

        assertSame(existed, result);
        assertEquals("PASSED", result.getResultStatus());
        assertEquals(new BigDecimal("10.00"), result.getScore());
        assertEquals(Long.valueOf(930), result.getDurationSeconds());
        verify(mapper).updateById(existed);
        verify(mapper, times(0)).insert(update);
    }

    /**
     * 验证非法步骤结果状态会被拒绝。
     */
    @Test
    void reportStepResultShouldRejectInvalidStatus() {
        PracticeStepResult stepResult = buildStepResult();
        stepResult.setResultStatus("DONE");

        assertThrows(BusinessException.class, () -> service.reportStepResult(stepResult));
        verify(mapper, times(0)).insert(stepResult);
    }

    /**
     * 验证结束时间早于开始时间会被拒绝，避免产生负耗时。
     */
    @Test
    void reportStepResultShouldRejectNegativeDuration() {
        PracticeStepResult stepResult = buildStepResult();
        stepResult.setEndTime(LocalDateTime.of(2026, 7, 16, 14, 0));
        stepResult.setStartTime(LocalDateTime.of(2026, 7, 16, 14, 15));

        assertThrows(BusinessException.class, () -> service.reportStepResult(stepResult));
        verify(mapper, times(0)).insert(stepResult);
    }

    /**
     * 验证起止时间不完整时不计算耗时。
     */
    @Test
    void reportStepResultShouldAllowMissingTimeRange() {
        PracticeStepResult stepResult = buildStepResult();
        stepResult.setStartTime(null);
        stepResult.setEndTime(null);

        PracticeStepResult saved = service.reportStepResult(stepResult);

        assertNull(saved.getDurationSeconds());
        verify(mapper).insert(saved);
    }

    /**
     * 验证按练习次数查询时返回 Mapper 查询结果。
     */
    @Test
    void listByAttemptShouldReturnMapperResult() {
        PracticeStepResult stepResult = buildStepResult();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(stepResult));

        List<PracticeStepResult> result = service.listByAttempt("tenant_001", "attempt_001");

        assertEquals(1, result.size());
        assertSame(stepResult, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造合法练习步骤结果。
     *
     * @return 练习步骤结果实体。
     */
    private PracticeStepResult buildStepResult() {
        PracticeStepResult stepResult = new PracticeStepResult();
        stepResult.setId("step_result_001");
        stepResult.setTenantId("tenant_001");
        stepResult.setAttemptId("attempt_001");
        stepResult.setExecutionId("execution_001");
        stepResult.setStudentId("student_001");
        stepResult.setTaskId("task_001");
        stepResult.setTaskStepId("step_001");
        stepResult.setStepCode("SUBMIT_FORM");
        stepResult.setSequenceNo(1L);
        stepResult.setResultStatus("PASSED");
        stepResult.setScore(new BigDecimal("10.00"));
        stepResult.setMaxScore(new BigDecimal("10.00"));
        stepResult.setPassFlag(Boolean.TRUE);
        stepResult.setStartTime(LocalDateTime.of(2026, 7, 16, 14, 0));
        stepResult.setEndTime(LocalDateTime.of(2026, 7, 16, 14, 15, 30));
        stepResult.setErrorCount(0L);
        stepResult.setHintCount(1L);
        stepResult.setEvidenceJson("{\"traceId\":\"trace_001\"}");
        stepResult.setFeedback("步骤完成");
        stepResult.setCreateBy("student_001");
        return stepResult;
    }
}
