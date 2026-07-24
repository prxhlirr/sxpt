package com.sxpt.module.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.mapper.PracticeStepResultMapper;
import com.sxpt.module.practice.service.PracticeStepResultService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 练习步骤结果服务实现。
 *
 * 业务功能：
 * 1. 保存每次练习的步骤级结果，让教师和平台能看到学生卡在哪个 task_step。
 * 2. 对 SDK 重试和重复评分做幂等处理，避免同一练习步骤产生多条结果。
 *
 * 关键流程：
 * 1. 校验租户、练习次数、任务步骤、步骤序号和结果状态。
 * 2. 按 tenantId、attemptId、taskStepId 查询已有记录，存在则更新结果字段，不存在则补齐默认值后插入。
 */
@Service
@Profile("!test")
public class PracticeStepResultServiceImpl implements PracticeStepResultService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String PASSED_STATUS = "PASSED";

    private static final String FAILED_STATUS = "FAILED";

    private static final String SKIPPED_STATUS = "SKIPPED";

    private final PracticeStepResultMapper practiceStepResultMapper;

    public PracticeStepResultServiceImpl(PracticeStepResultMapper practiceStepResultMapper) {
        this.practiceStepResultMapper = practiceStepResultMapper;
    }

    /**
     * 上报单个练习步骤结果。
     *
     * @param stepResult 练习步骤结果。
     * @return 已保存或已更新的练习步骤结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeStepResult reportStepResult(PracticeStepResult stepResult) {
        validateReportFields(stepResult);
        PracticeStepResult existed = findExistingStepResult(stepResult);
        if (existed != null) {
            fillUpdateValues(existed, stepResult);
            practiceStepResultMapper.updateById(existed);
            return existed;
        }
        fillCreateDefaults(stepResult);
        practiceStepResultMapper.insert(stepResult);
        return stepResult;
    }

    /**
     * 按练习次数查询步骤结果。
     *
     * @param tenantId 租户 ID。
     * @param attemptId 练习次数 ID。
     * @return 步骤结果列表。
     */
    @Override
    public List<PracticeStepResult> listByAttempt(String tenantId, String attemptId) {
        requireText(tenantId);
        requireText(attemptId);
        return practiceStepResultMapper.selectList(new QueryWrapper<PracticeStepResult>()
                .eq("tenant_id", tenantId)
                .eq("attempt_id", attemptId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "create_time"));
    }

    /**
     * 校验步骤结果上报最小字段。
     *
     * @param stepResult 练习步骤结果。
     */
    private void validateReportFields(PracticeStepResult stepResult) {
        if (stepResult == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(stepResult.getId());
        requireText(stepResult.getTenantId());
        requireText(stepResult.getAttemptId());
        requireText(stepResult.getTaskStepId());
        requireText(stepResult.getResultStatus());
        if (stepResult.getSequenceNo() == null || stepResult.getSequenceNo() < 1) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!PASSED_STATUS.equals(stepResult.getResultStatus())
                && !FAILED_STATUS.equals(stepResult.getResultStatus())
                && !SKIPPED_STATUS.equals(stepResult.getResultStatus())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (stepResult.getStartTime() != null
                && stepResult.getEndTime() != null
                && stepResult.getEndTime().isBefore(stepResult.getStartTime())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 按幂等业务键查找已有步骤结果。
     *
     * @param stepResult 本次上报的练习步骤结果。
     * @return 已存在的步骤结果；未命中时返回 null。
     */
    private PracticeStepResult findExistingStepResult(PracticeStepResult stepResult) {
        return practiceStepResultMapper.selectOne(new QueryWrapper<PracticeStepResult>()
                .eq("tenant_id", stepResult.getTenantId())
                .eq("attempt_id", stepResult.getAttemptId())
                .eq("task_step_id", stepResult.getTaskStepId())
                .eq("deleted", Boolean.FALSE));
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
     * 补齐新建步骤结果的默认字段。
     *
     * @param stepResult 练习步骤结果。
     */
    private void fillCreateDefaults(PracticeStepResult stepResult) {
        LocalDateTime now = LocalDateTime.now();
        fillResultValues(stepResult, stepResult);
        if (stepResult.getCreateTime() == null) {
            stepResult.setCreateTime(now);
        }
        if (stepResult.getUpdateTime() == null) {
            stepResult.setUpdateTime(now);
        }
        if (!StringUtils.hasText(stepResult.getStatus())) {
            stepResult.setStatus(DEFAULT_STATUS);
        }
        if (stepResult.getDeleted() == null) {
            stepResult.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 回写已存在步骤结果。
     *
     * @param existed 已存在的步骤结果。
     * @param update 本次上报字段。
     */
    private void fillUpdateValues(PracticeStepResult existed, PracticeStepResult update) {
        fillResultValues(existed, update);
        existed.setUpdateBy(update.getCreateBy());
        existed.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 复制步骤结果业务字段并计算耗时。
     *
     * @param target 待写入的目标结果。
     * @param source 本次上报的源结果。
     */
    private void fillResultValues(PracticeStepResult target, PracticeStepResult source) {
        target.setExecutionId(source.getExecutionId());
        target.setStudentId(source.getStudentId());
        target.setTaskId(source.getTaskId());
        target.setStepCode(source.getStepCode());
        target.setSequenceNo(source.getSequenceNo());
        target.setResultStatus(source.getResultStatus());
        target.setScore(source.getScore());
        target.setMaxScore(source.getMaxScore());
        target.setPassFlag(source.getPassFlag());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setDurationSeconds(calculateDurationSeconds(source));
        target.setErrorCount(defaultLong(source.getErrorCount()));
        target.setHintCount(defaultLong(source.getHintCount()));
        target.setRetryCount(defaultLong(source.getRetryCount()));
        target.setEvidenceJson(source.getEvidenceJson());
        target.setFeedback(source.getFeedback());
        if (target.getCreateBy() == null) {
            target.setCreateBy(source.getCreateBy());
        }
    }

    /**
     * 计算步骤耗时秒数。
     *
     * @param stepResult 练习步骤结果。
     * @return 起止时间完整时返回耗时秒数，否则返回 null。
     */
    private Long calculateDurationSeconds(PracticeStepResult stepResult) {
        if (stepResult.getStartTime() == null || stepResult.getEndTime() == null) {
            return null;
        }
        return Duration.between(stepResult.getStartTime(), stepResult.getEndTime()).getSeconds();
    }

    /**
     * 将空计数字段归零。
     *
     * @param value 原始计数。
     * @return 非空计数。
     */
    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}

