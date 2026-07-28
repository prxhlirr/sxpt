package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
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
 * 原平台业务模块数据准备策略接口测试。
 *
 * 业务功能：
 * 1. 验证策略创建、更新、启停和查询接口遵循统一响应结构。
 * 2. 验证 Controller 只负责 HTTP 入参转换和 VO 输出，策略完整性统一交给 Service 处理。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口，固定后台管理页面后续依赖的接口路径和返回字段。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库和策略持久化实现。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.module-data-strategy-controller.enabled=true")
class ModuleDataStrategyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ModuleDataStrategyService moduleDataStrategyService;

    /**
     * 校验创建策略成功返回统一响应，并将创建请求转换为策略实体。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnModuleDataStrategyVo() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        when(moduleDataStrategyService.createModuleDataStrategy(any(ModuleDataStrategy.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/module-data-strategies/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"businessModuleId\":\"module_001\",\"moduleCode\":\"record_apply\",\"moduleName\":\"备案申请\",\"sceneType\":\"RECORD\",\"needPreData\":true,\"dataSourceStrategy\":\"MOCK_GENERATE\",\"initExternalStatus\":\"DRAFT\",\"targetExternalStatus\":\"SUBMITTED\",\"defaultOrgRolePolicyJson\":\"{}\",\"sharePolicy\":\"ATTEMPT_EXCLUSIVE\",\"regeneratePolicy\":\"ON_ATTEMPT\",\"lockPolicy\":\"NONE\",\"expirePolicyJson\":\"{}\",\"resultCheckPolicyJson\":\"{}\",\"strategyCode\":\"record_apply_record\",\"templateId\":\"tpl_001\",\"prepareTiming\":\"ON_PUBLISH\",\"poolSizePolicyJson\":\"{}\",\"validationPolicyJson\":\"{}\",\"archivePolicyJson\":\"{}\",\"createBy\":\"admin_001\",\"updateBy\":\"admin_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("strategy_001")))
                .andExpect(jsonPath("$.result.tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result.connectorSystemId", is("connector_001")))
                .andExpect(jsonPath("$.result.businessModuleId", is("module_001")))
                .andExpect(jsonPath("$.result.moduleCode", is("record_apply")))
                .andExpect(jsonPath("$.result.sceneType", is("RECORD")))
                .andExpect(jsonPath("$.result.strategyVersion", is(1)))
                .andExpect(jsonPath("$.result.status", is("DISABLED")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist())
                .andExpect(jsonPath("$.result.lockVersion").doesNotExist());

        ArgumentCaptor<ModuleDataStrategy> captor = ArgumentCaptor.forClass(ModuleDataStrategy.class);
        verify(moduleDataStrategyService).createModuleDataStrategy(captor.capture());
        ModuleDataStrategy requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("module_001", requestEntity.getBusinessModuleId());
        org.junit.jupiter.api.Assertions.assertEquals("record_apply", requestEntity.getModuleCode());
        org.junit.jupiter.api.Assertions.assertEquals("RECORD", requestEntity.getSceneType());
        org.junit.jupiter.api.Assertions.assertEquals("admin_001", requestEntity.getCreateBy());
        org.junit.jupiter.api.Assertions.assertEquals("admin_001", requestEntity.getUpdateBy());
    }

    /**
     * 校验更新策略成功返回统一响应，并且不会从请求体改写稳定身份字段。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void updateShouldReturnModuleDataStrategyVo() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        saved.setModuleName("备案申请更新");
        saved.setTemplateId("tpl_002");
        saved.setPrepareTiming("ON_DEMAND");
        saved.setStrategyVersion(2L);
        when(moduleDataStrategyService.updateModuleDataStrategy(any(ModuleDataStrategy.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/module-data-strategies/strategy_001/update")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"wrong_tenant\",\"connectorSystemId\":\"wrong_connector\",\"businessModuleId\":\"wrong_module\",\"moduleCode\":\"wrong_code\",\"sceneType\":\"EXAM\",\"moduleName\":\"备案申请更新\",\"dataSourceStrategy\":\"MOCK_GENERATE\",\"sharePolicy\":\"QUESTION_EXCLUSIVE\",\"regeneratePolicy\":\"ON_ATTEMPT\",\"lockPolicy\":\"ON_EXAM_START\",\"templateId\":\"tpl_002\",\"prepareTiming\":\"ON_DEMAND\",\"updateBy\":\"admin_002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("strategy_001")))
                .andExpect(jsonPath("$.result.moduleName", is("备案申请更新")))
                .andExpect(jsonPath("$.result.templateId", is("tpl_002")))
                .andExpect(jsonPath("$.result.prepareTiming", is("ON_DEMAND")))
                .andExpect(jsonPath("$.result.strategyVersion", is(2)));

        ArgumentCaptor<ModuleDataStrategy> captor = ArgumentCaptor.forClass(ModuleDataStrategy.class);
        verify(moduleDataStrategyService).updateModuleDataStrategy(captor.capture());
        ModuleDataStrategy requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("strategy_001", requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("备案申请更新", requestEntity.getModuleName());
        org.junit.jupiter.api.Assertions.assertEquals("tpl_002", requestEntity.getTemplateId());
        org.junit.jupiter.api.Assertions.assertEquals("ON_DEMAND", requestEntity.getPrepareTiming());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getBusinessModuleId());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getModuleCode());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getSceneType());
    }

    /**
     * 校验启用策略成功返回 ACTIVE 状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void enableShouldReturnActiveStrategyVo() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        saved.setStatus("ACTIVE");
        when(moduleDataStrategyService.enableModuleDataStrategy("strategy_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/module-data-strategies/strategy_001/enable")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.id", is("strategy_001")))
                .andExpect(jsonPath("$.result.status", is("ACTIVE")));

        verify(moduleDataStrategyService).enableModuleDataStrategy("strategy_001");
    }

    /**
     * 校验禁用策略成功返回 DISABLED 状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void disableShouldReturnDisabledStrategyVo() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        saved.setStatus("DISABLED");
        when(moduleDataStrategyService.disableModuleDataStrategy("strategy_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/module-data-strategies/strategy_001/disable")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.id", is("strategy_001")))
                .andExpect(jsonPath("$.result.status", is("DISABLED")));

        verify(moduleDataStrategyService).disableModuleDataStrategy("strategy_001");
    }

    /**
     * 校验查询策略详情成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void detailShouldReturnStrategyVo() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        when(moduleDataStrategyService.getModuleDataStrategyById("strategy_001")).thenReturn(saved);

        mockMvc.perform(get("/api/v1/connector/module-data-strategies/strategy_001")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.id", is("strategy_001")))
                .andExpect(jsonPath("$.result.moduleCode", is("record_apply")))
                .andExpect(jsonPath("$.result.sceneType", is("RECORD")));

        verify(moduleDataStrategyService).getModuleDataStrategyById("strategy_001");
    }

    /**
     * 校验按业务模块查询策略列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listShouldReturnStrategyVos() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        when(moduleDataStrategyService.listStrategiesByBusinessModule("tenant_001", "connector_001", "module_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/module-data-strategies")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001")
                        .param("businessModuleId", "module_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result[0].tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result[0].businessModuleId", is("module_001")))
                .andExpect(jsonPath("$.result[0].strategyCode", is("record_apply_record")));

        verify(moduleDataStrategyService)
                .listStrategiesByBusinessModule("tenant_001", "connector_001", "module_001");
    }

    /**
     * 校验按业务模块查询启用策略列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listActiveShouldReturnStrategyVos() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        saved.setStatus("ACTIVE");
        when(moduleDataStrategyService.listActiveStrategiesByBusinessModule("tenant_001", "connector_001", "module_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/module-data-strategies/active")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001")
                        .param("businessModuleId", "module_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result[0].id", is("strategy_001")))
                .andExpect(jsonPath("$.result[0].status", is("ACTIVE")));

        verify(moduleDataStrategyService)
                .listActiveStrategiesByBusinessModule("tenant_001", "connector_001", "module_001");
    }

    /**
     * 校验按模块编码和场景查询启用策略成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void activeBySceneShouldReturnStrategyVo() throws Exception {
        ModuleDataStrategy saved = buildSavedStrategy();
        saved.setStatus("ACTIVE");
        when(moduleDataStrategyService.getActiveStrategyByModuleCodeAndScene(
                "tenant_001", "connector_001", "record_apply", "RECORD")).thenReturn(saved);

        mockMvc.perform(get("/api/v1/connector/module-data-strategies/active/by-scene")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001")
                        .param("moduleCode", "record_apply")
                        .param("sceneType", "RECORD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.moduleCode", is("record_apply")))
                .andExpect(jsonPath("$.result.sceneType", is("RECORD")))
                .andExpect(jsonPath("$.result.status", is("ACTIVE")));

        verify(moduleDataStrategyService)
                .getActiveStrategyByModuleCodeAndScene("tenant_001", "connector_001", "record_apply", "RECORD");
    }

    /**
     * 构造 Service 返回的已保存策略。
     *
     * @return 原平台业务模块数据准备策略实体。
     */
    private ModuleDataStrategy buildSavedStrategy() {
        ModuleDataStrategy strategy = new ModuleDataStrategy();
        strategy.setId("strategy_001");
        strategy.setTenantId("tenant_001");
        strategy.setConnectorSystemId("connector_001");
        strategy.setBusinessModuleId("module_001");
        strategy.setModuleCode("record_apply");
        strategy.setModuleName("备案申请");
        strategy.setSceneType("RECORD");
        strategy.setNeedPreData(Boolean.TRUE);
        strategy.setDataSourceStrategy("MOCK_GENERATE");
        strategy.setInitExternalStatus("DRAFT");
        strategy.setTargetExternalStatus("SUBMITTED");
        strategy.setDefaultOrgRolePolicyJson("{}");
        strategy.setSharePolicy("ATTEMPT_EXCLUSIVE");
        strategy.setRegeneratePolicy("ON_ATTEMPT");
        strategy.setLockPolicy("NONE");
        strategy.setExpirePolicyJson("{}");
        strategy.setResultCheckPolicyJson("{}");
        strategy.setStrategyCode("record_apply_record");
        strategy.setTemplateId("tpl_001");
        strategy.setPrepareTiming("ON_PUBLISH");
        strategy.setPoolSizePolicyJson("{}");
        strategy.setValidationPolicyJson("{}");
        strategy.setArchivePolicyJson("{}");
        strategy.setStrategyVersion(1L);
        strategy.setStatus("DISABLED");
        strategy.setCreateTime(LocalDateTime.now());
        strategy.setUpdateTime(LocalDateTime.now());
        return strategy;
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
