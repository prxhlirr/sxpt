package com.sxpt.module.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.mapper.PracticeAttemptMapper;
import com.sxpt.module.practice.mapper.PracticeScoreSummaryMapper;
import com.sxpt.module.practice.mapper.PracticeStepResultMapper;
import com.sxpt.module.practice.service.PracticeScoreSummaryService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 练习过程分汇总服务实现。
 *
 * 业务功能：
 * 1. 按学生、任务、教学点汇总练习次数、完成率和过程分。
 * 2. 将失败或未通过的步骤整理为薄弱步骤摘要，供教师定位辅导重点。
 *
 * 关键流程：
 * 1. 按范围读取未删除 practice_attempt，计算最高分、最近分、平均分和完成率。
 * 2. 按练习次数 ID 读取失败步骤，序列化为 weakStepJson。
 * 3. 按 tenantId、studentId、taskId、teachingPointId 幂等更新汇总。
 */
@Service
@Profile("!test")
public class PracticeScoreSummaryServiceImpl implements PracticeScoreSummaryService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String COMPLETED_STATUS = "COMPLETED";

    private static final String FAILED_STATUS = "FAILED";

    private static final String SCORE_POLICY = "WEIGHTED";

    private static final BigDecimal BEST_WEIGHT = new BigDecimal("0.50");

    private static final BigDecimal LAST_WEIGHT = new BigDecimal("0.30");

    private static final BigDecimal COMPLETION_WEIGHT = new BigDecimal("0.20");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private static final int MAX_WEAK_STEP_COUNT = 5;

    private final PracticeScoreSummaryMapper practiceScoreSummaryMapper;

    private final PracticeAttemptMapper practiceAttemptMapper;

    private final PracticeStepResultMapper practiceStepResultMapper;

    private final ObjectMapper objectMapper;

    public PracticeScoreSummaryServiceImpl(PracticeScoreSummaryMapper practiceScoreSummaryMapper,
                                           PracticeAttemptMapper practiceAttemptMapper,
                                           PracticeStepResultMapper practiceStepResultMapper,
                                           ObjectMapper objectMapper) {
        this.practiceScoreSummaryMapper = practiceScoreSummaryMapper;
        this.practiceAttemptMapper = practiceAttemptMapper;
        this.practiceStepResultMapper = practiceStepResultMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成或重算练习过程分汇总。
     *
     * @param scope 汇总范围。
     * @return 已保存的练习过程分汇总。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeScoreSummary generateSummary(PracticeScoreSummary scope) {
        validateScope(scope);
        List<PracticeAttempt> attempts = listAttempts(scope);
        PracticeScoreSummary calculated = calculateSummary(scope, attempts);
        PracticeScoreSummary existed = findExistingSummary(scope);
        if (existed != null) {
            copyCalculatedFields(existed, calculated);
            practiceScoreSummaryMapper.updateById(existed);
            return existed;
        }
        fillCreateDefaults(calculated);
        practiceScoreSummaryMapper.insert(calculated);
        return calculated;
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
    @Override
    public PracticeScoreSummary getSummary(String tenantId, String studentId, String taskId, String teachingPointId) {
        requireText(tenantId);
        requireText(studentId);
        requireText(taskId);
        PracticeScoreSummary summary = practiceScoreSummaryMapper.selectOne(summaryScopeWrapper(
                tenantId, studentId, taskId, teachingPointId));
        if (summary == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return summary;
    }

    /**
     * 校验汇总范围最小字段。
     *
     * @param scope 汇总范围。
     */
    private void validateScope(PracticeScoreSummary scope) {
        if (scope == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(scope.getId());
        requireText(scope.getTenantId());
        requireText(scope.getStudentId());
        requireText(scope.getTaskId());
    }

    /**
     * 查询汇总范围内的练习次数。
     *
     * @param scope 汇总范围。
     * @return 练习次数列表。
     */
    private List<PracticeAttempt> listAttempts(PracticeScoreSummary scope) {
        QueryWrapper<PracticeAttempt> wrapper = new QueryWrapper<PracticeAttempt>()
                .eq("tenant_id", scope.getTenantId())
                .eq("student_id", scope.getStudentId())
                .eq("task_id", scope.getTaskId())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("attempt_no", "create_time");
        if (StringUtils.hasText(scope.getTeachingPointId())) {
            wrapper.eq("teaching_point_id", scope.getTeachingPointId());
        } else {
            wrapper.isNull("teaching_point_id");
        }
        return practiceAttemptMapper.selectList(wrapper);
    }

    /**
     * 根据练习次数和步骤结果计算汇总。
     *
     * @param scope 汇总范围。
     * @param attempts 练习次数列表。
     * @return 已计算但尚未持久化的汇总。
     */
    private PracticeScoreSummary calculateSummary(PracticeScoreSummary scope, List<PracticeAttempt> attempts) {
        PracticeScoreSummary summary = new PracticeScoreSummary();
        summary.setId(scope.getId());
        summary.setTenantId(scope.getTenantId());
        summary.setStudentId(scope.getStudentId());
        summary.setClassId(scope.getClassId());
        summary.setCourseId(scope.getCourseId());
        summary.setTaskId(scope.getTaskId());
        summary.setTeachingPointId(scope.getTeachingPointId());
        summary.setPracticeCount((long) attempts.size());
        summary.setCompleteCount(countCompleted(attempts));
        summary.setBestScore(bestScore(attempts));
        summary.setLastScore(lastScore(attempts));
        summary.setAvgScore(avgScore(attempts));
        summary.setCompletionRate(completionRate(summary.getCompleteCount(), summary.getPracticeCount()));
        summary.setFinalPracticeScore(finalPracticeScore(summary));
        summary.setScorePolicy(SCORE_POLICY);
        summary.setWeakStepJson(weakStepJson(attempts));
        summary.setSummaryTime(LocalDateTime.now());
        summary.setCreateBy(scope.getCreateBy());
        summary.setUpdateBy(scope.getCreateBy());
        return summary;
    }

    /**
     * 统计完成次数。
     *
     * @param attempts 练习次数列表。
     * @return 完成次数。
     */
    private Long countCompleted(List<PracticeAttempt> attempts) {
        long count = 0L;
        for (PracticeAttempt attempt : attempts) {
            if (COMPLETED_STATUS.equals(attempt.getAttemptStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算最高分。
     *
     * @param attempts 练习次数列表。
     * @return 最高分；没有得分时返回 null。
     */
    private BigDecimal bestScore(List<PracticeAttempt> attempts) {
        BigDecimal best = null;
        for (PracticeAttempt attempt : attempts) {
            if (attempt.getScore() != null && (best == null || attempt.getScore().compareTo(best) > 0)) {
                best = attempt.getScore();
            }
        }
        return scaleScore(best);
    }

    /**
     * 读取最近一次有分数的练习得分。
     *
     * @param attempts 已按练习序号升序排列的练习次数列表。
     * @return 最近一次得分；没有得分时返回 null。
     */
    private BigDecimal lastScore(List<PracticeAttempt> attempts) {
        for (int i = attempts.size() - 1; i >= 0; i--) {
            if (attempts.get(i).getScore() != null) {
                return scaleScore(attempts.get(i).getScore());
            }
        }
        return null;
    }

    /**
     * 计算平均分。
     *
     * @param attempts 练习次数列表。
     * @return 平均分；没有得分时返回 null。
     */
    private BigDecimal avgScore(List<PracticeAttempt> attempts) {
        BigDecimal total = BigDecimal.ZERO;
        long count = 0L;
        for (PracticeAttempt attempt : attempts) {
            if (attempt.getScore() != null) {
                total = total.add(attempt.getScore());
                count++;
            }
        }
        if (count == 0L) {
            return null;
        }
        return total.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算完成率。
     *
     * @param completeCount 完成次数。
     * @param practiceCount 练习总次数。
     * @return 完成率，范围 0 到 1。
     */
    private BigDecimal completionRate(Long completeCount, Long practiceCount) {
        if (practiceCount == null || practiceCount == 0L) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return new BigDecimal(completeCount)
                .divide(new BigDecimal(practiceCount), 4, RoundingMode.HALF_UP);
    }

    /**
     * 按 MVP 加权策略计算最终练习过程分。
     *
     * @param summary 已计算基础指标的汇总。
     * @return 最终练习过程分。
     */
    private BigDecimal finalPracticeScore(PracticeScoreSummary summary) {
        BigDecimal best = defaultScore(summary.getBestScore());
        BigDecimal last = defaultScore(summary.getLastScore());
        BigDecimal completionScore = summary.getCompletionRate().multiply(HUNDRED);
        return best.multiply(BEST_WEIGHT)
                .add(last.multiply(LAST_WEIGHT))
                .add(completionScore.multiply(COMPLETION_WEIGHT))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成薄弱步骤摘要 JSON。
     *
     * @param attempts 练习次数列表。
     * @return 薄弱步骤摘要 JSON。
     */
    private String weakStepJson(List<PracticeAttempt> attempts) {
        List<String> attemptIds = attemptIds(attempts);
        if (attemptIds.isEmpty()) {
            return "[]";
        }
        List<PracticeStepResult> stepResults = practiceStepResultMapper.selectList(new QueryWrapper<PracticeStepResult>()
                .in("attempt_id", attemptIds)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "create_time"));
        List<Map<String, Object>> weakSteps = new ArrayList<>();
        for (PracticeStepResult stepResult : stepResults) {
            if (!isWeakStep(stepResult)) {
                continue;
            }
            weakSteps.add(toWeakStepMap(stepResult));
            if (weakSteps.size() >= MAX_WEAK_STEP_COUNT) {
                break;
            }
        }
        try {
            return objectMapper.writeValueAsString(weakSteps);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 提取练习次数 ID。
     *
     * @param attempts 练习次数列表。
     * @return 练习次数 ID 列表。
     */
    private List<String> attemptIds(List<PracticeAttempt> attempts) {
        List<String> ids = new ArrayList<>();
        for (PracticeAttempt attempt : attempts) {
            ids.add(attempt.getId());
        }
        return ids;
    }

    /**
     * 判断步骤是否属于薄弱步骤。
     *
     * @param stepResult 步骤结果。
     * @return 失败或未通过时返回 true。
     */
    private boolean isWeakStep(PracticeStepResult stepResult) {
        return FAILED_STATUS.equals(stepResult.getResultStatus()) || Boolean.FALSE.equals(stepResult.getPassFlag());
    }

    /**
     * 将步骤结果转换为薄弱步骤摘要。
     *
     * @param stepResult 步骤结果。
     * @return 薄弱步骤摘要。
     */
    private Map<String, Object> toWeakStepMap(PracticeStepResult stepResult) {
        Map<String, Object> weakStep = new HashMap<>();
        weakStep.put("attemptId", stepResult.getAttemptId());
        weakStep.put("taskStepId", stepResult.getTaskStepId());
        weakStep.put("stepCode", stepResult.getStepCode());
        weakStep.put("sequenceNo", stepResult.getSequenceNo());
        weakStep.put("resultStatus", stepResult.getResultStatus());
        weakStep.put("errorCount", stepResult.getErrorCount());
        weakStep.put("durationSeconds", stepResult.getDurationSeconds());
        return weakStep;
    }

    /**
     * 查询已有汇总。
     *
     * @param scope 汇总范围。
     * @return 已有汇总；未命中时返回 null。
     */
    private PracticeScoreSummary findExistingSummary(PracticeScoreSummary scope) {
        return practiceScoreSummaryMapper.selectOne(summaryScopeWrapper(
                scope.getTenantId(), scope.getStudentId(), scope.getTaskId(), scope.getTeachingPointId()));
    }

    /**
     * 构造汇总唯一范围查询条件。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 查询条件。
     */
    private QueryWrapper<PracticeScoreSummary> summaryScopeWrapper(String tenantId, String studentId,
                                                                   String taskId, String teachingPointId) {
        QueryWrapper<PracticeScoreSummary> wrapper = new QueryWrapper<PracticeScoreSummary>()
                .eq("tenant_id", tenantId)
                .eq("student_id", studentId)
                .eq("task_id", taskId)
                .eq("deleted", Boolean.FALSE);
        if (StringUtils.hasText(teachingPointId)) {
            wrapper.eq("teaching_point_id", teachingPointId);
        } else {
            wrapper.isNull("teaching_point_id");
        }
        return wrapper;
    }

    /**
     * 将计算结果复制到已有汇总。
     *
     * @param target 已有汇总。
     * @param source 新计算的汇总。
     */
    private void copyCalculatedFields(PracticeScoreSummary target, PracticeScoreSummary source) {
        target.setClassId(source.getClassId());
        target.setCourseId(source.getCourseId());
        target.setPracticeCount(source.getPracticeCount());
        target.setCompleteCount(source.getCompleteCount());
        target.setBestScore(source.getBestScore());
        target.setLastScore(source.getLastScore());
        target.setAvgScore(source.getAvgScore());
        target.setCompletionRate(source.getCompletionRate());
        target.setFinalPracticeScore(source.getFinalPracticeScore());
        target.setScorePolicy(source.getScorePolicy());
        target.setWeakStepJson(source.getWeakStepJson());
        target.setSummaryTime(source.getSummaryTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 补齐新建汇总默认字段。
     *
     * @param summary 练习过程分汇总。
     */
    private void fillCreateDefaults(PracticeScoreSummary summary) {
        LocalDateTime now = LocalDateTime.now();
        if (summary.getCreateTime() == null) {
            summary.setCreateTime(now);
        }
        if (summary.getUpdateTime() == null) {
            summary.setUpdateTime(now);
        }
        if (!StringUtils.hasText(summary.getStatus())) {
            summary.setStatus(DEFAULT_STATUS);
        }
        if (summary.getDeleted() == null) {
            summary.setDeleted(Boolean.FALSE);
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

    /**
     * 将空分数归零。
     *
     * @param value 原始分数。
     * @return 非空分数。
     */
    private BigDecimal defaultScore(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 统一分数精度。
     *
     * @param value 原始分数。
     * @return 两位小数分数。
     */
    private BigDecimal scaleScore(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}

