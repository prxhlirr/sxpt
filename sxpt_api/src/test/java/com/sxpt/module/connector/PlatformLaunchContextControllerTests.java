package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 原平台启动上下文接口测试。
 *
 * 业务功能：
 * 1. 验证创建启动上下文接口遵循统一响应结构。
 * 2. 验证接口只返回明文 launchToken，不返回数据库 token hash。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.launch-context-controller.enabled=true")
class PlatformLaunchContextControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private PlatformLaunchContextService platformLaunchContextService;

    /**
     * 校验创建启动上下文成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnLaunchTokenAndContext() throws Exception {
        PlatformLaunchContext saved = buildSavedLaunchContext();
        when(platformLaunchContextService.createLaunchContext(any(PlatformLaunchContext.class)))
                .thenReturn(new PlatformLaunchContextService.CreatedLaunchContext(saved, "ctx_token_001"));

        mockMvc.perform(post("/api/v1/connector/launch-contexts/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"userId\":\"user_001\",\"connectorSystemId\":\"connector_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"executionId\":\"exec_001\",\"sceneType\":\"RECORD\",\"sdkMode\":\"CAPTURE\",\"targetUrl\":\"/record/apply\",\"segmentNo\":1,\"actorType\":\"APPLICANT\",\"requiredExternalOrgId\":\"org_ext_001\",\"requiredExternalRoleId\":\"role_ext_001\",\"dataScopeJson\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("launch_001")))
                .andExpect(jsonPath("$.result.launchToken", is("ctx_token_001")))
                .andExpect(jsonPath("$.result.tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result.userId", is("user_001")))
                .andExpect(jsonPath("$.result.connectorSystemId", is("connector_001")))
                .andExpect(jsonPath("$.result.sceneType", is("RECORD")))
                .andExpect(jsonPath("$.result.sdkMode", is("CAPTURE")))
                .andExpect(jsonPath("$.result.launchStatus", is("CREATED")))
                .andExpect(jsonPath("$.result.launchTokenHash").doesNotExist())
                .andExpect(jsonPath("$.result.deleted").doesNotExist())
                .andExpect(jsonPath("$.result.errorMessage").doesNotExist());

        ArgumentCaptor<PlatformLaunchContext> captor = ArgumentCaptor.forClass(PlatformLaunchContext.class);
        verify(platformLaunchContextService).createLaunchContext(captor.capture());
        PlatformLaunchContext requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("user_001", requestEntity.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("task_001", requestEntity.getTaskId());
        org.junit.jupiter.api.Assertions.assertEquals("tp_001", requestEntity.getTeachingPointId());
        org.junit.jupiter.api.Assertions.assertEquals("exec_001", requestEntity.getExecutionId());
        org.junit.jupiter.api.Assertions.assertEquals("RECORD", requestEntity.getSceneType());
        org.junit.jupiter.api.Assertions.assertEquals("CAPTURE", requestEntity.getSdkMode());
        org.junit.jupiter.api.Assertions.assertEquals("/record/apply", requestEntity.getTargetUrl());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1), requestEntity.getSegmentNo());
        org.junit.jupiter.api.Assertions.assertEquals("APPLICANT", requestEntity.getActorType());
        org.junit.jupiter.api.Assertions.assertEquals("org_ext_001", requestEntity.getRequiredExternalOrgId());
        org.junit.jupiter.api.Assertions.assertEquals("role_ext_001", requestEntity.getRequiredExternalRoleId());
        org.junit.jupiter.api.Assertions.assertEquals("{}", requestEntity.getDataScopeJson());
    }

    /**
     * 校验缺少目标地址时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingTargetUrl() throws Exception {
        mockMvc.perform(post("/api/v1/connector/launch-contexts/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"userId\":\"user_001\",\"connectorSystemId\":\"connector_001\",\"sceneType\":\"RECORD\",\"sdkMode\":\"CAPTURE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造 Service 返回的已保存启动上下文。
     *
     * @return 原平台启动上下文实体。
     */
    /**
     * 校验启动令牌成功时返回原平台运行上下文。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void verifyShouldReturnVerifiedLaunchContext() throws Exception {
        PlatformLaunchContext saved = buildSavedLaunchContext();
        saved.setDataInstanceId("data_001");
        saved.setSegmentNo(1L);
        saved.setActorType("APPLICANT");
        saved.setRequiredExternalOrgId("org_ext_001");
        saved.setRequiredExternalOrgName("外部单位");
        saved.setRequiredExternalRoleId("role_ext_001");
        saved.setRequiredExternalRoleName("备案申请人");
        saved.setExternalBusinessId("biz_001");
        saved.setExternalBusinessNo("NO_001");
        saved.setDataScopeJson("{}");
        saved.setLaunchStatus("VERIFIED");
        saved.setVerifiedTime(LocalDateTime.now());
        when(platformLaunchContextService.verifyLaunchToken("tenant_001", "ctx_token_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/launch-contexts/verify")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"launchToken\":\"ctx_token_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("launch_001")))
                .andExpect(jsonPath("$.result.launchStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.result.userId", is("user_001")))
                .andExpect(jsonPath("$.result.requiredExternalOrgId", is("org_ext_001")))
                .andExpect(jsonPath("$.result.requiredExternalRoleId", is("role_ext_001")))
                .andExpect(jsonPath("$.result.externalBusinessId", is("biz_001")))
                .andExpect(jsonPath("$.result.dataScopeJson", is("{}")))
                .andExpect(jsonPath("$.result.launchToken").doesNotExist())
                .andExpect(jsonPath("$.result.launchTokenHash").doesNotExist())
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        verify(platformLaunchContextService).verifyLaunchToken("tenant_001", "ctx_token_001");
    }

    /**
     * 校验缺少启动令牌时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void verifyShouldRejectMissingLaunchToken() throws Exception {
        mockMvc.perform(post("/api/v1/connector/launch-contexts/verify")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验标记 USED 成功时返回已使用状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void markUsedShouldReturnUsedLaunchContext() throws Exception {
        PlatformLaunchContext saved = buildSavedLaunchContext();
        saved.setLaunchStatus("USED");
        saved.setUsedTime(LocalDateTime.now());
        when(platformLaunchContextService.markLaunchContextUsed("launch_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/launch-contexts/used")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"launch_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("launch_001")))
                .andExpect(jsonPath("$.result.launchStatus", is("USED")))
                .andExpect(jsonPath("$.result.launchToken").doesNotExist())
                .andExpect(jsonPath("$.result.launchTokenHash").doesNotExist());

        verify(platformLaunchContextService).markLaunchContextUsed("launch_001");
    }

    /**
     * 校验标记 FAILED 成功时返回失败状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void markFailedShouldReturnFailedLaunchContext() throws Exception {
        PlatformLaunchContext saved = buildSavedLaunchContext();
        saved.setLaunchStatus("FAILED");
        saved.setErrorMessage("原平台 session 建立失败");
        when(platformLaunchContextService.markLaunchContextFailed("launch_001", "原平台 session 建立失败")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/launch-contexts/failed")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"launch_001\",\"errorMessage\":\"原平台 session 建立失败\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("launch_001")))
                .andExpect(jsonPath("$.result.launchStatus", is("FAILED")))
                .andExpect(jsonPath("$.result.launchToken").doesNotExist())
                .andExpect(jsonPath("$.result.launchTokenHash").doesNotExist());

        verify(platformLaunchContextService).markLaunchContextFailed("launch_001", "原平台 session 建立失败");
    }

    /**
     * 校验标记 FAILED 缺少失败原因时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void markFailedShouldRejectMissingErrorMessage() throws Exception {
        mockMvc.perform(post("/api/v1/connector/launch-contexts/failed")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"launch_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    private PlatformLaunchContext buildSavedLaunchContext() {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId("launch_001");
        launchContext.setTenantId("tenant_001");
        launchContext.setUserId("user_001");
        launchContext.setConnectorSystemId("connector_001");
        launchContext.setTaskId("task_001");
        launchContext.setTeachingPointId("tp_001");
        launchContext.setExecutionId("exec_001");
        launchContext.setSceneType("RECORD");
        launchContext.setSdkMode("CAPTURE");
        launchContext.setTargetUrl("/record/apply");
        launchContext.setLaunchStatus("CREATED");
        launchContext.setExpireTime(LocalDateTime.now().plusMinutes(5));
        launchContext.setCreateTime(LocalDateTime.now());
        return launchContext;
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
