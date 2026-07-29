package com.sxpt.module.teachingdata;

import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.AllocationStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.CollaborationStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataPoolStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ExamQuestionDataStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.LaunchStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementItemStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.TriggerType;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 数据准备状态枚举测试。
 *
 * 业务功能：
 * 1. 验证数据准备批次、明细、任务、数据池、实例、分配、协作、考试题目、校验和触发方式的枚举解析稳定。
 * 2. 验证终态、可重试、可领取和是否允许生成 launchToken 的关键业务判断稳定。
 *
 * 关键流程：
 * 1. 使用纯单元测试验证枚举，不依赖 Spring、数据库和 Redis。
 * 2. 先固化状态基线，再让后续 Service 按枚举实现状态机。
 */
class DataPrepareStatusEnumsTests {

    /**
     * 验证数据需求批次状态可以按持久化值解析，并正确识别终态。
     */
    @Test
    void requirementStatusShouldParseValueAndExposeTerminalFlag() {
        assertSame(RequirementStatus.CREATED, RequirementStatus.fromValue("CREATED"));
        assertSame(RequirementStatus.PARTIAL_FAILED, RequirementStatus.fromValue("PARTIAL_FAILED"));
        assertFalse(RequirementStatus.PREPARING.isTerminal());
        assertTrue(RequirementStatus.READY.isTerminal());
        assertNull(RequirementStatus.fromValue(" "));
    }

    /**
     * 验证需求项状态可以表达逐条准备结果，避免批量任务只能整体成功或整体失败。
     */
    @Test
    void requirementItemStatusShouldParseValueAndExposeTerminalFlag() {
        assertSame(RequirementItemStatus.REQUESTED, RequirementItemStatus.fromValue("REQUESTED"));
        assertSame(RequirementItemStatus.VALIDATION_FAILED, RequirementItemStatus.fromValue("VALIDATION_FAILED"));
        assertFalse(RequirementItemStatus.REQUESTED.isTerminal());
        assertTrue(RequirementItemStatus.DISCARDED.isTerminal());
        assertNull(RequirementItemStatus.fromValue("UNKNOWN_ITEM_STATUS"));
    }

    /**
     * 验证数据准备任务状态可以表达失败补偿和终态。
     */
    @Test
    void prepareJobStatusShouldParseValueAndExposeRetryableFlag() {
        assertSame(PrepareJobStatus.CREATED, PrepareJobStatus.fromValue("CREATED"));
        assertSame(PrepareJobStatus.FAILED, PrepareJobStatus.fromValue("FAILED"));
        assertFalse(PrepareJobStatus.RUNNING.isTerminal());
        assertTrue(PrepareJobStatus.SUCCESS.isTerminal());
        assertTrue(PrepareJobStatus.FAILED.isRetryable());
        assertFalse(PrepareJobStatus.CANCELLED.isRetryable());
    }

    /**
     * 验证只有 READY 数据池允许进入领取流程。
     */
    @Test
    void dataPoolStatusShouldOnlyAllowAllocationWhenReady() {
        assertSame(DataPoolStatus.READY, DataPoolStatus.fromValue("READY"));
        assertTrue(DataPoolStatus.READY.isAllocatable());
        assertFalse(DataPoolStatus.CREATED.isAllocatable());
        assertFalse(DataPoolStatus.EXHAUSTED.isAllocatable());
        assertTrue(DataPoolStatus.CLOSED.isTerminal());
    }

    /**
     * 验证教学数据实例状态可以表达锁定、废弃和归档生命周期。
     */
    @Test
    void dataInstanceStatusShouldParseValueAndExposeTerminalFlag() {
        assertSame(DataInstanceStatus.LOCKED, DataInstanceStatus.fromValue("LOCKED"));
        assertSame(DataInstanceStatus.RESET, DataInstanceStatus.fromValue("RESET"));
        assertFalse(DataInstanceStatus.RESET.isTerminal());
        assertFalse(DataInstanceStatus.ALLOCATED.isTerminal());
        assertTrue(DataInstanceStatus.ARCHIVED.isTerminal());
        assertTrue(DataInstanceStatus.DISCARDED.isTerminal());
    }

