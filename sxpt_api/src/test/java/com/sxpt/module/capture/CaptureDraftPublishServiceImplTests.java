package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.capture.service.impl.CaptureDraftPublishServiceImpl;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.mapper.TaskStepMapper;
import com.sxpt.module.teaching.service.TaskStepService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Collections;

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
 * 动作草稿发布教学步骤服务测试。
 *
 * 业务功能：
 * 1. 验证已确认动作草稿可以生成教学任务步骤。
 * 2. 验证未确认或不存在的草稿不会进入教学编排。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper 和 TaskStepService，聚焦发布编排规则。
 * 2. 检查生成的 TaskStep 字段，确保步骤可追溯来源草稿和正式资源。
 */
class CaptureDraftPublishServiceImplTests {

    private final CaptureActionDraftMapper actionDraftMapper = mock(CaptureActionDraftMapper.class);

    private final TaskStepMapper taskStepMapper = mock(TaskStepMapper.class);

    private final TaskStepService taskStepService = mock(TaskStepService.class);

    private final EvaluationConfigService evaluationConfigService = mock(EvaluationConfigService.class);

    private final CaptureDraftPublishService publishService = new CaptureDraftPublishServiceImpl(
            actionDraftMapper, taskStepMapper, taskStepService, evaluationConfigService);

    /**
     * 校验已确认动作草稿可以发布为教学任务步骤。
     */
    @Test
    void publishDraftToTaskStepShouldCreateTaskStepFromConfirmedDraft() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(null);
        when(taskStepService.createTaskStep(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskStep taskStep = publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "teacher_001");

        assertNotNull(taskStep.getId());
        assertEquals("tenant_001", taskStep.getTenantId());
        assertEquals("task_001", taskStep.getTaskId());
        assertEquals("tp_001", taskStep.getTeachingPointId());
        assertEquals("CAPTURE_DRAFT_draft_001", taskStep.getStepCode());
        assertEquals("提交申请表", taskStep.getStepName());
        assertEquals("提交备案申请", taskStep.getStepDescription());
        assertEquals(Long.valueOf(3), taskStep.getSequenceNo());
        assertEquals("[\"res_001\"]", taskStep.getRelatedResourceIds());
        assertEquals("点击提交按钮。", taskStep.getGuideContent());
        assertEquals("提交前检查必填项。", taskStep.getPracticeHint());
        assertEquals("draft_001", taskStep.getSourceActionDraftId());
        assertEquals("teacher_001", taskStep.getCreateBy());
        assertEquals("teacher_001", taskStep.getUpdateBy());
    }

    /**
     * 校验草稿缺少确认步骤名时使用动作名称兜底。
     */
    @Test
    void publishDraftToTaskStepShouldCreateTraceExistsEvaluationItem() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(null);
        when(taskStepService.createTaskStep(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evaluationConfigService.createEvaluationItem(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskStep taskStep = publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001");

        ArgumentCaptor<EvaluationItem> captor = ArgumentCaptor.forClass(EvaluationItem.class);
        verify(evaluationConfigService).createEvaluationItem(captor.capture());
        EvaluationItem item = captor.getValue();
        assertNotNull(item.getId());
        assertEquals("tenant_001", item.getTenantId());
        assertEquals("rule_001", item.getEvaluationRuleId());
        assertEquals("tp_001", item.getTeachingPointId());
        assertEquals("CAPTURE_DRAFT_draft_001", item.getItemCode());
        assertEquals(taskStep.getStepName(), item.getItemName());
        assertEquals("KEY_ACTION", item.getItemType());
        assertEquals("res_001", item.getRelatedResourceId());
        assertEquals(taskStep.getId(), item.getRelatedTaskStepId());
        assertEquals(new BigDecimal("10.00"), item.getScore());
        assertEquals(Boolean.TRUE, item.getRequired());
        assertEquals("TRACE_EXISTS", item.getAssertionType());
        assertEquals("{\"traceType\":\"CLICK\"}", item.getAssertionConfigJson());
        assertEquals("NO_SCORE", item.getFailPolicy());
        assertEquals("teacher_001", item.getCreateBy());
        assertEquals("teacher_001", item.getUpdateBy());
    }

    @Test
    void publishDraftToTaskStepShouldMapVerifyStateToStateTraceType() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        actionDraft.setActionType("VERIFY_STATE");
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(null);
        when(taskStepService.createTaskStep(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(evaluationConfigService.createEvaluationItem(any())).thenAnswer(invocation -> invocation.getArgument(0));

        publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001");

        ArgumentCaptor<EvaluationItem> captor = ArgumentCaptor.forClass(EvaluationItem.class);
        verify(evaluationConfigService).createEvaluationItem(captor.capture());
        assertEquals("{\"traceType\":\"STATE\"}", captor.getValue().getAssertionConfigJson());
    }

    @Test
    void publishDraftToTaskStepShouldFallbackStepName() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        actionDraft.setConfirmedStepName(" ");
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(null);
        when(taskStepService.createTaskStep(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskStep taskStep = publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "teacher_001");

        assertEquals("点击提交按钮", taskStep.getStepName());
    }

    /**
     * 校验草稿未绑定正式资源时仍可发布，避免非资源型动作被错误拦截。
     */
    @Test
    void publishDraftToTaskStepShouldAllowMissingResource() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        actionDraft.setConnectorResourceId(null);
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(null);
        when(taskStepService.createTaskStep(any())).thenAnswer(invocation -> invocation.getArgument(0));

        publishService.publishDraftToTaskStep("tenant_001", "draft_001", "task_001", "tp_001", "teacher_001");

        ArgumentCaptor<TaskStep> captor = ArgumentCaptor.forClass(TaskStep.class);
        verify(taskStepService).createTaskStep(captor.capture());
        assertEquals(null, captor.getValue().getRelatedResourceIds());
    }

    /**
     * 校验已发布过的草稿直接返回已有步骤，避免重复生成 task_step。
     */
    @Test
    void publishDraftToTaskStepShouldReturnExistingTaskStep() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        TaskStep existed = buildExistingTaskStep();
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(existed);

        TaskStep taskStep = publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "teacher_001");

        assertSame(existed, taskStep);
        verify(taskStepService, times(0)).createTaskStep(any());
    }

