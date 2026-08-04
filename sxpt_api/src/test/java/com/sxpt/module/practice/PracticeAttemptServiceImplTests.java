package com.sxpt.module.practice;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.mapper.PracticeAttemptMapper;
import com.sxpt.module.practice.service.PracticeAttemptService;
import com.sxpt.module.practice.service.impl.PracticeAttemptServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
 * 练习次数服务测试。
 *
 * 业务功能：
 * 1. 验证开始练习会由服务端计算 attemptNo 并补齐默认值。
 * 2. 验证完成练习只允许 RUNNING 状态流转到结束状态。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class PracticeAttemptServiceImplTests {

    private final PracticeAttemptMapper mapper = mock(PracticeAttemptMapper.class);

    private final PracticeAttemptService service = new PracticeAttemptServiceImpl(mapper);

    /**
     * 校验首次开始练习时 attemptNo 为 1 并补齐默认值。
     */
    @Test
    void startAttemptShouldInsertFirstAttemptAndFillDefaults() {
        PracticeAttempt attempt = buildStartAttempt();

        PracticeAttempt saved = service.startAttempt(attempt);

        assertSame(attempt, saved);
        assertEquals(Long.valueOf(1), saved.getAttemptNo());
        assertEquals("RUNNING", saved.getAttemptStatus());
        assertEquals(Long.valueOf(0), saved.getErrorCount());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getStartTime());
        verify(mapper).selectOne(any());
        verify(mapper).insert(saved);
    }

    /**
     * 校验已有历史练习时 attemptNo 自动加一。
     */
    @Test
    void startAttemptShouldIncreaseAttemptNoFromLatestAttempt() {
        PracticeAttempt latest = buildRunningAttempt();
        latest.setAttemptNo(3L);
        when(mapper.selectOne(any())).thenReturn(latest);
        PracticeAttempt attempt = buildStartAttempt();

        PracticeAttempt saved = service.startAttempt(attempt);

        assertEquals(Long.valueOf(4), saved.getAttemptNo());
        verify(mapper).insert(saved);
    }

    /**
     * 校验完成 RUNNING 练习时回写耗时和统计字段。
     */
    @Test
    void finishAttemptShouldCompleteRunningAttempt() {
        PracticeAttempt existed = buildRunningAttempt();
        when(mapper.selectOne(any())).thenReturn(existed);
        PracticeAttempt update = buildFinishAttempt();

        PracticeAttempt result = service.finishAttempt(update);

        assertSame(existed, result);
        assertEquals("COMPLETED", result.getAttemptStatus());
        assertEquals(Long.valueOf(930), result.getDurationSeconds());
        assertEquals(new BigDecimal("85.50"), result.getScore());
        assertEquals(Long.valueOf(2), result.getErrorCount());
        verify(mapper).updateById(existed);
    }

    /**
     * 校验非 RUNNING 状态不能重复完成。
     */
    @Test
    void finishAttemptShouldRejectFinishedAttempt() {
        PracticeAttempt existed = buildRunningAttempt();
        existed.setAttemptStatus("COMPLETED");
        when(mapper.selectOne(any())).thenReturn(existed);

        assertThrows(BusinessException.class, () -> service.finishAttempt(buildFinishAttempt()));
        verify(mapper, times(0)).updateById(existed);
    }

    /**
     * 校验按学生和任务查询练习次数时返回 Mapper 结果。
     */
    @Test
    void listByStudentAndTaskShouldReturnMapperResult() {
        PracticeAttempt attempt = buildRunningAttempt();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(attempt));

        List<PracticeAttempt> result = service.listByStudentAndTask("tenant_001", "student_001", "task_001");

        assertEquals(1, result.size());
        assertSame(attempt, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造开始练习记录。
     *
     * @return 练习次数实体。
     */
    private PracticeAttempt buildStartAttempt() {
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId("attempt_001");
        attempt.setTenantId("tenant_001");
        attempt.setExecutionId("execution_001");
        attempt.setStudentId("student_001");
        attempt.setClassId("class_001");
        attempt.setTaskId("task_001");
        attempt.setTeachingPointId("tp_001");
        attempt.setDataInstanceId("data_001");
        return attempt;
    }

    /**
     * 构造运行中的练习记录。
     *
     * @return 练习次数实体。
     */
    private PracticeAttempt buildRunningAttempt() {
        PracticeAttempt attempt = buildStartAttempt();
        attempt.setAttemptNo(1L);
        attempt.setStartTime(LocalDateTime.of(2026, 7, 16, 14, 0));
        attempt.setAttemptStatus("RUNNING");
        attempt.setErrorCount(0L);
        attempt.setHintCount(0L);
        attempt.setRollbackCount(0L);
        attempt.setStatus("ACTIVE");
        attempt.setDeleted(Boolean.FALSE);
        return attempt;
    }

    /**
     * 构造完成练习回写记录。
     *
     * @return 练习次数实体。
     */
    private PracticeAttempt buildFinishAttempt() {
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId("attempt_001");
        attempt.setTenantId("tenant_001");
        attempt.setStudentId("student_001");
        attempt.setAttemptStatus("COMPLETED");
        attempt.setEndTime(LocalDateTime.of(2026, 7, 16, 14, 15, 30));
        attempt.setScore(new BigDecimal("85.50"));
        attempt.setMaxScore(new BigDecimal("100.00"));
        attempt.setPassFlag(Boolean.TRUE);
        attempt.setErrorCount(2L);
        attempt.setHintCount(1L);
        attempt.setRollbackCount(0L);
        attempt.setUpdateBy("student_001");
        return attempt;
    }
}
