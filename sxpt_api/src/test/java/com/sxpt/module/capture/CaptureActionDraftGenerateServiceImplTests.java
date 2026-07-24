package com.sxpt.module.capture;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import com.sxpt.module.capture.mapper.CaptureEventMapper;
import com.sxpt.module.capture.service.CaptureActionDraftGenerateService;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import com.sxpt.module.capture.service.impl.CaptureActionDraftGenerateServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 采集事件自动生成动作草稿服务测试。
 *
 * 业务功能：
 * 1. 验证 MVP 阶段可以从单个采集事件生成 PENDING 动作草稿。
 * 2. 验证重复事件、无效事件和暂不支持事件不会生成重复或错误草稿。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper 和草稿写入服务，聚焦草稿生成规则本身。
 * 2. 保持纯单元测试，不依赖 Spring、数据库和 Redis。
 */
class CaptureActionDraftGenerateServiceImplTests {

    private final CaptureEventMapper eventMapper = mock(CaptureEventMapper.class);

    private final CaptureActionDraftMapper draftMapper = mock(CaptureActionDraftMapper.class);

    private final CaptureActionDraftService draftService = mock(CaptureActionDraftService.class);

    private final CaptureActionDraftGenerateService generateService =
            new CaptureActionDraftGenerateServiceImpl(eventMapper, draftMapper, draftService);