    /**
     * 校验已有教学步骤但缺少评分项时会补齐评分项，避免旧发布数据进入“有步骤、无评分”的半成品状态。
     */
    @Test
    void publishDraftToTaskStepShouldBackfillEvaluationItemForExistingTaskStep() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        TaskStep existed = buildExistingTaskStep();
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(existed);
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001")).thenReturn(Collections.emptyList());
        when(evaluationConfigService.createEvaluationItem(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TaskStep taskStep = publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001");

        assertSame(existed, taskStep);
        verify(taskStepService, times(0)).createTaskStep(any());
        ArgumentCaptor<EvaluationItem> captor = ArgumentCaptor.forClass(EvaluationItem.class);
        verify(evaluationConfigService).createEvaluationItem(captor.capture());
        assertEquals("step_001", captor.getValue().getRelatedTaskStepId());
        assertEquals("CAPTURE_DRAFT_draft_001", captor.getValue().getItemCode());
    }

    /**
     * 校验已有教学步骤且评分项已存在时不重复创建评分项，保证补偿逻辑本身也是幂等的。
     */
    @Test
    void publishDraftToTaskStepShouldNotDuplicateExistingEvaluationItem() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        TaskStep existed = buildExistingTaskStep();
        EvaluationItem existedItem = new EvaluationItem();
        existedItem.setItemCode("CAPTURE_DRAFT_draft_001");
        existedItem.setRelatedTaskStepId("step_001");
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);
        when(taskStepMapper.selectOne(any())).thenReturn(existed);
        when(evaluationConfigService.listItemsByRule("tenant_001", "rule_001"))
                .thenReturn(Collections.singletonList(existedItem));

        TaskStep taskStep = publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001");

        assertSame(existed, taskStep);
        verify(taskStepService, times(0)).createTaskStep(any());
        verify(evaluationConfigService, times(0)).createEvaluationItem(any());
    }

    /**
     * 校验 PENDING 草稿不能发布，防止未经教师确认的系统建议直接面向学生。
     */
    @Test
    void publishDraftToTaskStepShouldRejectPendingDraft() {
        CaptureActionDraft actionDraft = buildConfirmedDraft();
        actionDraft.setConfirmStatus("PENDING");
        when(actionDraftMapper.selectOne(any())).thenReturn(actionDraft);

        assertThrows(BusinessException.class, () -> publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "teacher_001"));
        verify(taskStepService, times(0)).createTaskStep(any());
    }

    /**
     * 校验草稿不存在时拒绝发布。
     */
    @Test
    void publishDraftToTaskStepShouldRejectMissingDraft() {
        when(actionDraftMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "teacher_001"));
        verify(taskStepService, times(0)).createTaskStep(any());
    }

    /**
     * 校验缺少任务或教学点时拒绝发布，避免生成无归属步骤。
     */
    @Test
    void publishDraftToTaskStepShouldRejectMissingScope() {
        assertThrows(BusinessException.class, () -> publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", " ", "tp_001", "teacher_001"));
        assertThrows(BusinessException.class, () -> publishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", " ", "teacher_001"));
    }

    /**
     * 构造已确认动作草稿。
     *
     * @return 已确认动作草稿。
     */
    private CaptureActionDraft buildConfirmedDraft() {
        CaptureActionDraft actionDraft = new CaptureActionDraft();
        actionDraft.setId("draft_001");
        actionDraft.setTenantId("tenant_001");
        actionDraft.setActionType("CLICK");
        actionDraft.setActionName("点击提交按钮");
        actionDraft.setConfirmedOperationName("提交备案申请");
        actionDraft.setConfirmedStepName("提交申请表");
        actionDraft.setGuideContent("点击提交按钮。");
        actionDraft.setPracticeHint("提交前检查必填项。");
        actionDraft.setConnectorResourceId("res_001");
        actionDraft.setSequenceNo(3L);
        actionDraft.setConfirmStatus("CONFIRMED");
        return actionDraft;
    }

    /**
     * 构造已发布教学步骤。
     *
     * @return 已发布教学步骤。
     */
    private TaskStep buildExistingTaskStep() {
        TaskStep existed = new TaskStep();
        existed.setId("step_001");
        existed.setStepName("提交申请表");
        existed.setSourceActionDraftId("draft_001");
        return existed;
    }
}
