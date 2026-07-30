package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.connector.service.BusinessModuleService;
import com.sxpt.module.user.mapper.TeachRoleMapper;
import com.sxpt.module.user.mapper.TeachUserMapper;
import com.sxpt.module.user.mapper.TeachUserOrgMapper;
import com.sxpt.module.user.mapper.TeachUserRoleMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 原平台业务模块管理接口测试。
 *
 * 业务功能：
 * 1. 验证业务模块创建、更新、启停和查询接口遵循统一响应结构。
 * 2. 验证 Controller 只负责 HTTP 入参转换和 VO 输出，业务约束统一交给 Service 处理。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口，固定后台管理页面后续依赖的接口路径和返回字段。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库和业务编排实现。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.business-module-controller.enabled=true")
class BusinessModuleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private BusinessModuleService businessModuleService;

    @MockBean
    private BusinessModuleProcessChainService businessModuleProcessChainService;

    @MockBean
    private TeachUserMapper teachUserMapper;

    @MockBean
    private TeachUserRoleMapper teachUserRoleMapper;

    @MockBean
    private TeachRoleMapper teachRoleMapper;

    @MockBean
    private TeachUserOrgMapper teachUserOrgMapper;

    /**
     * 校验创建业务模块成功返回统一响应，并将创建请求转换为业务模块实体。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnBusinessModuleVo() throws Exception {
        BusinessModule saved = buildSavedBusinessModule();
        when(businessModuleService.createBusinessModule(any(BusinessModule.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/business-modules/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"moduleCode\":\"record_apply\",\"moduleName\":\"备案申请\",\"externalModuleId\":\"origin_record_apply\",\"entryUrl\":\"/record/apply\",\"moduleType\":\"BUSINESS\",\"supportScenes\":\"[\\\"RECORD\\\",\\\"PRACTICE\\\",\\\"EXAM\\\"]\",\"needPreData\":true,\"defaultInitialStatus\":\"DRAFT\",\"defaultTargetStatus\":\"SUBMITTED\",\"capabilityCodesJson\":\"[\\\"CREATE\\\",\\\"SUBMIT\\\"]\",\"defaultTemplateId\":\"tpl_001\",\"remark\":\"用于备案申请教学\",\"updateBy\":\"admin_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("module_001")))
                .andExpect(jsonPath("$.result.tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result.connectorSystemId", is("connector_001")))
                .andExpect(jsonPath("$.result.moduleCode", is("record_apply")))
                .andExpect(jsonPath("$.result.moduleName", is("备案申请")))
                .andExpect(jsonPath("$.result.supportScenes", is("[\"RECORD\",\"PRACTICE\",\"EXAM\"]")))
                .andExpect(jsonPath("$.result.status", is("ACTIVE")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist())
                .andExpect(jsonPath("$.result.lockVersion").doesNotExist());

        ArgumentCaptor<BusinessModule> captor = ArgumentCaptor.forClass(BusinessModule.class);
        verify(businessModuleService).createBusinessModule(captor.capture());
        BusinessModule requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("record_apply", requestEntity.getModuleCode());
        org.junit.jupiter.api.Assertions.assertEquals("备案申请", requestEntity.getModuleName());
        org.junit.jupiter.api.Assertions.assertEquals("/record/apply", requestEntity.getEntryUrl());
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, requestEntity.getNeedPreData());
        org.junit.jupiter.api.Assertions.assertEquals("admin_001", requestEntity.getUpdateBy());
    }

    /**
     * 校验更新业务模块成功返回统一响应，并且不会从请求体改写稳定身份字段。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void updateShouldReturnBusinessModuleVo() throws Exception {
        BusinessModule saved = buildSavedBusinessModule();
        saved.setModuleName("备案申请更新");
        saved.setEntryUrl("/record/apply/new");
        when(businessModuleService.updateBusinessModule(any(BusinessModule.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/business-modules/module_001/update")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"wrong_tenant\",\"connectorSystemId\":\"wrong_connector\",\"moduleCode\":\"wrong_code\",\"moduleName\":\"备案申请更新\",\"entryUrl\":\"/record/apply/new\",\"supportScenes\":\"[\\\"RECORD\\\",\\\"EXAM\\\"]\",\"needPreData\":false,\"updateBy\":\"admin_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("module_001")))
                .andExpect(jsonPath("$.result.moduleName", is("备案申请更新")))
                .andExpect(jsonPath("$.result.entryUrl", is("/record/apply/new")));

        ArgumentCaptor<BusinessModule> captor = ArgumentCaptor.forClass(BusinessModule.class);
        verify(businessModuleService).updateBusinessModule(captor.capture());
        BusinessModule requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("module_001", requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("备案申请更新", requestEntity.getModuleName());
        org.junit.jupiter.api.Assertions.assertEquals("/record/apply/new", requestEntity.getEntryUrl());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getModuleCode());
    }

    /**
     * 校验启用业务模块成功返回 ACTIVE 状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void enableShouldReturnActiveBusinessModuleVo() throws Exception {
        BusinessModule saved = buildSavedBusinessModule();
        saved.setStatus("ACTIVE");
        when(businessModuleService.enableBusinessModule("module_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/business-modules/module_001/enable")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.id", is("module_001")))
                .andExpect(jsonPath("$.result.status", is("ACTIVE")));

        verify(businessModuleService).enableBusinessModule("module_001");
    }

    /**
     * 校验禁用业务模块成功返回 DISABLED 状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void disableShouldReturnDisabledBusinessModuleVo() throws Exception {
        BusinessModule saved = buildSavedBusinessModule();
        saved.setStatus("DISABLED");
        when(businessModuleService.disableBusinessModule("module_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/business-modules/module_001/disable")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.id", is("module_001")))
                .andExpect(jsonPath("$.result.status", is("DISABLED")));

        verify(businessModuleService).disableBusinessModule("module_001");
    }

    /**
     * 校验查询业务模块详情成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void detailShouldReturnBusinessModuleVo() throws Exception {
        BusinessModule saved = buildSavedBusinessModule();
        when(businessModuleService.getBusinessModuleById("module_001")).thenReturn(saved);

        mockMvc.perform(get("/api/v1/connector/business-modules/module_001")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.id", is("module_001")))
                .andExpect(jsonPath("$.result.moduleCode", is("record_apply")))
                .andExpect(jsonPath("$.result.moduleName", is("备案申请")));

        verify(businessModuleService).getBusinessModuleById("module_001");
    }

    /**
     * 校验按租户和原平台查询业务模块列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listShouldReturnBusinessModuleVos() throws Exception {
        BusinessModule saved = buildSavedBusinessModule();
        when(businessModuleService.listBusinessModules("tenant_001", "connector_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/business-modules")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result[0].tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result[0].connectorSystemId", is("connector_001")))
                .andExpect(jsonPath("$.result[0].moduleCode", is("record_apply")));

        verify(businessModuleService).listBusinessModules("tenant_001", "connector_001");
    }

    /**
     * 校验查询启用业务模块列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listActiveShouldReturnBusinessModuleVos() throws Exception {
        BusinessModule saved = buildSavedBusinessModule();
        when(businessModuleService.listActiveBusinessModules("tenant_001", "connector_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/business-modules/active")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result[0].id", is("module_001")))
                .andExpect(jsonPath("$.result[0].status", is("ACTIVE")));

        verify(businessModuleService).listActiveBusinessModules("tenant_001", "connector_001");
    }

    /**
     * 构造 Service 返回的已保存业务模块。
     *
     * @return 原平台业务模块实体。
     */
    private BusinessModule buildSavedBusinessModule() {
        BusinessModule businessModule = new BusinessModule();
        businessModule.setId("module_001");
        businessModule.setTenantId("tenant_001");
        businessModule.setConnectorSystemId("connector_001");
        businessModule.setModuleCode("record_apply");
        businessModule.setModuleName("备案申请");
        businessModule.setExternalModuleId("origin_record_apply");
        businessModule.setEntryUrl("/record/apply");
        businessModule.setModuleType("BUSINESS");
        businessModule.setSupportScenes("[\"RECORD\",\"PRACTICE\",\"EXAM\"]");
        businessModule.setNeedPreData(Boolean.TRUE);
        businessModule.setDefaultInitialStatus("DRAFT");
        businessModule.setDefaultTargetStatus("SUBMITTED");
        businessModule.setCapabilityCodesJson("[\"CREATE\",\"SUBMIT\"]");
        businessModule.setDefaultTemplateId("tpl_001");
        businessModule.setRemark("用于备案申请教学");
        businessModule.setStatus("ACTIVE");
        businessModule.setCreateTime(LocalDateTime.now());
        businessModule.setUpdateTime(LocalDateTime.now());
        return businessModule;
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
