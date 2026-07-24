package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import com.sxpt.module.capture.service.impl.CaptureActionDraftServiceImpl;
import org.junit.jupiter.api.Test;

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
 * 页面动作草稿服务测试。
 *
 * 业务功能：
 * 1. 验证系统生成动作草稿时会补齐 PENDING 确认状态。
 * 2. 验证动作草稿查询按采集会话委托 Mapper 完成。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class CaptureActionDraftServiceImplTests {

    private final CaptureActionDraftMapper mapper = mock(CaptureActionDraftMapper.class);

    private final CaptureActionDraftService service = new CaptureActionDraftServiceImpl(mapper);

    /**
     * 校验创建动作草稿时插入 Mapper 并补齐默认值。
     */
    @Test
    void createActionDraftShouldInsertAndFillDefaults() {
        CaptureActionDraft actionDraft = buildValidDraft();

        CaptureActionDraft saved = service.createActionDraft(actionDraft);

        assertSame(actionDraft, saved);
        assertEquals("PENDING", saved.getConfirmStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少动作名称时拒绝创建，避免教师看到不可理解的空草稿。
     */
    @Test
    void createActionDraftShouldRejectMissingActionName() {
        CaptureActionDraft actionDraft = buildValidDraft();
        actionDraft.setActionName(" ");

        assertThrows(BusinessException.class, () -> service.createActionDraft(actionDraft));
        verify(mapper, times(0)).insert(actionDraft);
    }

    /**
     * 校验缺少顺序号时拒绝创建，避免草稿无法按采集顺序展示。
     */
    @Test
    void createActionDraftShouldRejectMissingSequenceNo() {
        CaptureActionDraft actionDraft = buildValidDraft();
        actionDraft.setSequenceNo(null);

        assertThrows(BusinessException.class, () -> service.createActionDraft(actionDraft));
        verify(mapper, times(0)).insert(actionDraft);
    }

    /**
     * 校验按采集会话查询动作草稿时返回 Mapper 结果。
     */
    @Test
    void listBySessionShouldReturnMapperResult() {
        CaptureActionDraft actionDraft = buildValidDraft();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(actionDraft));

        List<CaptureActionDraft> result = service.listBySession("tenant_001", "cap_001");

        assertEquals(1, result.size());
        assertSame(actionDraft, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 校验确认动作草稿时写入教师确认字段和 CONFIRMED 状态。
     */
    @Test
    void confirmActionDraftShouldMarkConfirmed() {
        CaptureActionDraft actionDraft = buildValidDraft();
        actionDraft.setConfirmStatus("PENDING");
        when(mapper.selectById("draft_001")).thenReturn(actionDraft);

        CaptureActionDraft result = service.confirmActionDraft(
                "draft_001", "提交备案申请", "提交申请表", "点击提交按钮。", "提交前检查必填项。", "teacher_001");

        assertSame(actionDraft, result);
        assertEquals("CONFIRMED", result.getConfirmStatus());
        assertEquals("提交备案申请", result.getConfirmedOperationName());
        assertEquals("提交申请表", result.getConfirmedStepName());
        assertEquals("teacher_001", result.getUpdateBy());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(actionDraft);
    }

    /**
     * 校验确认动作草稿时可以同步绑定正式资源，支撑后续发布教学步骤引用资源。
     */
    @Test
    void confirmActionDraftShouldBindConnectorResource() {
        CaptureActionDraft actionDraft = buildValidDraft();
        actionDraft.setConfirmStatus("PENDING");
        when(mapper.selectById("draft_001")).thenReturn(actionDraft);

        CaptureActionDraft result = service.confirmActionDraft(
                "draft_001", "提交备案申请", "提交申请表", "点击提交按钮。", "提交前检查必填项。",
                "res_001", "teacher_001");

        assertSame(actionDraft, result);
        assertEquals("CONFIRMED", result.getConfirmStatus());
        assertEquals("res_001", result.getConnectorResourceId());
        assertEquals("teacher_001", result.getUpdateBy());
        verify(mapper).updateById(actionDraft);
    }

    /**
     * 校验丢弃动作草稿时写入 DISCARDED 状态。
     */
    @Test
    void discardActionDraftShouldMarkDiscarded() {
        CaptureActionDraft actionDraft = buildValidDraft();
        actionDraft.setConfirmStatus("PENDING");
        when(mapper.selectById("draft_001")).thenReturn(actionDraft);

        CaptureActionDraft result = service.discardActionDraft("draft_001", "teacher_001");

        assertSame(actionDraft, result);
        assertEquals("DISCARDED", result.getConfirmStatus());
        assertEquals("teacher_001", result.getUpdateBy());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(actionDraft);
    }

    /**
     * 校验已确认草稿不能再次确认，避免教师确认结果被重复覆盖。
     */
    @Test
    void confirmActionDraftShouldRejectConfirmedDraft() {
        CaptureActionDraft actionDraft = buildValidDraft();
        actionDraft.setConfirmStatus("CONFIRMED");
        when(mapper.selectById("draft_001")).thenReturn(actionDraft);

        assertThrows(BusinessException.class, () -> service.confirmActionDraft(
                "draft_001", "提交备案申请", "提交申请表", null, null, "teacher_001"));
        verify(mapper, times(0)).updateById(actionDraft);
    }

    /**
     * 校验草稿不存在时不能确认。
     */
    @Test
    void confirmActionDraftShouldRejectMissingDraft() {
        when(mapper.selectById("draft_missing")).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.confirmActionDraft(
                "draft_missing", "提交备案申请", "提交申请表", null, null, "teacher_001"));
        verify(mapper, times(0)).updateById(any());
    }

    /**
     * 构造最小有效页面动作草稿。
     *
     * @return 页面动作草稿实体。
     */
    private CaptureActionDraft buildValidDraft() {
        CaptureActionDraft actionDraft = new CaptureActionDraft();
        actionDraft.setId("draft_001");
        actionDraft.setTenantId("tenant_001");
        actionDraft.setCaptureSessionId("cap_001");
        actionDraft.setEventId("evt_001");
        actionDraft.setActionName("点击提交按钮");
        actionDraft.setActionType("CLICK");
        actionDraft.setSequenceNo(1L);
        actionDraft.setSuggestedOperationName("提交备案申请");
        actionDraft.setGuideContent("点击页面底部的提交按钮。");
        actionDraft.setPracticeHint("确认表单填写完整后再提交。");
        return actionDraft;
    }
}
