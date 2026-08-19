package com.sxpt.module.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.evaluation.assertion.ExternalApiResultAssertionStrategy;
import com.sxpt.module.evaluation.assertion.FieldEqualsAssertionStrategy;
import com.sxpt.module.evaluation.assertion.TraceExistsAssertionStrategy;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationResult;
import com.sxpt.module.evaluation.mapper.EvaluationResultMapper;
import com.sxpt.module.evaluation.service.AutoEvaluationService;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.evaluation.service.impl.AutoEvaluationServiceImpl;
import com.sxpt.module.execution.entity.ExecutionTrace;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.mapper.TaskExecutionMapper;
import com.sxpt.module.execution.service.ExecutionTraceService;
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
 * 自动评分服务测试。
 *
 * 业务功能：
 * 1. 验证 TRACE_EXISTS 评分项能基于执行轨迹生成自动分。
 * 2. 验证重复评分会更新已有 evaluation_result。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代评分配置服务、轨迹服务和 Mapper。
 * 2. 直接调用 Service，覆盖命中、未支持断言、幂等更新和查询异常。
 */
class AutoEvaluationServiceImplTests {

    private final EvaluationResultMapper resultMapper = mock(EvaluationResultMapper.class);

    private final EvaluationConfigService evaluationConfigService = mock(EvaluationConfigService.class);

    private final ExecutionTraceService executionTraceService = mock(ExecutionTraceService.class);

    private final TaskExecutionMapper taskExecutionMapper = mock(TaskExecutionMapper.class);

    private final AutoEvaluationService service = new AutoEvaluationServiceImpl(
            resultMapper,
            evaluationConfigService,
            executionTraceService,
            taskExecutionMapper,
            Arrays.asList(new TraceExistsAssertionStrategy(),
                    new FieldEqualsAssertionStrategy(new ObjectMapper()),
                    new ExternalApiResultAssertionStrategy()),
            new ObjectMapper());

