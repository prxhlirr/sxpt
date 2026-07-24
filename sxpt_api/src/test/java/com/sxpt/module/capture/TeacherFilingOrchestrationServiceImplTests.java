package com.sxpt.module.capture;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.service.CaptureActionDraftGenerateService;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.capture.service.TeacherFilingOrchestrationService;
import com.sxpt.module.capture.service.impl.TeacherFilingOrchestrationServiceImpl;
import com.sxpt.module.capture.vo.TeacherFilingOrchestrationResultVO;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TeachingAssetPublishCheckService;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 老师备案编排服务实现测试。
 *
 * 业务功能：
 * 1. 验证编排服务会按采集会话生成草稿、发布已确认草稿并执行发布前校验。
 * 2. 验证未确认草稿和校验失败会进入缺陷清单，而不是被静默忽略。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代底层服务，聚焦编排顺序和阶段结果。
 * 2. 不加载 Spring 上下文，避免单元测试依赖数据库或 Mapper。
 */
class TeacherFilingOrchestrationServiceImplTests {

    private final CaptureActionDraftGenerateService actionDraftGenerateService =
            mock(CaptureActionDraftGenerateService.class);

    private final CaptureActionDraftService actionDraftService = mock(CaptureActionDraftService.class);

    private final CaptureDraftPublishService draftPublishService = mock(CaptureDraftPublishService.class);

    private final TeachingAssetPublishCheckService publishCheckService = mock(TeachingAssetPublishCheckService.class);

    private final TeacherFilingOrchestrationService service = new TeacherFilingOrchestrationServiceImpl(
            actionDraftGenerateService, actionDraftService, draftPublishService, publishCheckService);

    /**
     * 校验完整备案资产可以发布已确认草稿并返回发布就绪。
     */
    @Test
    void prepareTeachingAssetsShouldPublishConfirmedDraftAndMarkReady() {
        CaptureActionDraft generated = buildDraft("draft_new", "PENDING");
        CaptureActionDraft confirmed = buildDraft("draft_001", "CONFIRMED");
        TaskStep taskStep = new TaskStep();
        taskStep.setId("step_001");
        when(actionDraftGenerateService.generateDraftsFromSession("tenant_001", "session_001", "teacher_001"))
                .thenReturn(Collections.singletonList(generated));
        when(actionDraftService.listBySession("tenant_001", "session_001"))
                .thenReturn(Collections.singletonList(confirmed));
        when(draftPublishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001"))
                .thenReturn(taskStep);

        TeacherFilingOrchestrationResultVO result = service.prepareTeachingAssets(
                "tenant_001", "session_001", "task_001", "tp_001", "rule_001", "teacher_001");

        assertEquals(Integer.valueOf(1), result.getGeneratedDraftCount());
        assertEquals(Integer.valueOf(1), result.getPublishedStepCount());
        assertEquals(Boolean.TRUE, result.getReadyToPublish());
        assertTrue(result.getDefectMessages().isEmpty());
        verify(publishCheckService).validateReadyToPublish("tenant_001", "task_001", "tp_001", "rule_001");
    }

    /**
     * 校验未确认草稿不会发布，并会返回缺陷消息。
     */
    @Test
    void prepareTeachingAssetsShouldReportUnconfirmedDraftDefect() {
        CaptureActionDraft pending = buildDraft("draft_001", "PENDING");
        when(actionDraftGenerateService.generateDraftsFromSession("tenant_001", "session_001", "teacher_001"))
                .thenReturn(Collections.emptyList());
        when(actionDraftService.listBySession("tenant_001", "session_001"))
                .thenReturn(Collections.singletonList(pending));

        TeacherFilingOrchestrationResultVO result = service.prepareTeachingAssets(
                "tenant_001", "session_001", "task_001", "tp_001", "rule_001", "teacher_001");

        assertEquals(Integer.valueOf(0), result.getPublishedStepCount());
        assertEquals(Boolean.FALSE, result.getReadyToPublish());
        assertFalse(result.getDefectMessages().isEmpty());
        verify(draftPublishService, times(0)).publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001");
    }

    /**
     * 校验发布前完整性校验失败时返回未就绪结果。
     */
    @Test
    void prepareTeachingAssetsShouldReportPublishCheckFailure() {
        CaptureActionDraft confirmed = buildDraft("draft_001", "CONFIRMED");
        when(actionDraftGenerateService.generateDraftsFromSession("tenant_001", "session_001", "teacher_001"))
                .thenReturn(Collections.emptyList());
        when(actionDraftService.listBySession("tenant_001", "session_001"))
                .thenReturn(Collections.singletonList(confirmed));
        when(draftPublishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001"))
                .thenReturn(new TaskStep());
        doThrow(new BusinessException(ApiResultCode.STATE_NOT_ALLOWED))
                .when(publishCheckService)
                .validateReadyToPublish("tenant_001", "task_001", "tp_001", "rule_001");

        TeacherFilingOrchestrationResultVO result = service.prepareTeachingAssets(
                "tenant_001", "session_001", "task_001", "tp_001", "rule_001", "teacher_001");

        assertEquals(Integer.valueOf(1), result.getPublishedStepCount());
        assertEquals(Boolean.FALSE, result.getReadyToPublish());
        assertTrue(result.getDefectMessages().contains("教学资产发布前完整性校验未通过。"));
    }

    /**
     * 校验缺少归属参数时拒绝编排。
     */
    @Test
    void prepareTeachingAssetsShouldRejectMissingScope() {
        assertThrows(BusinessException.class, () -> service.prepareTeachingAssets(
                "tenant_001", " ", "task_001", "tp_001", "rule_001", "teacher_001"));
    }

    /**
     * 构造动作草稿。
     *
     * @param id 草稿 ID。
     * @param confirmStatus 确认状态。
     * @return 动作草稿。
     */
    private CaptureActionDraft buildDraft(String id, String confirmStatus) {
        CaptureActionDraft draft = new CaptureActionDraft();
        draft.setId(id);
        draft.setConfirmStatus(confirmStatus);
        return draft;
    }
}
