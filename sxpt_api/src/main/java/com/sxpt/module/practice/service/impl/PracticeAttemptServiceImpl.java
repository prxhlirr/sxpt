package com.sxpt.module.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.mapper.PracticeAttemptMapper;
import com.sxpt.module.practice.service.PracticeAttemptService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 练习次数服务实现。
 *
 * 业务功能：
 * 1. 记录学生每一次练习，支撑后续练习步骤结果和过程分汇总。
 * 2. 控制练习状态流转，避免重复完成或客户端伪造练习次数。
 *
 * 关键流程：
 * 1. 开始练习时校验最小字段，查询同一学生同一任务下的最大 attemptNo 后加一。
 * 2. 完成练习时读取未删除记录，只允许 RUNNING 进入 COMPLETED、ABANDONED 或 ERROR。
 */
@Service
@Profile("!test")
public class PracticeAttemptServiceImpl implements PracticeAttemptService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String RUNNING_STATUS = "RUNNING";

    private static final String COMPLETED_STATUS = "COMPLETED";

    private static final String ABANDONED_STATUS = "ABANDONED";

    private static final String ERROR_STATUS = "ERROR";

    private final PracticeAttemptMapper practiceAttemptMapper;

    public PracticeAttemptServiceImpl(PracticeAttemptMapper practiceAttemptMapper) {
        this.practiceAttemptMapper = practiceAttemptMapper;
    }

    /**
     * 开始一次练习。
     *
     * @param attempt 待创建的练习次数记录。
     * @return 已创建的练习次数记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeAttempt startAttempt(PracticeAttempt attempt) {
        validateStartFields(attempt);
        fillStartDefaults(attempt);
        practiceAttemptMapper.insert(attempt);
        return attempt;
    }

    /**
     * 完成一次练习。
     *
     * @param attempt 需要回写的练习结果。
     * @return 已完成的练习次数记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PracticeAttempt finishAttempt(PracticeAttempt attempt) {
        validateFinishFields(attempt);
        PracticeAttempt existed = getActiveAttemptById(attempt.getId());
        if (!RUNNING_STATUS.equals(existed.getAttemptStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        fillFinishValues(existed, attempt);
        practiceAttemptMapper.updateById(existed);
        return existed;
    }

    /**
     * 按学生和任务查询练习次数。
     *
     * @param tenantId 租户 ID。
     * @param studentId 学生 ID。
     * @param taskId 任务 ID。
     * @return 练习次数列表。
     */
    @Override
    public List<PracticeAttempt> listByStudentAndTask(String tenantId, String studentId, String taskId) {
        requireText(tenantId);
        requireText(studentId);
        requireText(taskId);
        return practiceAttemptMapper.selectList(new QueryWrapper<PracticeAttempt>()
                .eq("tenant_id", tenantId)
                .eq("student_id", studentId)
                .eq("task_id", taskId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("attempt_no", "create_time"));
    }

    /**
     * 校验开始练习所需字段。
     *
     * @param attempt 练习次数记录。
     */
    private void validateStartFields(PracticeAttempt attempt) {
        if (attempt == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(attempt.getId());
        requireText(attempt.getTenantId());
        requireText(attempt.getExecutionId());
        requireText(attempt.getStudentId());
        requireText(attempt.getTaskId());
    }

    /**
     * 校验完成练习所需字段。
     *
     * @param attempt 练习次数记录。
     */
    private void validateFinishFields(PracticeAttempt attempt) {
        if (attempt == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(attempt.getId());
        requireText(attempt.getAttemptStatus());
        if (!COMPLETED_STATUS.equals(attempt.getAttemptStatus())
                && !ABANDONED_STATUS.equals(attempt.getAttemptStatus())
                && !ERROR_STATUS.equals(attempt.getAttemptStatus())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
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
     * 补齐开始练习时的默认字段。
     *
     * @param attempt 练习次数记录。
     */
    private void fillStartDefaults(PracticeAttempt attempt) {
        LocalDateTime now = LocalDateTime.now();
        if (attempt.getStartTime() == null) {
            attempt.setStartTime(now);
        }
        attempt.setAttemptNo(nextAttemptNo(attempt));
        attempt.setAttemptStatus(RUNNING_STATUS);
        if (attempt.getErrorCount() == null) {
            attempt.setErrorCount(0L);
        }
        if (attempt.getHintCount() == null) {
            attempt.setHintCount(0L);
        }
        if (attempt.getRollbackCount() == null) {
            attempt.setRollbackCount(0L);
        }
        if (attempt.getCreateTime() == null) {
            attempt.setCreateTime(now);
        }
        if (attempt.getUpdateTime() == null) {
            attempt.setUpdateTime(now);
        }
        if (!StringUtils.hasText(attempt.getStatus())) {
            attempt.setStatus(DEFAULT_STATUS);
        }
        if (attempt.getDeleted() == null) {
            attempt.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 计算下一次练习序号。
     *
     * @param attempt 当前待创建记录。
     * @return 下一次练习序号。
     */
    private Long nextAttemptNo(PracticeAttempt attempt) {
        PracticeAttempt latest = practiceAttemptMapper.selectOne(new QueryWrapper<PracticeAttempt>()
                .eq("tenant_id", attempt.getTenantId())
                .eq("student_id", attempt.getStudentId())
                .eq("task_id", attempt.getTaskId())
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("attempt_no")
                .last("limit 1"));
        if (latest == null || latest.getAttemptNo() == null) {
            return 1L;
        }
        return latest.getAttemptNo() + 1L;
    }

    /**
     * 按 ID 查询未删除练习记录。
     *
     * @param id 练习次数 ID。
     * @return 未删除练习记录。
     */
    private PracticeAttempt getActiveAttemptById(String id) {
        PracticeAttempt attempt = practiceAttemptMapper.selectOne(new QueryWrapper<PracticeAttempt>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (attempt == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return attempt;
    }

    /**
     * 回写完成练习时的统计字段。
     *
     * @param existed 已存在的练习记录。
     * @param update 请求回写字段。
     */
    private void fillFinishValues(PracticeAttempt existed, PracticeAttempt update) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = update.getEndTime() == null ? now : update.getEndTime();
        existed.setAttemptStatus(update.getAttemptStatus());
        existed.setEndTime(endTime);
        existed.setDurationSeconds(Duration.between(existed.getStartTime(), endTime).getSeconds());
        existed.setScore(update.getScore());
        existed.setMaxScore(update.getMaxScore());
        existed.setPassFlag(update.getPassFlag());
        existed.setErrorCount(defaultLong(update.getErrorCount()));
        existed.setHintCount(defaultLong(update.getHintCount()));
        existed.setRollbackCount(defaultLong(update.getRollbackCount()));
        existed.setUpdateBy(update.getUpdateBy());
        existed.setUpdateTime(now);
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

