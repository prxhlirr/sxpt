package com.sxpt.module.capture;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.service.CaptureActionDraftGenerateService;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.teaching.entity.TaskStep;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 页面动作草稿接口测试。
 *
 * 业务功能：
 * 1. 验证动作草稿创建接口遵循统一响应结构。
 * 2. 验证按采集会话查询草稿接口可用，且不返回内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.capture.action-draft-controller.enabled=true")
class CaptureActionDraftControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CaptureActionDraftService actionDraftService;

    @MockBean
    private CaptureActionDraftGenerateService actionDraftGenerateService;

    @MockBean
    private CaptureDraftPublishService draftPublishService;

    /**
     * 校验动作草稿创建成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnCaptureActionDraftVo() throws Exception {
        CaptureActionDraft saved = buildSavedDraft();
        when(actionDraftService.createActionDraft(any(CaptureActionDraft.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/capture/action-drafts/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"captureSessionId\":\"cap_001\",\"eventId\":\"evt_001\",\"actionName\":\"点击提交按钮\",\"actionType\":\"CLICK\",\"sequenceNo\":1,\"suggestedOperationName\":\"提交备案申请\",\"guideContent\":\"点击页面底部的提交按钮。\",\"practiceHint\":\"确认表单填写完整后再提交。\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("draft_001")))
                .andExpect(jsonPath("$.result.actionName", is("点击提交按钮")))
                .andExpect(jsonPath("$.result.confirmStatus", is("PENDING")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<CaptureActionDraft> captor = ArgumentCaptor.forClass(CaptureActionDraft.class);
        verify(actionDraftService).createActionDraft(captor.capture());
        CaptureActionDraft requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("cap_001", requestEntity.getCaptureSessionId());
        org.junit.jupiter.api.Assertions.assertEquals("evt_001", requestEntity.getEventId());
        org.junit.jupiter.api.Assertions.assertEquals("CLICK", requestEntity.getActionType());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1), requestEntity.getSequenceNo());
    }

    /**
     * 校验缺少动作名称时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingActionName() throws Exception {
        mockMvc.perform(post("/api/v1/capture/action-drafts/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"captureSessionId\":\"cap_001\",\"actionType\":\"CLICK\",\"sequenceNo\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按采集会话查询动作草稿列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listBySessionShouldReturnCaptureActionDraftVos() throws Exception {
        CaptureActionDraft saved = buildSavedDraft();
        when(actionDraftService.listBySession("tenant_001", "cap_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/capture/action-drafts")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("captureSessionId", "cap_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("draft_001")))
                .andExpect(jsonPath("$.result[0].captureSessionId", is("cap_001")))
                .andExpect(jsonPath("$.result[0].sequenceNo", is(1)));

        verify(actionDraftService).listBySession("tenant_001", "cap_001");
    }

    /**
     * 校验按采集会话自动生成动作草稿时返回生成数量和草稿列表。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void generateFromSessionShouldReturnGeneratedDrafts() throws Exception {
        CaptureActionDraft saved = buildSavedDraft();
        when(actionDraftGenerateService.generateDraftsFromSession("tenant_001", "cap_001", "teacher_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(post("/api/v1/capture/action-drafts/generate")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("captureSessionId", "cap_001")
                        .param("operatorId", "teacher_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.generatedCount", is(1)))
                .andExpect(jsonPath("$.result.drafts[0].id", is("draft_001")))
                .andExpect(jsonPath("$.result.drafts[0].actionName", is("点击提交按钮")))
                .andExpect(jsonPath("$.result.drafts[0].deleted").doesNotExist());

        verify(actionDraftGenerateService).generateDraftsFromSession("tenant_001", "cap_001", "teacher_001");
    }

    /**
     * 校验确认动作草稿成功返回 CONFIRMED 状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void confirmShouldReturnConfirmedCaptureActionDraftVo() throws Exception {
        CaptureActionDraft saved = buildSavedDraft();
        saved.setConfirmStatus("CONFIRMED");
        saved.setConfirmedOperationName("提交备案申请");
        saved.setConfirmedStepName("提交申请表");
        saved.setConnectorResourceId("res_001");
        when(actionDraftService.confirmActionDraft(
                "draft_001", "提交备案申请", "提交申请表", "点击提交按钮。", "提交前检查必填项。",
                "res_001", "teacher_001"))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/capture/action-drafts/draft_001/confirm")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedOperationName\":\"提交备案申请\",\"confirmedStepName\":\"提交申请表\",\"guideContent\":\"点击提交按钮。\",\"practiceHint\":\"提交前检查必填项。\",\"connectorResourceId\":\"res_001\",\"updateBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("draft_001")))
                .andExpect(jsonPath("$.result.confirmStatus", is("CONFIRMED")))
                .andExpect(jsonPath("$.result.confirmedOperationName", is("提交备案申请")))
                .andExpect(jsonPath("$.result.confirmedStepName", is("提交申请表")))
                .andExpect(jsonPath("$.result.connectorResourceId", is("res_001")));

        verify(actionDraftService).confirmActionDraft(
                "draft_001", "提交备案申请", "提交申请表", "点击提交按钮。", "提交前检查必填项。",
                "res_001", "teacher_001");
    }

    /**
     * 校验确认动作草稿缺少业务操作名称时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void confirmShouldRejectMissingOperationName() throws Exception {
        mockMvc.perform(post("/api/v1/capture/action-drafts/draft_001/confirm")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedStepName\":\"提交申请表\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验发布动作草稿成功返回教学任务步骤。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void publishShouldReturnTaskStepVo() throws Exception {
        TaskStep taskStep = buildPublishedTaskStep();
        when(draftPublishService.publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001"))
                .thenReturn(taskStep);

        mockMvc.perform(post("/api/v1/capture/action-drafts/draft_001/publish")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"evaluationRuleId\":\"rule_001\",\"operatorId\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("step_001")))
                .andExpect(jsonPath("$.result.taskId", is("task_001")))
                .andExpect(jsonPath("$.result.teachingPointId", is("tp_001")))
                .andExpect(jsonPath("$.result.stepCode", is("CAPTURE_DRAFT_draft_001")))
                .andExpect(jsonPath("$.result.stepName", is("提交申请表")))
                .andExpect(jsonPath("$.result.relatedResourceIds", is("[\"res_001\"]")))
                .andExpect(jsonPath("$.result.sourceActionDraftId", is("draft_001")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        verify(draftPublishService).publishDraftToTaskStep(
                "tenant_001", "draft_001", "task_001", "tp_001", "rule_001", "teacher_001");
    }

    /**
     * 校验发布动作草稿缺少评分规则时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void publishShouldRejectMissingEvaluationRuleId() throws Exception {
        mockMvc.perform(post("/api/v1/capture/action-drafts/draft_001/publish")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"operatorId\":\"teacher_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验丢弃动作草稿成功返回 DISCARDED 状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void discardShouldReturnDiscardedCaptureActionDraftVo() throws Exception {
        CaptureActionDraft saved = buildSavedDraft();
        saved.setConfirmStatus("DISCARDED");
        when(actionDraftService.discardActionDraft("draft_001", "teacher_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/capture/action-drafts/draft_001/discard")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"updateBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("draft_001")))
                .andExpect(jsonPath("$.result.confirmStatus", is("DISCARDED")));

        verify(actionDraftService).discardActionDraft("draft_001", "teacher_001");
    }

    /**
     * 构造 Service 返回的已保存动作草稿。
     *
     * @return 页面动作草稿实体。
     */
    private CaptureActionDraft buildSavedDraft() {
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
        actionDraft.setConfirmStatus("PENDING");
        actionDraft.setStatus("ACTIVE");
        actionDraft.setCreateTime(LocalDateTime.now());
        actionDraft.setUpdateTime(LocalDateTime.now());
        return actionDraft;
    }

    /**
     * 构造 Service 返回的已发布教学步骤。
     *
     * @return 教学任务步骤实体。
     */
    private TaskStep buildPublishedTaskStep() {
        TaskStep taskStep = new TaskStep();
        taskStep.setId("step_001");
        taskStep.setTenantId("tenant_001");
        taskStep.setTaskId("task_001");
        taskStep.setTeachingPointId("tp_001");
        taskStep.setStepCode("CAPTURE_DRAFT_draft_001");
        taskStep.setStepName("提交申请表");
        taskStep.setStepDescription("提交备案申请");
        taskStep.setSequenceNo(1L);
        taskStep.setRelatedResourceIds("[\"res_001\"]");
        taskStep.setGuideContent("点击提交按钮。");
        taskStep.setPracticeHint("提交前检查必填项。");
        taskStep.setRequired(Boolean.TRUE);
        taskStep.setAllowSkip(Boolean.FALSE);
        taskStep.setSourceActionDraftId("draft_001");
        taskStep.setStatus("ACTIVE");
        taskStep.setCreateTime(LocalDateTime.now());
        taskStep.setUpdateTime(LocalDateTime.now());
        return taskStep;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("admin_001", "admin");
    }
}