    /**
     * 验证分配状态可以表达领取、消费、释放和废弃。
     */
    @Test
    void allocationStatusShouldParseValueAndExposeTerminalFlag() {
        assertSame(AllocationStatus.ALLOCATED, AllocationStatus.fromValue("ALLOCATED"));
        assertFalse(AllocationStatus.ALLOCATED.isTerminal());
        assertTrue(AllocationStatus.CONSUMED.isTerminal());
        assertTrue(AllocationStatus.RELEASED.isTerminal());
    }

    /**
     * 验证协作单元状态可以表达整体办理和整体废弃。
     */
    @Test
    void collaborationStatusShouldParseValueAndExposeTerminalFlag() {
        assertSame(CollaborationStatus.IN_PROGRESS, CollaborationStatus.fromValue("IN_PROGRESS"));
        assertFalse(CollaborationStatus.CREATED.isTerminal());
        assertTrue(CollaborationStatus.COMPLETED.isTerminal());
        assertTrue(CollaborationStatus.DISCARDED.isTerminal());
    }

    /**
     * 验证考试题目数据状态可以表达锁定、提交和评分。
     */
    @Test
    void examQuestionDataStatusShouldParseValueAndExposeTerminalFlag() {
        assertSame(ExamQuestionDataStatus.LOCKED, ExamQuestionDataStatus.fromValue("LOCKED"));
        assertFalse(ExamQuestionDataStatus.SUBMITTED.isTerminal());
        assertTrue(ExamQuestionDataStatus.SCORED.isTerminal());
        assertTrue(ExamQuestionDataStatus.ABNORMAL.isTerminal());
    }

    /**
     * 验证启动上下文状态可以表达 token 校验、使用、过期和失败。
     */
    @Test
    void launchStatusShouldParseValueAndExposeTerminalFlag() {
        assertSame(LaunchStatus.CREATED, LaunchStatus.fromValue("CREATED"));
        assertSame(LaunchStatus.VERIFIED, LaunchStatus.fromValue("VERIFIED"));
        assertFalse(LaunchStatus.CREATED.isTerminal());
        assertFalse(LaunchStatus.VERIFIED.isTerminal());
        assertTrue(LaunchStatus.USED.isTerminal());
        assertTrue(LaunchStatus.EXPIRED.isTerminal());
        assertTrue(LaunchStatus.FAILED.isTerminal());
    }

    /**
     * 验证只有校验通过的数据允许生成 launchToken 进入原平台。
     */
    @Test
    void validationStatusShouldOnlyAllowLaunchWhenPassed() {
        assertSame(ValidationStatus.PASSED, ValidationStatus.fromValue("PASSED"));
        assertTrue(ValidationStatus.PASSED.isLaunchAllowed());
        assertFalse(ValidationStatus.NOT_CHECKED.isLaunchAllowed());
        assertFalse(ValidationStatus.FAILED.isLaunchAllowed());
        assertFalse(ValidationStatus.UNKNOWN.isLaunchAllowed());
    }

    /**
     * 验证通用记录状态可以统一表达业务启停。
     */
    @Test
    void recordStatusShouldParseValue() {
        assertSame(RecordStatus.ACTIVE, RecordStatus.fromValue("ACTIVE"));
        assertSame(RecordStatus.DISABLED, RecordStatus.fromValue("DISABLED"));
        assertNull(RecordStatus.fromValue("DELETED"));
    }

    /**
     * 验证数据准备触发方式可以按持久化值解析，支撑后续幂等键和审计分类。
     */
    @Test
    void triggerTypeShouldParseValue() {
        assertSame(TriggerType.MANUAL, TriggerType.fromValue("MANUAL"));
        assertSame(TriggerType.PUBLISH, TriggerType.fromValue("PUBLISH"));
        assertSame(TriggerType.ON_DEMAND, TriggerType.fromValue("ON_DEMAND"));
        assertSame(TriggerType.RETRY, TriggerType.fromValue("RETRY"));
        assertNull(TriggerType.fromValue(null));
    }
}