    /**
     * 验证 TRACE_EXISTS 命中时累加评分项分数。
     */
    @Test
    void generateAutoEvaluationShouldScoreMatchedTraceExistsItem() {
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Arrays.asList(buildTraceExistsItem(), buildUnsupportedItem()));
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.singletonList(buildTrace()));
        EvaluationResult scope = buildScope();

        EvaluationResult result = service.generateAutoEvaluation(scope);

        assertEquals(scope.getId(), result.getId());
        assertEquals(new BigDecimal("10.00"), result.getAutoScore());
        assertEquals(new BigDecimal("10.00"), result.getFinalScore());
        assertEquals("AUTO_EVALUATED", result.getEvaluationStatus());
        assertEquals("NONE", result.getArchiveStatus());
        assertEquals("ACTIVE", result.getStatus());
        assertTrue(result.getEvidenceJson().contains("ITEM_SUBMIT"));
        assertTrue(result.getEvidenceJson().contains("UNSUPPORTED_ASSERTION"));
        assertNotNull(result.getCreateTime());
        verify(resultMapper).insert(result);
    }

    /**
     * 验证修复前已按 CLICK 入库、但幂等键明确标识练习完成的轨迹仍可参与评分。
     */
    @Test
    void generateAutoEvaluationShouldScoreLegacyPracticeCompletionTrace() {
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildTraceExistsItem()));
        ExecutionTrace legacyTrace = buildTrace();
        legacyTrace.setTraceType("CLICK");
        legacyTrace.setClientTraceId("execution_001:step_001:practice-completed");
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.singletonList(legacyTrace));

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertEquals(new BigDecimal("10.00"), result.getAutoScore());
    }

    /**
     * 验证没有命中轨迹时不给分。
     */
    @Test
    void generateAutoEvaluationShouldNoScoreWhenTraceMissing() {
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildTraceExistsItem()));
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.emptyList());

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertEquals(new BigDecimal("0.00"), result.getAutoScore());
        assertTrue(result.getEvidenceJson().contains("NO_TRACE"));
        verify(resultMapper).insert(result);
    }

    /**
     * 验证原平台结果匹配时累加评分项分数。
     */
    @Test
    void generateAutoEvaluationShouldScoreMatchedExternalApiResultItem() {
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildExternalApiResultItem("APPROVED", "APPROVED")));
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.emptyList());

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertEquals(new BigDecimal("30.00"), result.getAutoScore());
        assertTrue(result.getEvidenceJson().contains("api_res_001"));
        assertTrue(result.getEvidenceJson().contains("MATCHED"));
        verify(resultMapper).insert(result);
    }

    /**
     * 验证原平台结果不匹配时不给分。
     */
    @Test
    void generateAutoEvaluationShouldNoScoreWhenExternalApiResultMismatched() {
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildExternalApiResultItem("APPROVED", "REJECTED")));
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.emptyList());

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertEquals(new BigDecimal("0.00"), result.getAutoScore());
        assertTrue(result.getEvidenceJson().contains("RESULT_MISMATCH"));
        verify(resultMapper).insert(result);
    }

    /**
     * 验证缺少原平台 API 资源时不给分。
     */
    @Test
    void generateAutoEvaluationShouldNoScoreWhenExternalApiResourceMissing() {
        EvaluationItem item = buildExternalApiResultItem("APPROVED", "APPROVED");
        item.setRelatedApiResourceId(null);
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(item));
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.emptyList());

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertEquals(new BigDecimal("0.00"), result.getAutoScore());
        assertTrue(result.getEvidenceJson().contains("MISSING_API_RESOURCE"));
        verify(resultMapper).insert(result);
    }

    /**
     * 验证字段等值断言命中时累加评分项分数。
     */
    @Test
    void generateAutoEvaluationShouldScoreMatchedFieldEqualsItem() {
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildFieldEqualsItem("APPROVED")));
        ExecutionTrace trace = buildTrace();
        trace.setTraceType("STATE");
        trace.setOutputDataJson("{\"result\":{\"status\":\"APPROVED\"}}");
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.singletonList(trace));

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertEquals(new BigDecimal("15.00"), result.getAutoScore());
        assertTrue(result.getEvidenceJson().contains("FIELD_EQUALS"));
        assertTrue(result.getEvidenceJson().contains("MATCHED"));
        verify(resultMapper).insert(result);
    }

    /**
     * 验证字段等值断言不匹配时不给分。
     */
    @Test
    void generateAutoEvaluationShouldNoScoreWhenFieldEqualsMismatched() {
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildFieldEqualsItem("APPROVED")));
        ExecutionTrace trace = buildTrace();
        trace.setTraceType("STATE");
        trace.setOutputDataJson("{\"result\":{\"status\":\"REJECTED\"}}");
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.singletonList(trace));

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertEquals(new BigDecimal("0.00"), result.getAutoScore());
        assertTrue(result.getEvidenceJson().contains("FIELD_NOT_MATCHED"));
        verify(resultMapper).insert(result);
    }

    /**
     * 验证已有评分结果时更新原记录。
     */
    @Test
    void generateAutoEvaluationShouldUpdateExistingResult() {
        EvaluationResult existed = buildScope();
        existed.setId("result_old");
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildTraceExistsItem()));
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.singletonList(buildTrace()));
        when(resultMapper.selectOne(any())).thenReturn(existed);
        when(resultMapper.selectList(any())).thenReturn(Collections.singletonList(existed));
        when(taskExecutionMapper.selectOne(any())).thenReturn(buildExecution());

        EvaluationResult result = service.generateAutoEvaluation(buildScope());

        assertSame(existed, result);
        assertEquals(new BigDecimal("10.00"), result.getAutoScore());
        verify(resultMapper).updateById(existed);
        verify(resultMapper, times(0)).insert(any());
    }

    /**
     * 验证缺少执行 ID 时拒绝生成评分。
     */
    @Test
    void generateAutoEvaluationShouldRejectMissingExecutionId() {
        EvaluationResult scope = buildScope();
        scope.setExecutionId(null);

        assertThrows(BusinessException.class, () -> service.generateAutoEvaluation(scope));
        verify(evaluationConfigService, times(0)).listItemsByRule(any(), any());
    }

    /**
     * 验证查询不到评分结果时抛出业务异常。
     */
    @Test
    void getEvaluationResultShouldRejectMissingData() {
        assertThrows(BusinessException.class,
                () -> service.getEvaluationResult("tenant_001", "execution_001", "rule_001"));
    }

    /**
     * 验证教师复核会写入人工分、最终分和复核状态。
     */
    @Test
    void reviewEvaluationResultShouldUpdateManualScoreAndStatus() {
        EvaluationResult existed = buildScope();
        existed.setAutoScore(new BigDecimal("10.00"));
        existed.setFinalScore(new BigDecimal("10.00"));
        existed.setEvaluationStatus("AUTO_EVALUATED");
        when(resultMapper.selectOne(any())).thenReturn(existed);
        when(resultMapper.selectList(any())).thenReturn(Collections.singletonList(existed));
        when(taskExecutionMapper.selectOne(any())).thenReturn(buildExecution());
        EvaluationResult review = buildReviewScope(new BigDecimal("8.50"));

        EvaluationResult result = service.reviewEvaluationResult(review);

        assertSame(existed, result);
        assertEquals(new BigDecimal("8.50"), result.getManualScore());
        assertEquals(new BigDecimal("18.50"), result.getFinalScore());
        assertEquals("teacher_001", result.getReviewedBy());
        assertEquals("REVIEWED", result.getEvaluationStatus());
        assertEquals("ADJUSTED", result.getReviewStatus());
        assertEquals("流程规范", result.getReviewReason());
        assertNotNull(result.getReviewedTime());
        verify(resultMapper).updateById(existed);
        verify(taskExecutionMapper).updateById(any(TaskExecution.class));
    }

    /**
     * 验证缺少人工分时拒绝教师复核。
     */
    @Test
    void reviewEvaluationResultShouldRejectMissingManualScore() {
        EvaluationResult review = buildReviewScope(null);

        assertThrows(BusinessException.class, () -> service.reviewEvaluationResult(review));
        verify(resultMapper, times(0)).updateById(any());
    }

    /**
     * 验证找不到自动评分结果时拒绝教师复核。
     */
    @Test
    void reviewEvaluationResultShouldRejectMissingEvaluationResult() {
        assertThrows(BusinessException.class,
                () -> service.reviewEvaluationResult(buildReviewScope(new BigDecimal("8.50"))));
        verify(resultMapper, times(0)).updateById(any());
    }

    /**
     * 构造评分结果范围。
     *
     * @return 评分结果实体。
     */
    private EvaluationResult buildScope() {
        EvaluationResult result = new EvaluationResult();
        result.setId("result_001");
        result.setTenantId("tenant_001");
        result.setExecutionId("execution_001");
        result.setEvaluationRuleId("rule_001");
        result.setCreateBy("system");
        return result;
    }

    /**
     * 构造教师复核范围。
     *
     * @param manualScore 人工分。
     * @return 评分结果复核实体。
     */
    private EvaluationResult buildReviewScope(BigDecimal manualScore) {
        EvaluationResult result = new EvaluationResult();
        result.setTenantId("tenant_001");
        result.setExecutionId("execution_001");
        result.setEvaluationRuleId("rule_001");
        result.setManualScore(manualScore);
        result.setReviewedBy("teacher_001");
        result.setReviewReason("流程规范");
        return result;
    }

    private TaskExecution buildExecution() {
        TaskExecution execution = new TaskExecution();
        execution.setId("execution_001");
        execution.setTenantId("tenant_001");
        execution.setExecutionStatus("COMPLETED");
        execution.setDeleted(Boolean.FALSE);
        return execution;
    }

    /**
     * 构造 TRACE_EXISTS 评分项。
     *
     * @return 评分项实体。
     */
    private EvaluationItem buildTraceExistsItem() {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_001");
        item.setItemCode("ITEM_SUBMIT");
        item.setRelatedTaskStepId("step_001");
        item.setRelatedResourceId("res_001");
        item.setScore(new BigDecimal("10.00"));
        item.setAssertionType("TRACE_EXISTS");
        item.setAssertionConfigJson("{\"traceType\":\"STEP_COMPLETED\",\"success\":true}");
        item.setFailPolicy("NO_SCORE");
        return item;
    }

    /**
     * 构造暂未支持的评分项。
     *
     * @return 评分项实体。
     */
    private EvaluationItem buildUnsupportedItem() {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_002");
        item.setItemCode("ITEM_RESULT");
        item.setScore(new BigDecimal("20.00"));
        item.setAssertionType("RESULT_EXISTS");
        item.setAssertionConfigJson("{}");
        item.setFailPolicy("NO_SCORE");
        return item;
    }

    /**
     * 构造原平台结果校验评分项。
     *
     * @param expectedStatus 期望状态。
     * @param actualStatus 实际状态。
     * @return 评分项实体。
     */
    private EvaluationItem buildExternalApiResultItem(String expectedStatus, String actualStatus) {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_003");
        item.setItemCode("ITEM_EXTERNAL_RESULT");
        item.setRelatedApiResourceId("api_res_001");
        item.setScore(new BigDecimal("30.00"));
        item.setAssertionType("EXTERNAL_API_RESULT");
        item.setAssertionConfigJson("{\"expectedStatus\":\"" + expectedStatus
                + "\",\"actualStatus\":\"" + actualStatus + "\"}");
        item.setFailPolicy("NO_SCORE");
        return item;
    }

    /**
     * 构造字段等值校验评分项。
     *
     * @param expectedStatus 期望状态。
     * @return 评分项实体。
     */
    private EvaluationItem buildFieldEqualsItem(String expectedStatus) {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_004");
        item.setItemCode("ITEM_FIELD_RESULT");
        item.setRelatedTaskStepId("step_001");
        item.setRelatedResourceId("res_001");
        item.setScore(new BigDecimal("15.00"));
        item.setAssertionType("FIELD_EQUALS");
        item.setAssertionConfigJson("{\"traceType\":\"STATE\",\"source\":\"output\",\"field\":\"result.status\","
                + "\"expected\":\"" + expectedStatus + "\"}");
        item.setFailPolicy("NO_SCORE");
        return item;
    }

    /**
     * 构造命中的执行轨迹。
     *
     * @return 执行轨迹实体。
     */
    private ExecutionTrace buildTrace() {
        ExecutionTrace trace = new ExecutionTrace();
        trace.setId("trace_001");
        trace.setTaskStepId("step_001");
        trace.setResourceId("res_001");
        trace.setTraceType("STEP_COMPLETED");
        trace.setTraceTime(LocalDateTime.now());
        trace.setSuccess(Boolean.TRUE);
        return trace;
    }
}
