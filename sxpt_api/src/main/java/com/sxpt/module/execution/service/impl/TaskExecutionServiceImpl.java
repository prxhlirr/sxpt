package com.sxpt.module.execution.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
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
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.service.PracticeAttemptService;
import com.sxpt.module.practice.service.PracticeScoreSummaryService;
import com.sxpt.module.practice.service.PracticeStepResultService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 学生任务执行主聚合服务实现。
 *
 * 业务功能：
 * 1. 创建学生执行主记录，确保每次学生任务执行都有统一 executionId。
 * 2. 控制最小状态机，避免未发布任务被学生消费，避免已提交或已完成执行重复提交。
 *
 * 关键流程：
 * 1. startExecution 校验任务处于 PUBLISHED 后，补齐 RUNNING、默认身份状态和生命周期字段。
 * 2. submitExecution 读取未删除执行记录，只允许 RUNNING 流转为 SUBMITTED。
 */
@Service
@Profile("!test")
public class TaskExecutionServiceImpl implements TaskExecutionService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String RUNNING_STATUS = "RUNNING";

    private static final String COMPLETED_STATUS = "COMPLETED";

    private static final String PUBLISHED_TASK_STATUS = "PUBLISHED";

    private static final String NONE_IDENTITY_STATUS = "NONE";

    private static final String READY_IDENTITY_STATUS = "READY";

    private static final String PRACTICE_SDK_MODE = "PRACTICE";

    private final TaskExecutionMapper taskExecutionMapper;

    private final TaskMapper taskMapper;

    private final TaskPublishService taskPublishService;

    private final TaskStepService taskStepService;

    private final EvaluationConfigService evaluationConfigService;

    private final AutoEvaluationService autoEvaluationService;

    private final PracticeAttemptService practiceAttemptService;

    private final PracticeScoreSummaryService practiceScoreSummaryService;

    private final PracticeStepResultService practiceStepResultService;

    private final TaskExecutionContextService taskExecutionContextService;

    private final ObjectMapper objectMapper;

    public TaskExecutionServiceImpl(TaskExecutionMapper taskExecutionMapper,
                                    TaskMapper taskMapper,
                                    TaskPublishService taskPublishService,
                                    TaskStepService taskStepService,
                                    EvaluationConfigService evaluationConfigService,
                                    AutoEvaluationService autoEvaluationService,
                                    PracticeAttemptService practiceAttemptService,
                                    PracticeScoreSummaryService practiceScoreSummaryService,
                                    PracticeStepResultService practiceStepResultService,
                                    TaskExecutionContextService taskExecutionContextService,
                                    ObjectMapper objectMapper) {
        this.taskExecutionMapper = taskExecutionMapper;
        this.taskMapper = taskMapper;
        this.taskPublishService = taskPublishService;
        this.taskStepService = taskStepService;
        this.evaluationConfigService = evaluationConfigService;
        this.autoEvaluationService = autoEvaluationService;
        this.practiceAttemptService = practiceAttemptService;
        this.practiceScoreSummaryService = practiceScoreSummaryService;
        this.practiceStepResultService = practiceStepResultService;
        this.taskExecutionContextService = taskExecutionContextService;
        this.objectMapper = objectMapper;
    }

    /**
     * 开始一次学生任务执行。
     *
     * @param execution 待创建的学生任务执行记录。
     * @return 已保存的学生任务执行记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskExecution startExecution(TaskExecution execution) {
        validateStartFields(execution);
        Task task = validateTaskPublished(execution.getTenantId(), execution.getTaskId());
        fillStartDefaults(execution);
        taskExecutionMapper.insert(execution);
        taskExecutionContextService.createContext(buildExecutionContext(execution, task));
        createPracticeAttemptIfNeeded(execution);
        return execution;
    }

    /**
     * 练习模式开始执行时同步创建练习次数记录。
     *
     * @param execution 已创建的学生任务执行记录。
     */
    private void createPracticeAttemptIfNeeded(TaskExecution execution) {
        if (!PRACTICE_SDK_MODE.equals(execution.getSdkMode())) {
            return;
        }
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId(generateId());
        attempt.setTenantId(execution.getTenantId());
        attempt.setExecutionId(execution.getId());
        attempt.setStudentId(execution.getStudentId());
        attempt.setTaskId(execution.getTaskId());
        List<TaskTeachingPoint> teachingPoints = taskPublishService.listTeachingPointsByTask(
                execution.getTenantId(), execution.getTaskId());
        if (teachingPoints != null && teachingPoints.size() == 1) {
            attempt.setTeachingPointId(teachingPoints.get(0).getTeachingPointId());
        }
        attempt.setStartTime(execution.getStartTime());
        attempt.setCreateBy(execution.getStudentId());
        attempt.setUpdateBy(execution.getStudentId());
        practiceAttemptService.startAttempt(attempt);
    }

    /**
     * 提交一次学生任务执行。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行记录 ID。
     * @param operatorId 操作人 ID。
     * @return 已提交的学生任务执行记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskExecution submitExecution(String tenantId, String executionId, String operatorId) {
        requireText(tenantId);
        requireText(executionId);
        requireText(operatorId);
        TaskExecution existed = getExecution(tenantId, executionId);
        if (COMPLETED_STATUS.equals(existed.getExecutionStatus())) {
            return existed;
        }
        if (!RUNNING_STATUS.equals(existed.getExecutionStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        List<EvaluationRule> rules = listEvaluationRulesForTask(existed);
        if (rules.isEmpty()) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        List<EvaluationResult> evaluationResults = triggerAutoEvaluation(existed, operatorId, rules);
        LocalDateTime now = LocalDateTime.now();
        fillSubmitResult(existed, operatorId, now, evaluationResults);
        PracticeAttempt completedAttempt = completePracticeAttemptIfNeeded(existed, operatorId, now);
        createPracticeStepResultsIfNeeded(existed, operatorId, now, completedAttempt, evaluationResults);
        refreshPracticeSummaryIfNeeded(completedAttempt, operatorId);
        taskExecutionMapper.updateById(existed);
        return existed;
    }

    /**
     * 练习模式提交完成后同步完成对应 attempt，让练习次数表成为汇总统计的事实来源。
     *
     * @param execution 已完成评分聚合的学生执行记录。
     * @param operatorId 触发提交的学生或系统操作人 ID。
     * @param now 本次提交完成时间。
     */
    private PracticeAttempt completePracticeAttemptIfNeeded(TaskExecution execution, String operatorId, LocalDateTime now) {
        if (!PRACTICE_SDK_MODE.equals(execution.getSdkMode())) {
            return null;
        }
        PracticeAttempt attempt = findPracticeAttemptByExecution(execution);
        PracticeAttempt update = new PracticeAttempt();
        update.setId(attempt.getId());
        update.setTenantId(execution.getTenantId());
        update.setStudentId(execution.getStudentId());
        update.setAttemptStatus(COMPLETED_STATUS);
        update.setEndTime(now);
        update.setScore(execution.getScore());
        update.setUpdateBy(operatorId);
        return practiceAttemptService.finishAttempt(update);
    }

    /**
     * 按执行主记录定位同一次练习 attempt。
     *
     * @param execution 学生执行主记录。
     * @return 与 executionId 匹配的练习次数记录。
     */
    private PracticeAttempt findPracticeAttemptByExecution(TaskExecution execution) {
        List<PracticeAttempt> attempts = practiceAttemptService.listByStudentAndTask(
                execution.getTenantId(), execution.getStudentId(), execution.getTaskId());
        if (attempts != null) {
            for (PracticeAttempt attempt : attempts) {
                if (attempt != null && execution.getId().equals(attempt.getExecutionId())) {
                    return attempt;
                }
            }
        }
        throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
    }

    /**
     * 练习模式提交完成后，根据评分证据生成步骤级结果。
     *
     * @param execution 已完成评分聚合的学生执行记录。
     * @param operatorId 触发提交的学生或系统操作人 ID。
     * @param now 本次提交完成时间。
     * @param attempt 已完成的练习次数记录。
     * @param evaluationResults 已生成的自动评分结果。
     */
    private void createPracticeStepResultsIfNeeded(TaskExecution execution, String operatorId, LocalDateTime now,
                                                   PracticeAttempt attempt,
                                                   List<EvaluationResult> evaluationResults) {
        if (attempt == null) {
            return;
        }
        Map<String, PracticeStepResult> stepResults = new LinkedHashMap<>();
        for (EvaluationResult evaluationResult : evaluationResults) {
            Map<String, BigDecimal> evidenceScores = evidenceScoresByItemId(evaluationResult.getEvidenceJson());
            List<EvaluationItem> items = evaluationConfigService.listItemsByRule(
                    execution.getTenantId(), evaluationResult.getEvaluationRuleId());
            if (items == null) {
                continue;
            }
            for (EvaluationItem item : items) {
                if (!StringUtils.hasText(item.getRelatedTaskStepId())) {
                    continue;
                }
                PracticeStepResult stepResult = stepResults.computeIfAbsent(item.getRelatedTaskStepId(),
                        taskStepId -> buildPracticeStepResult(execution, attempt, operatorId, now, item));
                BigDecimal itemScore = evidenceScores.containsKey(item.getId())
                        ? evidenceScores.get(item.getId()) : BigDecimal.ZERO;
                stepResult.setScore(defaultDecimal(stepResult.getScore()).add(itemScore));
                stepResult.setMaxScore(defaultDecimal(stepResult.getMaxScore()).add(defaultDecimal(item.getScore())));
                stepResult.setEvidenceJson(evaluationResult.getEvidenceJson());
                stepResult.setFeedback(evaluationResult.getEvaluationSummary());
            }
        }
        for (PracticeStepResult stepResult : stepResults.values()) {
            boolean passed = defaultDecimal(stepResult.getScore()).compareTo(BigDecimal.ZERO) > 0;
            stepResult.setPassFlag(passed);
            stepResult.setResultStatus(passed ? "PASSED" : "FAILED");
            practiceStepResultService.reportStepResult(stepResult);
        }
    }

    /**
     * 练习模式提交完成后刷新过程分汇总。
     *
     * 业务逻辑：执行服务只负责把“本次练习已完成”的事实通知汇总服务，
     * 汇总次数、最高分、最近分、薄弱步骤等计算仍由 PracticeScoreSummaryService 统一承担，
     * 避免学生执行主聚合和练习统计规则互相耦合。
     *
     * @param attempt 已完成的练习次数记录。
     * @param operatorId 触发提交的学生或系统操作人 ID。
     */
    private void refreshPracticeSummaryIfNeeded(PracticeAttempt attempt, String operatorId) {
        if (attempt == null) {
            return;
        }
        PracticeScoreSummary scope = new PracticeScoreSummary();
        scope.setId(generateId());
        scope.setTenantId(attempt.getTenantId());
        scope.setStudentId(attempt.getStudentId());
        scope.setClassId(attempt.getClassId());
        scope.setTaskId(attempt.getTaskId());
        scope.setTeachingPointId(attempt.getTeachingPointId());
        scope.setCreateBy(operatorId);
        practiceScoreSummaryService.generateSummary(scope);
    }

    /**
     * 构造步骤结果初始对象，步骤编号优先来自已发布 task_step。
     *
     * @param execution 学生执行主记录。
     * @param attempt 练习次数记录。
     * @param operatorId 触发提交的学生或系统操作人 ID。
     * @param now 本次提交完成时间。
     * @param item 评分项。
     * @return 待上报的步骤结果。
     */
    private PracticeStepResult buildPracticeStepResult(TaskExecution execution, PracticeAttempt attempt,
                                                       String operatorId, LocalDateTime now,
                                                       EvaluationItem item) {
        TaskStep taskStep = findTaskStep(execution, item);
        PracticeStepResult stepResult = new PracticeStepResult();
        stepResult.setId(generateId());
        stepResult.setTenantId(execution.getTenantId());
        stepResult.setAttemptId(attempt.getId());
        stepResult.setExecutionId(execution.getId());
        stepResult.setStudentId(execution.getStudentId());
        stepResult.setTaskId(execution.getTaskId());
        stepResult.setTeachingPointId(item.getTeachingPointId());
        stepResult.setTaskStepId(item.getRelatedTaskStepId());
        stepResult.setStepCode(taskStep == null ? item.getRelatedTaskStepId() : taskStep.getStepCode());
        stepResult.setSequenceNo(taskStep == null ? 1L : taskStep.getSequenceNo());
        stepResult.setScore(BigDecimal.ZERO);
        stepResult.setMaxScore(BigDecimal.ZERO);
        stepResult.setStartTime(attempt.getStartTime());
        stepResult.setEndTime(now);
        stepResult.setErrorCount(0L);
        stepResult.setHintCount(0L);
        stepResult.setRetryCount(0L);
        stepResult.setCreateBy(operatorId);
        return stepResult;
    }

    /**
     * 从评分结果证据中提取每个评分项的得分。
     *
     * @param evidenceJson 自动评分证据 JSON。
     * @return itemId 到得分的映射。
     */
    private Map<String, BigDecimal> evidenceScoresByItemId(String evidenceJson) {
        Map<String, BigDecimal> scores = new HashMap<>();
        if (!StringUtils.hasText(evidenceJson)) {
            return scores;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(evidenceJson, Map.class);
            Object items = root.get("items");
            if (!(items instanceof List)) {
                return scores;
            }
            for (Object item : (List<?>) items) {
                if (item instanceof Map) {
                    addEvidenceScore(scores, (Map<?, ?>) item);
                }
            }
            return scores;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 解析单条评分项证据。
     *
     * @param scores itemId 到得分的映射。
     * @param evidence 单条证据。
     */
    private void addEvidenceScore(Map<String, BigDecimal> scores, Map<?, ?> evidence) {
        Object itemId = evidence.get("itemId");
        Object score = evidence.get("score");
        if (itemId == null || score == null) {
            return;
        }
        scores.put(String.valueOf(itemId), new BigDecimal(String.valueOf(score)));
    }

    /**
     * 查询评分项关联的任务步骤。
     *
     * @param execution 学生执行主记录。
     * @param item 评分项。
     * @return 命中的任务步骤；未命中时返回 null。
     */
    private TaskStep findTaskStep(TaskExecution execution, EvaluationItem item) {
        if (!StringUtils.hasText(item.getTeachingPointId())) {
            return null;
        }
        List<TaskStep> steps = taskStepService.listByTaskAndTeachingPoint(
                execution.getTenantId(), execution.getTaskId(), item.getTeachingPointId());
        if (steps == null) {
            return null;
        }
        for (TaskStep step : steps) {
            if (item.getRelatedTaskStepId().equals(step.getId())) {
                return step;
            }
        }
        return null;
    }

    /**
     * 将空分数视为 0，避免聚合时出现空指针。
     *
     * @param value 原始分数。
     * @return 非空分数。
     */
    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<EvaluationResult> triggerAutoEvaluation(TaskExecution execution, String operatorId,
                                                         List<EvaluationRule> rules) {
        List<EvaluationResult> evaluationResults = new ArrayList<>();
        for (EvaluationRule rule : rules) {
            EvaluationResult result = new EvaluationResult();
            result.setId(generateId());
            result.setTenantId(execution.getTenantId());
            result.setExecutionId(execution.getId());
            result.setEvaluationRuleId(rule.getId());
            result.setCreateBy(operatorId);
            result.setUpdateBy(operatorId);
            EvaluationResult saved = autoEvaluationService.generateAutoEvaluation(result);
            if (saved == null) {
                throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
            }
            evaluationResults.add(saved);
        }
        return evaluationResults;
    }

    /**
     * 将评分结果聚合回执行主记录。
     *
     * @param execution 待完成的学生任务执行记录。
     * @param operatorId 触发提交的学生或系统操作人 ID。
     * @param now 完成时间。
     * @param evaluationResults 已生成的评分结果列表。
     */
    private void fillSubmitResult(TaskExecution execution, String operatorId, LocalDateTime now,
                                  List<EvaluationResult> evaluationResults) {
        BigDecimal totalScore = totalScore(evaluationResults);
        execution.setExecutionStatus(COMPLETED_STATUS);
        execution.setEndTime(now);
        execution.setScore(totalScore);
        execution.setResultSummary("自动评分完成：生成 " + evaluationResults.size()
                + " 条评分结果，总分 " + totalScore + "。");
        execution.setUpdateBy(operatorId);
        execution.setUpdateTime(now);
    }

    /**
     * 汇总评分结果得分，优先采用人工可修正后的 finalScore，缺失时退回 autoScore。
     *
     * @param evaluationResults 已生成的评分结果列表。
     * @return 本次执行总分。
     */
    private BigDecimal totalScore(List<EvaluationResult> evaluationResults) {
        BigDecimal totalScore = BigDecimal.ZERO;
        for (EvaluationResult result : evaluationResults) {
            BigDecimal score = result.getFinalScore() == null ? result.getAutoScore() : result.getFinalScore();
            if (score != null) {
                totalScore = totalScore.add(score);
            }
        }
        return totalScore.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 查询任务发布范围内的评分规则。
     *
     * @param execution 学生任务执行记录。
     * @return 去重后的评分规则列表。
     */
    private List<EvaluationRule> listEvaluationRulesForTask(TaskExecution execution) {
        Map<String, EvaluationRule> rules = new LinkedHashMap<>();
        List<TaskTeachingPoint> teachingPoints = taskPublishService.listTeachingPointsByTask(
                execution.getTenantId(), execution.getTaskId());
        for (TaskTeachingPoint teachingPoint : teachingPoints) {
            addRules(rules, evaluationConfigService.listRules(
                    execution.getTenantId(), execution.getTaskId(), teachingPoint.getTeachingPointId()));
        }
        addRules(rules, evaluationConfigService.listRules(execution.getTenantId(), execution.getTaskId(), null));
        return new ArrayList<>(rules.values());
    }

    /**
     * 将评分规则按 ID 去重加入结果集。
     *
     * @param target 评分规则去重集合。
     * @param source 待加入评分规则列表。
     */
    private void addRules(Map<String, EvaluationRule> target, List<EvaluationRule> source) {
        for (EvaluationRule rule : source) {
            if (rule != null && StringUtils.hasText(rule.getId())) {
                target.put(rule.getId(), rule);
            }
        }
    }

    /**
     * 查询单次学生任务执行。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行记录 ID。
     * @return 学生任务执行记录。
     */
    @Override
    public TaskExecution getExecution(String tenantId, String executionId) {
        requireText(tenantId);
        requireText(executionId);
        TaskExecution execution = taskExecutionMapper.selectOne(new QueryWrapper<TaskExecution>()
                .eq("tenant_id", tenantId)
                .eq("id", executionId)
                .eq("deleted", Boolean.FALSE));
        if (execution == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return execution;
    }

    /**
     * 按学生和任务查询执行历史。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @return 学生任务执行记录列表。
     */
    @Override
    public List<TaskExecution> listByStudentAndTask(String tenantId, String studentId, String taskId) {
        requireText(tenantId);
        requireText(studentId);
        requireText(taskId);
        return taskExecutionMapper.selectList(new QueryWrapper<TaskExecution>()
                .eq("tenant_id", tenantId)
                .eq("student_id", studentId)
                .eq("task_id", taskId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验开始执行所需的最小字段。
     *
     * @param execution 学生任务执行记录。
     */
    private void validateStartFields(TaskExecution execution) {
        if (execution == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(execution.getId());
        requireText(execution.getTenantId());
        requireText(execution.getTaskId());
        requireText(execution.getStudentId());
        requireText(execution.getConnectorSystemId());
        requireText(execution.getExecutionMode());
        requireText(execution.getSdkMode());
    }

    /**
     * 校验任务是否已经发布给学生消费。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     */
    private Task validateTaskPublished(String tenantId, String taskId) {
        Task task = taskMapper.selectOne(new QueryWrapper<Task>()
                .eq("tenant_id", tenantId)
                .eq("id", taskId)
                .eq("deleted", Boolean.FALSE));
        if (task == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!PUBLISHED_TASK_STATUS.equals(task.getTaskStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return task;
    }

    /**
     * 构造学生执行上下文快照。
     *
     * @param execution 已创建的学生任务执行记录。
     * @param task 已发布任务。
     * @return 任务执行上下文快照。
     */
    private TaskExecutionContext buildExecutionContext(TaskExecution execution, Task task) {
        List<TaskTeachingPoint> teachingPoints = taskPublishService.listTeachingPointsByTask(
                execution.getTenantId(), execution.getTaskId());
        if (teachingPoints.isEmpty()) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }

        List<Map<String, Object>> teachingPointSnapshots = new ArrayList<>();
        List<Map<String, Object>> resourceSnapshots = new ArrayList<>();
        List<Map<String, Object>> evaluationSnapshots = new ArrayList<>();
        for (TaskTeachingPoint teachingPoint : teachingPoints) {
            String teachingPointId = teachingPoint.getTeachingPointId();
            List<TaskStep> steps = taskStepService.listByTaskAndTeachingPoint(
                    execution.getTenantId(), execution.getTaskId(), teachingPointId);
            if (steps.isEmpty()) {
                throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
            }
            teachingPointSnapshots.add(toTeachingPointSnapshot(teachingPoint, steps));
            resourceSnapshots.addAll(toResourceSnapshots(teachingPointId, steps));
            evaluationSnapshots.addAll(toEvaluationSnapshots(execution, teachingPointId));
        }
        if (evaluationSnapshots.isEmpty()) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }

        TaskExecutionContext context = new TaskExecutionContext();
        context.setId(generateId());
        context.setTenantId(execution.getTenantId());
        context.setExecutionId(execution.getId());
        context.setTaskId(execution.getTaskId());
        context.setStudentId(execution.getStudentId());
        context.setSdkMode(execution.getSdkMode());
        context.setContextJson(toJson(toRuntimeContextMap(execution, task)));
        context.setOverlayPolicyJson(task.getOverlayPolicyJson());
        context.setTeachingPointSnapshotJson(toJson(teachingPointSnapshots));
        context.setResourceSnapshotJson(toJson(resourceSnapshots));
        context.setEvaluationSnapshotJson(toJson(evaluationSnapshots));
        context.setCreateBy(execution.getStudentId());
        context.setUpdateBy(execution.getStudentId());
        return context;
    }

    /**
     * 构造 SDK 运行上下文摘要。
     *
     * @param execution 学生任务执行记录。
     * @param task 已发布任务。
     * @return 运行上下文摘要。
     */
    private Map<String, Object> toRuntimeContextMap(TaskExecution execution, Task task) {
        Map<String, Object> context = new HashMap<>();
        context.put("executionId", execution.getId());
        context.put("taskId", execution.getTaskId());
        context.put("studentId", execution.getStudentId());
        context.put("executionMode", execution.getExecutionMode());
        context.put("sdkMode", execution.getSdkMode());
        context.put("taskType", task.getTaskType());
        context.put("taskName", task.getTaskName());
        context.put("timeLimitMinutes", task.getTimeLimitMinutes());
        context.put("startTime", String.valueOf(execution.getStartTime()));
        return context;
    }

    /**
     * 构造教学点及步骤快照。
     *
     * @param teachingPoint 任务教学点关联。
     * @param steps 教学步骤列表。
     * @return 教学点快照。
     */
    private Map<String, Object> toTeachingPointSnapshot(TaskTeachingPoint teachingPoint, List<TaskStep> steps) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("teachingPointId", teachingPoint.getTeachingPointId());
        snapshot.put("requiredFlag", teachingPoint.getRequiredFlag());
        snapshot.put("sequenceNo", teachingPoint.getSequenceNo());
        List<Map<String, Object>> stepSnapshots = new ArrayList<>();
        for (TaskStep step : steps) {
            stepSnapshots.add(toStepSnapshot(step));
        }
        snapshot.put("steps", stepSnapshots);
        return snapshot;
    }

    /**
     * 构造单个步骤快照。
     *
     * @param step 教学步骤。
     * @return 步骤快照。
     */
    private Map<String, Object> toStepSnapshot(TaskStep step) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("taskStepId", step.getId());
        snapshot.put("stepCode", step.getStepCode());
        snapshot.put("stepName", step.getStepName());
        snapshot.put("sequenceNo", step.getSequenceNo());
        snapshot.put("guideContent", step.getGuideContent());
        snapshot.put("practiceHint", step.getPracticeHint());
        snapshot.put("required", step.getRequired());
        snapshot.put("allowSkip", step.getAllowSkip());
        snapshot.put("relatedResourceIds", step.getRelatedResourceIds());
        return snapshot;
    }

    /**
     * 从步骤中提取资源引用快照。
     *
     * @param teachingPointId 教学点 ID。
     * @param steps 教学步骤列表。
     * @return 资源引用快照列表。
     */
    private List<Map<String, Object>> toResourceSnapshots(String teachingPointId, List<TaskStep> steps) {
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (TaskStep step : steps) {
            if (!StringUtils.hasText(step.getRelatedResourceIds())) {
                continue;
            }
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("teachingPointId", teachingPointId);
            snapshot.put("taskStepId", step.getId());
            snapshot.put("relatedResourceIds", step.getRelatedResourceIds());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /**
     * 构造评分规则和评分项快照。
     *
     * @param execution 学生任务执行记录。
     * @param teachingPointId 教学点 ID。
     * @return 评分快照列表。
     */
    private List<Map<String, Object>> toEvaluationSnapshots(TaskExecution execution, String teachingPointId) {
        List<EvaluationRule> rules = evaluationConfigService.listRules(
                execution.getTenantId(), execution.getTaskId(), teachingPointId);
        if (rules.isEmpty()) {
            rules = evaluationConfigService.listRules(execution.getTenantId(), execution.getTaskId(), null);
        }
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (EvaluationRule rule : rules) {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("evaluationRuleId", rule.getId());
            snapshot.put("ruleCode", rule.getRuleCode());
            snapshot.put("ruleName", rule.getRuleName());
            snapshot.put("teachingPointId", rule.getTeachingPointId());
            snapshot.put("totalScore", rule.getTotalScore());
            snapshot.put("items", toEvaluationItemSnapshots(
                    evaluationConfigService.listItemsByRule(execution.getTenantId(), rule.getId())));
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /**
     * 构造评分项快照。
     *
     * @param items 评分项列表。
     * @return 评分项快照列表。
     */
    private List<Map<String, Object>> toEvaluationItemSnapshots(List<EvaluationItem> items) {
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (EvaluationItem item : items) {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("evaluationItemId", item.getId());
            snapshot.put("itemCode", item.getItemCode());
            snapshot.put("itemName", item.getItemName());
            snapshot.put("itemType", item.getItemType());
            snapshot.put("relatedTaskStepId", item.getRelatedTaskStepId());
            snapshot.put("relatedResourceId", item.getRelatedResourceId());
            snapshot.put("score", item.getScore());
            snapshot.put("assertionType", item.getAssertionType());
            snapshot.put("assertionConfigJson", item.getAssertionConfigJson());
            snapshot.put("failPolicy", item.getFailPolicy());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /**
     * 将快照对象序列化为 JSON。
     *
     * @param value 快照对象。
     * @return JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 补齐开始执行时的默认字段。
     *
     * @param execution 学生任务执行记录。
     */
    private void fillStartDefaults(TaskExecution execution) {
        LocalDateTime now = LocalDateTime.now();
        if (execution.getStartTime() == null) {
            execution.setStartTime(now);
        }
        execution.setExecutionStatus(RUNNING_STATUS);
        if (!StringUtils.hasText(execution.getExecutionIdentityStatus())) {
            execution.setExecutionIdentityStatus(StringUtils.hasText(execution.getExecutionIdentityJson())
                    ? READY_IDENTITY_STATUS : NONE_IDENTITY_STATUS);
        }
        if (execution.getCreateTime() == null) {
            execution.setCreateTime(now);
        }
        if (execution.getUpdateTime() == null) {
            execution.setUpdateTime(now);
        }
        if (!StringUtils.hasText(execution.getStatus())) {
            execution.setStatus(DEFAULT_STATUS);
        }
        if (execution.getDeleted() == null) {
            execution.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }
}