    /**
     * 验证 CLICK 和 INPUT 事件可以生成老师可确认的动作草稿。
     */
    @Test
    void generateDraftsFromSessionShouldCreateDraftsForSupportedEvents() {
        CaptureEvent clickEvent = buildEvent("evt_001", "CLICK", "保存", 1L);
        CaptureEvent inputEvent = buildEvent("evt_002", "INPUT", "客户名称", 2L);
        when(eventMapper.selectList(any())).thenReturn(Arrays.asList(clickEvent, inputEvent));
        when(draftMapper.selectOne(any())).thenReturn(null);
        when(draftService.createActionDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CaptureActionDraft> drafts = generateService.generateDraftsFromSession(
                "tenant_001", "cap_001", "teacher_001");

        assertEquals(2, drafts.size());
        assertEquals("点击保存", drafts.get(0).getActionName());
        assertEquals("CLICK", drafts.get(0).getActionType());
        assertEquals("填写客户名称", drafts.get(1).getActionName());
        assertEquals("INPUT", drafts.get(1).getActionType());
        verify(draftService, times(2)).createActionDraft(any());
    }

    /**
     * 验证同页面同控件的连续 INPUT 会归并为一个填写草稿，避免逐字输入污染老师确认列表。
     */
    @Test
    void generateDraftsFromSessionShouldMergeContinuousInputOnSameControl() {
        CaptureEvent firstInput = buildEvent("evt_001", "INPUT", "客户名称", 1L);
        firstInput.setTargetStableKey("customer_name");
        CaptureEvent secondInput = buildEvent("evt_002", "INPUT", "客户名称", 2L);
        secondInput.setTargetStableKey("customer_name");
        CaptureEvent clickEvent = buildEvent("evt_003", "CLICK", "保存", 3L);
        when(eventMapper.selectList(any())).thenReturn(Arrays.asList(firstInput, secondInput, clickEvent));
        when(draftMapper.selectOne(any())).thenReturn(null);
        when(draftService.createActionDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CaptureActionDraft> drafts = generateService.generateDraftsFromSession(
                "tenant_001", "cap_001", "teacher_001");

        assertEquals(2, drafts.size());
        assertEquals("evt_001", drafts.get(0).getEventId());
        assertEquals("填写客户名称", drafts.get(0).getActionName());
        assertEquals(Long.valueOf(1), drafts.get(0).getSequenceNo());
        assertEquals("点击保存", drafts.get(1).getActionName());
        verify(draftService, times(2)).createActionDraft(any());
    }

    /**
     * 验证 CLICK 后紧跟页面 STATE 变化时归并为导航草稿，避免菜单点击和页面变化拆成两个低层动作。
     */
    @Test
    void generateDraftsFromSessionShouldMergeClickAndStateAsNavigateDraft() {
        CaptureEvent menuClick = buildEvent("evt_001", "CLICK", "客户管理", 1L);
        menuClick.setPageUrl("/home");
        CaptureEvent pageState = buildEvent("evt_002", "STATE", "客户列表", 2L);
        pageState.setPageUrl("/customer/list");
        when(eventMapper.selectList(any())).thenReturn(Arrays.asList(menuClick, pageState));
        when(draftMapper.selectOne(any())).thenReturn(null);
        when(draftService.createActionDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CaptureActionDraft> drafts = generateService.generateDraftsFromSession(
                "tenant_001", "cap_001", "teacher_001");

        assertEquals(1, drafts.size());
        assertEquals("evt_001", drafts.get(0).getEventId());
        assertEquals("NAVIGATE", drafts.get(0).getActionType());
        assertEquals("进入客户列表", drafts.get(0).getActionName());
        assertEquals(Long.valueOf(1), drafts.get(0).getSequenceNo());
        verify(draftService).createActionDraft(any());
    }

    /**
     * 验证 SELECT 后紧跟查询类 CLICK 时归并为一个业务查询草稿，减少老师确认重复动作的成本。
     */
    @Test
    void generateDraftsFromSessionShouldMergeSelectAndQueryClickAsBusinessQueryDraft() {
        CaptureEvent selectEvent = buildEvent("evt_001", "SELECT", "客户类型", 1L);
        selectEvent.setPageUrl("/customer/list");
        CaptureEvent queryClick = buildEvent("evt_002", "CLICK", "查询", 2L);
        queryClick.setPageUrl("/customer/list");
        when(eventMapper.selectList(any())).thenReturn(Arrays.asList(selectEvent, queryClick));
        when(draftMapper.selectOne(any())).thenReturn(null);
        when(draftService.createActionDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CaptureActionDraft> drafts = generateService.generateDraftsFromSession(
                "tenant_001", "cap_001", "teacher_001");

        assertEquals(1, drafts.size());
        assertEquals("evt_002", drafts.get(0).getEventId());
        assertEquals("CLICK", drafts.get(0).getActionType());
        assertEquals("按客户类型查询", drafts.get(0).getActionName());
        assertEquals(Long.valueOf(1), drafts.get(0).getSequenceNo());
        verify(draftService).createActionDraft(any());
    }

    /**
     * 验证已存在草稿的事件不会重复生成，避免老师重复点击生成按钮产生重复草稿。
     */
    @Test
    void generateDraftsFromSessionShouldSkipExistingDraft() {
        CaptureEvent clickEvent = buildEvent("evt_001", "CLICK", "保存", 1L);
        when(eventMapper.selectList(any())).thenReturn(Collections.singletonList(clickEvent));
        when(draftMapper.selectOne(any())).thenReturn(new CaptureActionDraft());

        List<CaptureActionDraft> drafts = generateService.generateDraftsFromSession(
                "tenant_001", "cap_001", "teacher_001");

        assertTrue(drafts.isEmpty());
        verify(draftService, times(0)).createActionDraft(any());
    }

    /**
     * 验证暂不支持的事件类型会被过滤，避免生成低质量草稿。
     */
    @Test
    void generateDraftsFromSessionShouldSkipUnsupportedEvents() {
        CaptureEvent scrollEvent = buildEvent("evt_001", "SCROLL", "页面滚动", 1L);
        when(eventMapper.selectList(any())).thenReturn(Collections.singletonList(scrollEvent));

        List<CaptureActionDraft> drafts = generateService.generateDraftsFromSession(
                "tenant_001", "cap_001", "teacher_001");

        assertTrue(drafts.isEmpty());
        verify(draftService, times(0)).createActionDraft(any());
        verify(draftMapper, times(0)).selectOne(any());
    }

    /**
     * 验证目标文本缺失时使用页面和顺序号生成兜底名称，避免空草稿进入确认页面。
     */
    @Test
    void generateDraftsFromSessionShouldUseFallbackName() {
        CaptureEvent clickEvent = buildEvent("evt_001", "CLICK", " ", 3L);
        clickEvent.setPageUrl("/customer/list");
        when(eventMapper.selectList(any())).thenReturn(Collections.singletonList(clickEvent));
        when(draftMapper.selectOne(any())).thenReturn(null);
        when(draftService.createActionDraft(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CaptureActionDraft> drafts = generateService.generateDraftsFromSession(
                "tenant_001", "cap_001", "teacher_001");

        assertEquals(1, drafts.size());
        assertEquals("点击/customer/list动作3", drafts.get(0).getActionName());
    }

    /**
     * 验证缺少租户、会话或操作人时拒绝生成，避免无归属草稿污染备案数据。
     */
    @Test
    void generateDraftsFromSessionShouldRejectMissingScope() {
        assertThrows(BusinessException.class,
                () -> generateService.generateDraftsFromSession(" ", "cap_001", "teacher_001"));
        assertThrows(BusinessException.class,
                () -> generateService.generateDraftsFromSession("tenant_001", " ", "teacher_001"));
        assertThrows(BusinessException.class,
                () -> generateService.generateDraftsFromSession("tenant_001", "cap_001", " "));
    }

    /**
     * 构造最小有效采集事件。
     *
     * @param id 事件 ID。
     * @param eventType 事件类型。
     * @param targetText 目标文本。
     * @param sequenceNo 顺序号。
     * @return 采集事件实体。
     */
    private CaptureEvent buildEvent(String id, String eventType, String targetText, Long sequenceNo) {
        CaptureEvent event = new CaptureEvent();
        event.setId(id);
        event.setTenantId("tenant_001");
        event.setCaptureSessionId("cap_001");
        event.setEventType(eventType);
        event.setTargetText(targetText);
        event.setPageUrl("/page");
        event.setSequenceNo(sequenceNo);
        return event;
    }
}
