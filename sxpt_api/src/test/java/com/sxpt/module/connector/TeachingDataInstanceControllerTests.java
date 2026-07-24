package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.service.TeachingDataInstanceService;
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
 * 教学业务数据实例接口测试。
 *
 * 业务功能：
 * 1. 验证实例创建接口遵循统一响应结构。
 * 2. 验证实例查询接口可按外部业务 ID、使用人和任务返回实例列表。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.data-instance-controller.enabled=true")
class TeachingDataInstanceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachingDataInstanceService teachingDataInstanceService;

    /**
     * 校验创建实例成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnTeachingDataInstanceVo() throws Exception {
        TeachingDataInstance saved = buildSavedInstance();
        when(teachingDataInstanceService.createTeachingDataInstance(any(TeachingDataInstance.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/data-instances/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"templateId\":\"tpl_001\",\"connectorSystemId\":\"connector_001\",\"ownerUserId\":\"user_001\",\"classId\":\"class_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"sceneType\":\"PRACTICE\",\"externalBusinessId\":\"biz_001\",\"externalBusinessNo\":\"NO-001\",\"externalStatus\":\"DRAFT\",\"metadataJson\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("tdi_001")))
                .andExpect(jsonPath("$.result.templateId", is("tpl_001")))
                .andExpect(jsonPath("$.result.externalBusinessId", is("biz_001")))
                .andExpect(jsonPath("$.result.instanceStatus", is("CREATED")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TeachingDataInstance> captor = ArgumentCaptor.forClass(TeachingDataInstance.class);
        verify(teachingDataInstanceService).createTeachingDataInstance(captor.capture());
        TeachingDataInstance requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("tpl_001", requestEntity.getTemplateId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("PRACTICE", requestEntity.getSceneType());
        org.junit.jupiter.api.Assertions.assertEquals("biz_001", requestEntity.getExternalBusinessId());
        org.junit.jupiter.api.Assertions.assertEquals("{}", requestEntity.getMetadataJson());
    }

    /**
     * 校验缺少原平台业务数据 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingExternalBusinessId() throws Exception {
        mockMvc.perform(post("/api/v1/connector/data-instances/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"templateId\":\"tpl_001\",\"connectorSystemId\":\"connector_001\",\"sceneType\":\"PRACTICE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按外部业务 ID 查询实例成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getByExternalBusinessShouldReturnInstanceVo() throws Exception {
        TeachingDataInstance saved = buildSavedInstance();
        when(teachingDataInstanceService.getByExternalBusiness("tenant_001", "connector_001", "biz_001"))
                .thenReturn(saved);

        mockMvc.perform(get("/api/v1/connector/data-instances/external")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001")
                        .param("externalBusinessId", "biz_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("tdi_001")))
                .andExpect(jsonPath("$.result.externalBusinessId", is("biz_001")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        verify(teachingDataInstanceService).getByExternalBusiness("tenant_001", "connector_001", "biz_001");
    }

    /**
     * 校验按使用人和场景查询实例列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByOwnerAndSceneShouldReturnInstanceVos() throws Exception {
        TeachingDataInstance saved = buildSavedInstance();
        when(teachingDataInstanceService.listByOwnerAndScene("tenant_001", "user_001", "PRACTICE"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/data-instances/owner")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("ownerUserId", "user_001")
                        .param("sceneType", "PRACTICE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].ownerUserId", is("user_001")))
                .andExpect(jsonPath("$.result[0].sceneType", is("PRACTICE")));

        verify(teachingDataInstanceService).listByOwnerAndScene("tenant_001", "user_001", "PRACTICE");
    }

    /**
     * 校验按任务和场景查询实例列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByTaskAndSceneShouldReturnInstanceVos() throws Exception {
        TeachingDataInstance saved = buildSavedInstance();
        saved.setSceneType("EXAM");
        when(teachingDataInstanceService.listByTaskAndScene("tenant_001", "task_001", "EXAM"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/data-instances/task")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("taskId", "task_001")
                        .param("sceneType", "EXAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].taskId", is("task_001")))
                .andExpect(jsonPath("$.result[0].sceneType", is("EXAM")));

        verify(teachingDataInstanceService).listByTaskAndScene("tenant_001", "task_001", "EXAM");
    }

    /**
     * 构造 Service 返回的已保存实例。
     *
     * @return 教学业务数据实例实体。
     */
    /**
     * 校验锁定实例成功返回锁定状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void lockShouldReturnLockedInstanceVo() throws Exception {
        TeachingDataInstance saved = buildSavedInstance();
        saved.setInstanceStatus("LOCKED");
        saved.setLockTime(LocalDateTime.now());
        when(teachingDataInstanceService.lockTeachingDataInstance("tdi_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/data-instances/tdi_001/lock")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("tdi_001")))
                .andExpect(jsonPath("$.result.instanceStatus", is("LOCKED")))
                .andExpect(jsonPath("$.result.lockTime", notNullValue()));

        verify(teachingDataInstanceService).lockTeachingDataInstance("tdi_001");
    }

    /**
     * 校验废弃实例成功返回废弃状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void discardShouldReturnDiscardedInstanceVo() throws Exception {
        TeachingDataInstance saved = buildSavedInstance();
        saved.setInstanceStatus("DISCARDED");
        saved.setStatus("DISABLED");
        when(teachingDataInstanceService.discardTeachingDataInstance("tdi_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/data-instances/tdi_001/discard")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("tdi_001")))
                .andExpect(jsonPath("$.result.instanceStatus", is("DISCARDED")))
                .andExpect(jsonPath("$.result.status", is("DISABLED")));

        verify(teachingDataInstanceService).discardTeachingDataInstance("tdi_001");
    }

    /**
     * 校验重置实例成功返回新的原平台业务数据引用。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void resetShouldReturnResetInstanceVo() throws Exception {
        TeachingDataInstance saved = buildSavedInstance();
        saved.setInstanceStatus("RESET");
        saved.setResetCount(1L);
        saved.setExternalBusinessId("biz_002");
        saved.setExternalBusinessNo("NO-002");
        saved.setExternalStatus("DRAFT");
        saved.setMetadataJson("{\"source\":\"reset\"}");
        when(teachingDataInstanceService.resetTeachingDataInstance(
                "tdi_001", "biz_002", "NO-002", "DRAFT", "{\"source\":\"reset\"}"))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/data-instances/tdi_001/reset")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalBusinessId\":\"biz_002\",\"externalBusinessNo\":\"NO-002\",\"externalStatus\":\"DRAFT\",\"metadataJson\":\"{\\\"source\\\":\\\"reset\\\"}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.instanceStatus", is("RESET")))
                .andExpect(jsonPath("$.result.resetCount", is(1)))
                .andExpect(jsonPath("$.result.externalBusinessId", is("biz_002")));

        verify(teachingDataInstanceService).resetTeachingDataInstance(
                "tdi_001", "biz_002", "NO-002", "DRAFT", "{\"source\":\"reset\"}");
    }

    /**
     * 校验重置实例缺少新原平台业务数据 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void resetShouldRejectMissingExternalBusinessId() throws Exception {
        mockMvc.perform(post("/api/v1/connector/data-instances/tdi_001/reset")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalBusinessNo\":\"NO-002\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    private TeachingDataInstance buildSavedInstance() {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId("tdi_001");
        instance.setTenantId("tenant_001");
        instance.setTemplateId("tpl_001");
        instance.setConnectorSystemId("connector_001");
        instance.setOwnerUserId("user_001");
        instance.setClassId("class_001");
        instance.setTaskId("task_001");
        instance.setTeachingPointId("tp_001");
        instance.setSceneType("PRACTICE");
        instance.setExternalBusinessId("biz_001");
        instance.setExternalBusinessNo("NO-001");
        instance.setExternalStatus("DRAFT");
        instance.setInstanceStatus("CREATED");
        instance.setResetCount(0L);
        instance.setMetadataJson("{}");
        instance.setStatus("ACTIVE");
        instance.setCreateTime(LocalDateTime.now());
        instance.setUpdateTime(LocalDateTime.now());
        return instance;
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
