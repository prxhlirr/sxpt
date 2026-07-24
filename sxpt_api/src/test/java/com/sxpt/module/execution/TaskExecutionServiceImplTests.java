package com.sxpt.module.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.course.entity.Task;
import com.sxpt.module.course.entity.TaskTeachingPoint;
import com.sxpt.module.course.mapper.TaskMapper;
import com.sxpt.module.course.service.TaskPublishService;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationResult;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import com.sxpt.module.evaluation.service.AutoEvaluationService;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.mapper.TaskExecutionMapper;
import com.sxpt.module.execution.service.TaskExecutionContextService;
import com.sxpt.module.execution.service.TaskExecutionService;
import com.sxpt.module.execution.service.impl.TaskExecutionServiceImpl;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.service.PracticeAttemptService;
import com.sxpt.module.practice.service.PracticeScoreSummaryService;
import com.sxpt.module.practice.service.PracticeStepResultService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学生任务执行主聚合服务测试。
 *
 * 业务功能：
 * 1. 验证开始执行必须依赖已发布任务，且会创建 RUNNING 主记录和执行上下文快照。
 * 2. 验证提交执行只能从 RUNNING 状态进入 SUBMITTED 状态。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper 和下游服务，隔离数据库验证 Service 业务规则。
 * 2. 直接调用 Service，验证状态流转、上下文快照和 Mapper 调用边界。
 */
class TaskExecutionServiceImplTests {

    private final TaskExecutionMapper taskExecutionMapper = mock(TaskExecutionMapper.class);

    private final TaskMapper taskMapper = mock(TaskMapper.class);

    private final TaskPublishService taskPublishService = mock(TaskPublishService.class);

    private final TaskStepService taskStepService = mock(TaskStepService.class);

    private final EvaluationConfigService evaluationConfigService = mock(EvaluationConfigService.class);

    private final AutoEvaluationService autoEvaluationService = mock(AutoEvaluationService.class);

    private final PracticeAttemptService practiceAttemptService = mock(PracticeAttemptService.class);

    private final PracticeScoreSummaryService practiceScoreSummaryService = mock(PracticeScoreSummaryService.class);

    private final PracticeStepResultService practiceStepResultService = mock(PracticeStepResultService.class);

