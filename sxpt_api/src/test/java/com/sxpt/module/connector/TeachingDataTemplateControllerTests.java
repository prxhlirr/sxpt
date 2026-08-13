package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
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
 * 教学业务数据模板接口测试。
 *
 * 业务功能：
 * 1. 验证模板创建接口遵循统一响应结构。
 * 2. 验证模板查询接口可按原平台、教学点和场景返回模板列表。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "sxpt.connector.data-template-controller.enabled=true",
        "sxpt.connector.classic-case-controller.enabled=false",
        "sxpt.connector.external-classic-case-controller.enabled=false"
})
class TeachingDataTemplateControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachingDataTemplateService teachingDataTemplateService;

    /**
     * 校验创建模板成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnTeachingDataTemplateVo() throws Exception {
        TeachingDataTemplate saved = buildSavedTemplate();
        when(teachingDataTemplateService.createTeachingDataTemplate(any(TeachingDataTemplate.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/data-templates/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"teachingPointId\":\"tp_001\",\"templateCode\":\"record_apply_default\",\"templateName\":\"标准备案申请默认数据\",\"sceneType\":\"RECORD\",\"initState\":\"DRAFT\",\"supportMode\":\"RECORD,PRACTICE\",\"configJson\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("tpl_001")))
                .andExpect(jsonPath("$.result.templateCode", is("record_apply_default")))
                .andExpect(jsonPath("$.result.sceneType", is("RECORD")))
                .andExpect(jsonPath("$.result.configJson", is("{}")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TeachingDataTemplate> captor = ArgumentCaptor.forClass(TeachingDataTemplate.class);
        verify(teachingDataTemplateService).createTeachingDataTemplate(captor.capture());
        TeachingDataTemplate requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("tp_001", requestEntity.getTeachingPointId());
        org.junit.jupiter.api.Assertions.assertEquals("record_apply_default", requestEntity.getTemplateCode());
        org.junit.jupiter.api.Assertions.assertEquals("RECORD", requestEntity.getSceneType());
        org.junit.jupiter.api.Assertions.assertEquals("{}", requestEntity.getConfigJson());
    }

    /**
     * 校验缺少模板编码时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingTemplateCode() throws Exception {
        mockMvc.perform(post("/api/v1/connector/data-templates/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"templateName\":\"标准备案申请默认数据\",\"sceneType\":\"RECORD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按原平台查询模板列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByConnectorShouldReturnTemplateVos() throws Exception {
        TeachingDataTemplate saved = buildSavedTemplate();
        when(teachingDataTemplateService.listTemplatesByConnector("tenant_001", "connector_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/data-templates")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("tpl_001")))
                .andExpect(jsonPath("$.result[0].templateCode", is("record_apply_default")))
                .andExpect(jsonPath("$.result[0].deleted").doesNotExist());

        verify(teachingDataTemplateService).listTemplatesByConnector("tenant_001", "connector_001");
    }

    /**
     * 校验按教学点和场景查询可用模板成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listActiveShouldReturnTemplateVos() throws Exception {
        TeachingDataTemplate saved = buildSavedTemplate();
        when(teachingDataTemplateService.listActiveTemplatesByTeachingPointAndScene(
                "tenant_001", "connector_001", "tp_001", "RECORD"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/data-templates/active")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001")
                        .param("teachingPointId", "tp_001")
                        .param("sceneType", "RECORD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].teachingPointId", is("tp_001")))
                .andExpect(jsonPath("$.result[0].sceneType", is("RECORD")));

        verify(teachingDataTemplateService)
                .listActiveTemplatesByTeachingPointAndScene("tenant_001", "connector_001", "tp_001", "RECORD");
    }

    /**
     * 构造 Service 返回的已保存模板。
     *
     * @return 教学业务数据模板实体。
     */
    private TeachingDataTemplate buildSavedTemplate() {
        TeachingDataTemplate template = new TeachingDataTemplate();
        template.setId("tpl_001");
        template.setTenantId("tenant_001");
        template.setConnectorSystemId("connector_001");
        template.setTeachingPointId("tp_001");
        template.setTemplateCode("record_apply_default");
        template.setTemplateName("标准备案申请默认数据");
        template.setSceneType("RECORD");
        template.setInitState("DRAFT");
        template.setSupportMode("RECORD,PRACTICE");
        template.setConfigJson("{}");
        template.setStatus("ACTIVE");
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        return template;
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
