package com.sxpt.module.teaching;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
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
 * 教学任务步骤接口测试。
 *
 * 业务功能：
 * 1. 验证教学步骤创建接口遵循统一响应结构。
 * 2. 验证按任务和教学点查询步骤接口可用，且不返回内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.teaching.task-step-controller.enabled=true")
class TaskStepControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TaskStepService taskStepService;

    /**
     * 校验创建教学步骤成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnTaskStepVo() throws Exception {
        TaskStep saved = buildSavedTaskStep();
        when(taskStepService.createTaskStep(any(TaskStep.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/teaching/task-steps/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"stepCode\":\"STEP_001\",\"stepName\":\"填写申请信息\",\"stepDescription\":\"完成备案申请表单\",\"sequenceNo\":1,\"segmentNo\":1,\"actorType\":\"APPLICANT\",\"requiredExternalOrgId\":\"org_001\",\"requiredExternalOrgName\":\"申报单位\",\"requiredExternalRoleId\":\"role_001\",\"requiredExternalRoleName\":\"经办人\",\"switchStrategy\":\"MANUAL_CONFIRM\",\"nextSegmentNo\":2,\"switchConfirmRequired\":true,\"switchDecisionSource\":\"TEACHER_PATH\",\"switchReason\":\"申请提交后进入受理角色\",\"rollbackPolicy\":\"UI_ONLY\",\"relatedResourceIds\":\"[\\\"res_001\\\"]\",\"guideContent\":\"请填写申请信息\",\"practiceHint\":\"注意必填项\",\"required\":true,\"allowSkip\":false,\"sourceActionDraftId\":\"draft_001\",\"createBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("step_001")))
                .andExpect(jsonPath("$.result.stepCode", is("STEP_001")))
                .andExpect(jsonPath("$.result.sequenceNo", is(1)))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TaskStep> captor = ArgumentCaptor.forClass(TaskStep.class);
        verify(taskStepService).createTaskStep(captor.capture());
        TaskStep requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("task_001", requestEntity.getTaskId());
        org.junit.jupiter.api.Assertions.assertEquals("tp_001", requestEntity.getTeachingPointId());
        org.junit.jupiter.api.Assertions.assertEquals("STEP_001", requestEntity.getStepCode());
    }

    /**
     * 校验缺少排序号时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingSequenceNo() throws Exception {
        mockMvc.perform(post("/api/v1/teaching/task-steps/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"stepCode\":\"STEP_001\",\"stepName\":\"填写申请信息\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按任务和教学点查询步骤成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByTaskAndTeachingPointShouldReturnTaskStepVos() throws Exception {
        TaskStep saved = buildSavedTaskStep();
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/teaching/task-steps")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("taskId", "task_001")
                        .param("teachingPointId", "tp_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("step_001")))
                .andExpect(jsonPath("$.result[0].stepCode", is("STEP_001")));

        verify(taskStepService).listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001");
    }

    /**
     * 构造 Service 返回的已保存教学步骤。
     *
     * @return 教学任务步骤实体。
     */
    private TaskStep buildSavedTaskStep() {
        TaskStep taskStep = new TaskStep();
        taskStep.setId("step_001");
        taskStep.setTenantId("tenant_001");
        taskStep.setTaskId("task_001");
        taskStep.setTeachingPointId("tp_001");
        taskStep.setStepCode("STEP_001");
        taskStep.setStepName("填写申请信息");
        taskStep.setStepDescription("完成备案申请表单");
        taskStep.setSequenceNo(1L);
        taskStep.setSegmentNo(1L);
        taskStep.setActorType("APPLICANT");
        taskStep.setRequiredExternalOrgId("org_001");
        taskStep.setRequiredExternalOrgName("申报单位");
        taskStep.setRequiredExternalRoleId("role_001");
        taskStep.setRequiredExternalRoleName("经办人");
        taskStep.setSwitchStrategy("MANUAL_CONFIRM");
        taskStep.setNextSegmentNo(2L);
        taskStep.setSwitchConfirmRequired(Boolean.TRUE);
        taskStep.setSwitchDecisionSource("TEACHER_PATH");
        taskStep.setSwitchReason("申请提交后进入受理角色");
        taskStep.setRollbackPolicy("UI_ONLY");
        taskStep.setRelatedResourceIds("[\"res_001\"]");
        taskStep.setGuideContent("请填写申请信息");
        taskStep.setPracticeHint("注意必填项");
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