    private final TaskExecutionContextService taskExecutionContextService = mock(TaskExecutionContextService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final TaskExecutionService service = new TaskExecutionServiceImpl(
            taskExecutionMapper,
            taskMapper,
            taskPublishService,
            taskStepService,
            evaluationConfigService,
            autoEvaluationService,
            practiceAttemptService,
            practiceScoreSummaryService,
            practiceStepResultService,
            taskExecutionContextService,
            objectMapper);

    /**
     * 验证开始执行时任务已发布才允许插入 RUNNING 主记录，并同步生成上下文快照。
     */
    @Test
    void startExecutionShouldInsertRunningExecutionAndContextWhenTaskPublished() {
        when(taskMapper.selectOne(any())).thenReturn(buildPublishedTask());
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.singletonList(buildTaskTeachingPoint()));
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildEvaluationRule()));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildEvaluationItem()));

        TaskExecution execution = buildStartExecution();
        TaskExecution saved = service.startExecution(execution);

        assertSame(execution, saved);
        assertEquals("RUNNING", saved.getExecutionStatus());
        assertEquals("READY", saved.getExecutionIdentityStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getStartTime());
        verify(taskMapper).selectOne(any());
        verify(taskExecutionMapper).insert(saved);

        ArgumentCaptor<TaskExecutionContext> contextCaptor = ArgumentCaptor.forClass(TaskExecutionContext.class);
        verify(taskExecutionContextService).createContext(contextCaptor.capture());
        TaskExecutionContext context = contextCaptor.getValue();
        assertEquals("execution_001", context.getExecutionId());
        assertEquals("task_001", context.getTaskId());
        assertEquals("student_001", context.getStudentId());
        assertEquals("PRACTICE", context.getSdkMode());
        assertRuntimeContext(context.getContextJson());
        assertTeachingPointSnapshot(context.getTeachingPointSnapshotJson());
        assertResourceSnapshot(context.getResourceSnapshotJson());
        assertEvaluationSnapshot(context.getEvaluationSnapshotJson());

        ArgumentCaptor<PracticeAttempt> attemptCaptor = ArgumentCaptor.forClass(PracticeAttempt.class);
        verify(practiceAttemptService).startAttempt(attemptCaptor.capture());
        PracticeAttempt attempt = attemptCaptor.getValue();
        assertNotNull(attempt.getId());
        assertEquals("tenant_001", attempt.getTenantId());
        assertEquals("execution_001", attempt.getExecutionId());
        assertEquals("student_001", attempt.getStudentId());
        assertEquals("task_001", attempt.getTaskId());
        assertEquals(saved.getStartTime(), attempt.getStartTime());
        assertEquals("student_001", attempt.getCreateBy());
        assertEquals("student_001", attempt.getUpdateBy());
    }

    /**
     * 验证非练习 SDK 模式不会创建 practice_attempt，避免学习或考试执行污染练习次数。
     */
    @Test
    void startExecutionShouldNotCreatePracticeAttemptWhenSdkModeIsNotPractice() {
        when(taskMapper.selectOne(any())).thenReturn(buildPublishedTask());
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.singletonList(buildTaskTeachingPoint()));
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildEvaluationRule()));
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildEvaluationItem()));

        TaskExecution execution = buildStartExecution();
        execution.setSdkMode("LEARNING");

        TaskExecution saved = service.startExecution(execution);

        assertSame(execution, saved);
        assertEquals("LEARNING", saved.getSdkMode());
        verify(practiceAttemptService, times(0)).startAttempt(any());
    }

    /**
     * 验证未发布任务不能开始执行，避免学生消费草稿资产。
     */
    @Test
    void startExecutionShouldRejectUnpublishedTask() {
        Task task = buildPublishedTask();
        task.setTaskStatus("DRAFT");
        when(taskMapper.selectOne(any())).thenReturn(task);

        assertThrows(BusinessException.class, () -> service.startExecution(buildStartExecution()));
        verify(taskExecutionMapper, times(0)).insert(any());
        verify(taskExecutionContextService, times(0)).createContext(any());
    }

    /**
     * 验证已发布任务缺少教学点资产时不能生成可运行上下文。
     */
    @Test
    void startExecutionShouldRejectWhenTeachingPointSnapshotMissing() {
        when(taskMapper.selectOne(any())).thenReturn(buildPublishedTask());
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> service.startExecution(buildStartExecution()));
        verify(taskExecutionMapper).insert(any());
        verify(taskExecutionContextService, times(0)).createContext(any());
    }

    /**
     * 验证 RUNNING 执行提交后进入 SUBMITTED 状态。
     */
    @Test
    void submitExecutionShouldCompleteExecutionWithAutoEvaluationScore() {
        TaskExecution running = buildRunningExecution();
        when(taskExecutionMapper.selectOne(any())).thenReturn(running);
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.singletonList(buildTaskTeachingPoint()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildEvaluationRule()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", null))
                .thenReturn(Collections.emptyList());
        when(autoEvaluationService.generateAutoEvaluation(any(EvaluationResult.class)))
                .thenReturn(buildEvaluationResult("result_001", "rule_001", new BigDecimal("10.00")));
        when(practiceAttemptService.listByStudentAndTask("tenant_001", "student_001", "task_001"))
                .thenReturn(Collections.singletonList(buildPracticeAttempt()));
        when(practiceAttemptService.finishAttempt(any(PracticeAttempt.class))).thenReturn(buildCompletedPracticeAttempt());
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(buildEvaluationItem()));
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildTaskStep()));

        TaskExecution submitted = service.submitExecution("tenant_001", "execution_001", "student_001");

        assertSame(running, submitted);
        assertEquals("COMPLETED", submitted.getExecutionStatus());
        assertEquals(new BigDecimal("10.00"), submitted.getScore());
        assertEquals("自动评分完成：生成 1 条评分结果，总分 10.00。", submitted.getResultSummary());
        assertEquals("student_001", submitted.getUpdateBy());
        assertNotNull(submitted.getEndTime());
        verify(taskExecutionMapper).updateById(running);
        ArgumentCaptor<EvaluationResult> resultCaptor = ArgumentCaptor.forClass(EvaluationResult.class);
        verify(autoEvaluationService).generateAutoEvaluation(resultCaptor.capture());
        EvaluationResult evaluationResult = resultCaptor.getValue();
        assertNotNull(evaluationResult.getId());
        assertEquals("tenant_001", evaluationResult.getTenantId());
        assertEquals("execution_001", evaluationResult.getExecutionId());
        assertEquals("rule_001", evaluationResult.getEvaluationRuleId());
        assertEquals("student_001", evaluationResult.getCreateBy());
        ArgumentCaptor<PracticeAttempt> finishCaptor = ArgumentCaptor.forClass(PracticeAttempt.class);
        verify(practiceAttemptService).finishAttempt(finishCaptor.capture());
        PracticeAttempt finishedAttempt = finishCaptor.getValue();
        assertEquals("attempt_001", finishedAttempt.getId());
        assertEquals("COMPLETED", finishedAttempt.getAttemptStatus());
        assertEquals(submitted.getEndTime(), finishedAttempt.getEndTime());
        assertEquals(new BigDecimal("10.00"), finishedAttempt.getScore());
        assertEquals("student_001", finishedAttempt.getUpdateBy());
        ArgumentCaptor<PracticeStepResult> stepResultCaptor = ArgumentCaptor.forClass(PracticeStepResult.class);
        verify(practiceStepResultService).reportStepResult(stepResultCaptor.capture());
        PracticeStepResult stepResult = stepResultCaptor.getValue();
        assertNotNull(stepResult.getId());
        assertEquals("attempt_001", stepResult.getAttemptId());
        assertEquals("execution_001", stepResult.getExecutionId());
        assertEquals("student_001", stepResult.getStudentId());
        assertEquals("task_001", stepResult.getTaskId());
        assertEquals("step_001", stepResult.getTaskStepId());
        assertEquals("STEP_SUBMIT", stepResult.getStepCode());
        assertEquals(Long.valueOf(1), stepResult.getSequenceNo());
        assertEquals("PASSED", stepResult.getResultStatus());
        assertEquals(0, new BigDecimal("10.00").compareTo(stepResult.getScore()));
        assertEquals(0, new BigDecimal("10.00").compareTo(stepResult.getMaxScore()));
        assertEquals(Boolean.TRUE, stepResult.getPassFlag());
        assertEquals(submitted.getEndTime(), stepResult.getEndTime());
        assertEquals("student_001", stepResult.getCreateBy());
        ArgumentCaptor<PracticeScoreSummary> summaryCaptor = ArgumentCaptor.forClass(PracticeScoreSummary.class);
        verify(practiceScoreSummaryService).generateSummary(summaryCaptor.capture());
        PracticeScoreSummary summary = summaryCaptor.getValue();
        assertNotNull(summary.getId());
        assertEquals("tenant_001", summary.getTenantId());
        assertEquals("student_001", summary.getStudentId());
        assertEquals("class_001", summary.getClassId());
        assertEquals("task_001", summary.getTaskId());
        assertEquals("tp_001", summary.getTeachingPointId());
        assertEquals("student_001", summary.getCreateBy());
    }

    /**
     * 验证自动评分没有返回结果时拒绝完成执行，避免主记录进入无分数的完成态。
     */
    @Test
    void submitExecutionShouldRejectWhenAutoEvaluationResultMissing() {
        TaskExecution running = buildRunningExecution();
        when(taskExecutionMapper.selectOne(any())).thenReturn(running);
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.singletonList(buildTaskTeachingPoint()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildEvaluationRule()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", null))
                .thenReturn(Collections.emptyList());
        when(autoEvaluationService.generateAutoEvaluation(any(EvaluationResult.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> service.submitExecution("tenant_001", "execution_001", "student_001"));
        verify(taskExecutionMapper, times(0)).updateById(any());
        verify(autoEvaluationService).generateAutoEvaluation(any(EvaluationResult.class));
    }

    /**
     * 验证练习模式缺少 attempt 时拒绝提交完成，避免执行记录和练习次数脱节。
     */
    @Test
    void submitExecutionShouldRejectWhenPracticeAttemptMissing() {
        TaskExecution running = buildRunningExecution();
        when(taskExecutionMapper.selectOne(any())).thenReturn(running);
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.singletonList(buildTaskTeachingPoint()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(buildEvaluationRule()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", null))
                .thenReturn(Collections.emptyList());
        when(autoEvaluationService.generateAutoEvaluation(any(EvaluationResult.class)))
                .thenReturn(buildEvaluationResult("result_001", "rule_001", new BigDecimal("10.00")));
        when(practiceAttemptService.listByStudentAndTask("tenant_001", "student_001", "task_001"))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class,
                () -> service.submitExecution("tenant_001", "execution_001", "student_001"));
        verify(taskExecutionMapper, times(0)).updateById(any());
        verify(practiceAttemptService, times(0)).finishAttempt(any());
        verify(practiceStepResultService, times(0)).reportStepResult(any());
        verify(practiceScoreSummaryService, times(0)).generateSummary(any());
    }

    /**
     * 验证非 RUNNING 状态不能重复提交。
     */
    @Test
    void submitExecutionShouldRejectNonRunningExecution() {
        TaskExecution submitted = buildRunningExecution();
        submitted.setExecutionStatus("SUBMITTED");
        when(taskExecutionMapper.selectOne(any())).thenReturn(submitted);

        assertThrows(BusinessException.class,
                () -> service.submitExecution("tenant_001", "execution_001", "student_001"));
        verify(taskExecutionMapper, times(0)).updateById(any());
        verify(autoEvaluationService, times(0)).generateAutoEvaluation(any());
        verify(practiceAttemptService, times(0)).listByStudentAndTask(any(), any(), any());
        verify(practiceAttemptService, times(0)).finishAttempt(any());
        verify(practiceStepResultService, times(0)).reportStepResult(any());
        verify(practiceScoreSummaryService, times(0)).generateSummary(any());
    }

    /**
     * 验证提交执行时缺少评分规则会拒绝提交，避免产生无评分结果的完成链路。
     */
    /**
     * 验证已完成执行重复提交时直接返回已有主记录，避免学生端重试导致重复评分。
     */
    @Test
    void submitExecutionShouldReturnCompletedExecutionWithoutDuplicateEvaluation() {
        TaskExecution completed = buildRunningExecution();
        completed.setExecutionStatus("COMPLETED");
        completed.setScore(new BigDecimal("10.00"));
        completed.setResultSummary("auto evaluation completed");
        when(taskExecutionMapper.selectOne(any())).thenReturn(completed);

        TaskExecution submitted = service.submitExecution("tenant_001", "execution_001", "student_001");

        assertSame(completed, submitted);
        assertEquals("COMPLETED", submitted.getExecutionStatus());
        assertEquals(new BigDecimal("10.00"), submitted.getScore());
        verify(taskExecutionMapper, times(0)).updateById(any());
        verify(autoEvaluationService, times(0)).generateAutoEvaluation(any());
        verify(practiceScoreSummaryService, times(0)).generateSummary(any());
    }

    @Test
    void submitExecutionShouldRejectWhenEvaluationRuleMissing() {
        TaskExecution running = buildRunningExecution();
        when(taskExecutionMapper.selectOne(any())).thenReturn(running);
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.singletonList(buildTaskTeachingPoint()));
        when(evaluationConfigService.listRules("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.emptyList());
        when(evaluationConfigService.listRules("tenant_001", "task_001", null))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class,
                () -> service.submitExecution("tenant_001", "execution_001", "student_001"));
        verify(taskExecutionMapper, times(0)).updateById(any());
        verify(autoEvaluationService, times(0)).generateAutoEvaluation(any());
    }

    /**
     * 验证按学生和任务查询执行历史时返回 Mapper 结果。
     */
    @Test
    void listByStudentAndTaskShouldReturnMapperResult() {
        TaskExecution execution = buildRunningExecution();
        when(taskExecutionMapper.selectList(any())).thenReturn(Collections.singletonList(execution));

        List<TaskExecution> result = service.listByStudentAndTask("tenant_001", "student_001", "task_001");

        assertEquals(1, result.size());
        assertSame(execution, result.get(0));
        verify(taskExecutionMapper).selectList(any());
    }

    private Task buildPublishedTask() {
        Task task = new Task();
        task.setId("task_001");
        task.setTenantId("tenant_001");
        task.setTaskName("备案练习任务");
        task.setTaskType("PRACTICE");
        task.setTaskStatus("PUBLISHED");
        task.setOverlayPolicyJson("{\"mask\":true}");
        return task;
    }

    private TaskTeachingPoint buildTaskTeachingPoint() {
        TaskTeachingPoint teachingPoint = new TaskTeachingPoint();
        teachingPoint.setTenantId("tenant_001");
        teachingPoint.setTaskId("task_001");
        teachingPoint.setTeachingPointId("tp_001");
        teachingPoint.setRequiredFlag(Boolean.TRUE);
        teachingPoint.setSequenceNo(1L);
        return teachingPoint;
    }

    private TaskStep buildTaskStep() {
        TaskStep step = new TaskStep();
        step.setId("step_001");
        step.setTenantId("tenant_001");
        step.setTaskId("task_001");
        step.setTeachingPointId("tp_001");
        step.setStepCode("STEP_SUBMIT");
        step.setStepName("提交备案申请");
        step.setSequenceNo(1L);
        step.setRelatedResourceIds("[\"res_001\"]");
        step.setGuideContent("点击提交按钮。");
        step.setPracticeHint("请点击提交。");
        step.setRequired(Boolean.TRUE);
        step.setAllowSkip(Boolean.FALSE);
        return step;
    }

    private EvaluationRule buildEvaluationRule() {
        EvaluationRule rule = new EvaluationRule();
        rule.setId("rule_001");
        rule.setTenantId("tenant_001");
        rule.setTaskId("task_001");
        rule.setTeachingPointId("tp_001");
        rule.setRuleCode("RULE_SUBMIT");
        rule.setRuleName("备案提交评分");
        rule.setTotalScore(new BigDecimal("10.00"));
        return rule;
    }

    private EvaluationResult buildEvaluationResult(String id, String ruleId, BigDecimal finalScore) {
        EvaluationResult result = new EvaluationResult();
        result.setId(id);
        result.setTenantId("tenant_001");
        result.setExecutionId("execution_001");
        result.setEvaluationRuleId(ruleId);
        result.setAutoScore(finalScore);
        result.setFinalScore(finalScore);
        result.setEvaluationSummary("auto evaluation completed");
        result.setEvidenceJson("{\"items\":[{\"itemId\":\"item_001\",\"matched\":true,\"score\":10.00}]}");
        result.setEvaluationStatus("AUTO_EVALUATED");
        return result;
    }

    private PracticeAttempt buildPracticeAttempt() {
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId("attempt_001");
        attempt.setTenantId("tenant_001");
        attempt.setExecutionId("execution_001");
        attempt.setStudentId("student_001");
        attempt.setTaskId("task_001");
        attempt.setAttemptNo(1L);
        attempt.setAttemptStatus("RUNNING");
        attempt.setStartTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        attempt.setDeleted(Boolean.FALSE);
        return attempt;
    }

    private PracticeAttempt buildCompletedPracticeAttempt() {
        PracticeAttempt attempt = buildPracticeAttempt();
        attempt.setAttemptStatus("COMPLETED");
        attempt.setClassId("class_001");
        attempt.setTeachingPointId("tp_001");
        attempt.setEndTime(LocalDateTime.of(2026, 7, 21, 9, 30));
        attempt.setScore(new BigDecimal("10.00"));
        return attempt;
    }

    private EvaluationItem buildEvaluationItem() {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_001");
        item.setTenantId("tenant_001");
        item.setEvaluationRuleId("rule_001");
        item.setTeachingPointId("tp_001");
        item.setItemCode("ITEM_SUBMIT");
        item.setItemName("完成提交");
        item.setItemType("KEY_ACTION");
        item.setRelatedTaskStepId("step_001");
        item.setRelatedResourceId("res_001");
        item.setScore(new BigDecimal("10.00"));
        item.setAssertionType("TRACE_EXISTS");
        item.setAssertionConfigJson("{\"traceType\":\"CLICK\"}");
        item.setFailPolicy("NO_SCORE");
        return item;
    }

    private TaskExecution buildStartExecution() {
        TaskExecution execution = new TaskExecution();
        execution.setId("execution_001");
        execution.setTenantId("tenant_001");
        execution.setTaskId("task_001");
        execution.setStudentId("student_001");
        execution.setConnectorSystemId("connector_001");
        execution.setExecutionMode("PRACTICE");
        execution.setSdkMode("PRACTICE");
        execution.setExecutionIdentityJson("{\"identityMode\":\"STUDENT\"}");
        execution.setCreateBy("student_001");
        execution.setUpdateBy("student_001");
        return execution;
    }

    private TaskExecution buildRunningExecution() {
        TaskExecution execution = buildStartExecution();
        execution.setStartTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        execution.setExecutionStatus("RUNNING");
        execution.setStatus("ACTIVE");
        execution.setDeleted(Boolean.FALSE);
        return execution;
    }

    private void assertRuntimeContext(String snapshotJson) {
        Map<String, Object> context = readMap(snapshotJson);
        assertEquals("execution_001", context.get("executionId"));
        assertEquals("task_001", context.get("taskId"));
        assertEquals("student_001", context.get("studentId"));
        assertEquals("PRACTICE", context.get("sdkMode"));
    }

    private void assertTeachingPointSnapshot(String snapshotJson) {
        List<Map<String, Object>> snapshots = readList(snapshotJson);
        assertEquals(1, snapshots.size());
        Map<String, Object> teachingPoint = snapshots.get(0);
        assertEquals("tp_001", teachingPoint.get("teachingPointId"));
        List<Map<String, Object>> steps = (List<Map<String, Object>>) teachingPoint.get("steps");
        assertEquals(1, steps.size());
        assertEquals("step_001", steps.get(0).get("taskStepId"));
        assertEquals("STEP_SUBMIT", steps.get(0).get("stepCode"));
        assertEquals("[\"res_001\"]", steps.get(0).get("relatedResourceIds"));
    }

    private void assertResourceSnapshot(String snapshotJson) {
        List<Map<String, Object>> snapshots = readList(snapshotJson);
        assertEquals(1, snapshots.size());
        assertEquals("tp_001", snapshots.get(0).get("teachingPointId"));
        assertEquals("step_001", snapshots.get(0).get("taskStepId"));
        assertEquals("[\"res_001\"]", snapshots.get(0).get("relatedResourceIds"));
    }

    private void assertEvaluationSnapshot(String snapshotJson) {
        List<Map<String, Object>> snapshots = readList(snapshotJson);
        assertEquals(1, snapshots.size());
        Map<String, Object> rule = snapshots.get(0);
        assertEquals("rule_001", rule.get("evaluationRuleId"));
        assertEquals("RULE_SUBMIT", rule.get("ruleCode"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) rule.get("items");
        assertEquals(1, items.size());
        assertEquals("item_001", items.get(0).get("evaluationItemId"));
        assertEquals("ITEM_SUBMIT", items.get(0).get("itemCode"));
        assertEquals("step_001", items.get(0).get("relatedTaskStepId"));
    }

    private List<Map<String, Object>> readList(String json) {
        try {
            return objectMapper.readValue(json, List.class);
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }
}
