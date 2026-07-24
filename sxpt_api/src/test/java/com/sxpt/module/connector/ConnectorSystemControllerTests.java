package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.service.ConnectorSystemService;
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
 * 原业务平台配置接口测试。
 *
 * 业务功能：
 * 1. 验证创建原平台配置接口遵循统一响应结构。
 * 2. 验证请求参数校验能在进入 Service 前拦截无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.system-controller.enabled=true")
class ConnectorSystemControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ConnectorSystemService connectorSystemService;

    /**
     * 校验创建原平台配置成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnConnectorSystemVo() throws Exception {
        ConnectorSystem saved = buildSavedConnectorSystem();
        when(connectorSystemService.createConnectorSystem(any(ConnectorSystem.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/system/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"systemCode\":\"origin_platform\",\"systemName\":\"原业务平台\",\"systemType\":\"CUSTOM\",\"baseUrl\":\"https://origin.example.com\",\"authType\":\"TOKEN\",\"configJson\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("connector_001")))
                .andExpect(jsonPath("$.result.systemCode", is("origin_platform")))
                .andExpect(jsonPath("$.result.configJson").doesNotExist());

        ArgumentCaptor<ConnectorSystem> captor = ArgumentCaptor.forClass(ConnectorSystem.class);
        verify(connectorSystemService).createConnectorSystem(captor.capture());
        ConnectorSystem requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("origin_platform", requestEntity.getSystemCode());
        org.junit.jupiter.api.Assertions.assertEquals("{}", requestEntity.getConfigJson());
    }

    /**
     * 校验缺少原平台编码时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingSystemCode() throws Exception {
        mockMvc.perform(post("/api/v1/connector/system/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"systemName\":\"原业务平台\",\"systemType\":\"CUSTOM\",\"baseUrl\":\"https://origin.example.com\",\"authType\":\"TOKEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验更新原平台配置成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void updateShouldReturnConnectorSystemVo() throws Exception {
        ConnectorSystem saved = buildSavedConnectorSystem();
        saved.setSystemName("更新后的原业务平台");
        saved.setBaseUrl("https://new-origin.example.com");
        when(connectorSystemService.updateConnectorSystem(any(ConnectorSystem.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/system/update")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"connector_001\",\"systemName\":\"更新后的原业务平台\",\"systemType\":\"CUSTOM\",\"baseUrl\":\"https://new-origin.example.com\",\"authType\":\"TOKEN\",\"configJson\":\"{}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("connector_001")))
                .andExpect(jsonPath("$.result.systemName", is("更新后的原业务平台")))
                .andExpect(jsonPath("$.result.configJson").doesNotExist());

        ArgumentCaptor<ConnectorSystem> captor = ArgumentCaptor.forClass(ConnectorSystem.class);
        verify(connectorSystemService).updateConnectorSystem(captor.capture());
        ConnectorSystem requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("更新后的原业务平台", requestEntity.getSystemName());
        org.junit.jupiter.api.Assertions.assertEquals("https://new-origin.example.com", requestEntity.getBaseUrl());
        org.junit.jupiter.api.Assertions.assertEquals("{}", requestEntity.getConfigJson());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals(null, requestEntity.getSystemCode());
    }

    /**
     * 校验缺少原平台配置 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void updateShouldRejectMissingId() throws Exception {
        mockMvc.perform(post("/api/v1/connector/system/update")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemName\":\"更新后的原业务平台\",\"systemType\":\"CUSTOM\",\"baseUrl\":\"https://new-origin.example.com\",\"authType\":\"TOKEN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验启用原平台配置成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void enableShouldReturnActiveConnectorSystemVo() throws Exception {
        ConnectorSystem saved = buildSavedConnectorSystem();
        saved.setStatus("ACTIVE");
        when(connectorSystemService.enableConnectorSystem("connector_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/system/connector_001/enable")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("connector_001")))
                .andExpect(jsonPath("$.result.status", is("ACTIVE")));

        verify(connectorSystemService).enableConnectorSystem("connector_001");
    }

    /**
     * 校验禁用原平台配置成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void disableShouldReturnDisabledConnectorSystemVo() throws Exception {
        ConnectorSystem saved = buildSavedConnectorSystem();
        saved.setStatus("DISABLED");
        when(connectorSystemService.disableConnectorSystem("connector_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/system/connector_001/disable")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("connector_001")))
                .andExpect(jsonPath("$.result.status", is("DISABLED")));

        verify(connectorSystemService).disableConnectorSystem("connector_001");
    }

    /**
     * 校验查询原平台配置详情成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void detailShouldReturnConnectorSystemVo() throws Exception {
        ConnectorSystem saved = buildSavedConnectorSystem();
        when(connectorSystemService.getConnectorSystemById("connector_001")).thenReturn(saved);

        mockMvc.perform(get("/api/v1/connector/system/connector_001")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("connector_001")))
                .andExpect(jsonPath("$.result.systemName", is("原业务平台")))
                .andExpect(jsonPath("$.result.configJson").doesNotExist());

        verify(connectorSystemService).getConnectorSystemById("connector_001");
    }

    /**
     * 校验按租户查询原平台配置列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listShouldReturnConnectorSystemVos() throws Exception {
        ConnectorSystem saved = buildSavedConnectorSystem();
        when(connectorSystemService.listConnectorSystemsByTenantId("tenant_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/system/list")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result[0].systemCode", is("origin_platform")))
                .andExpect(jsonPath("$.result[0].configJson").doesNotExist());

        verify(connectorSystemService).listConnectorSystemsByTenantId("tenant_001");
    }

    /**
     * 构造 Service 返回的已保存原平台配置。
     *
     * @return 原平台配置实体。
     */
    private ConnectorSystem buildSavedConnectorSystem() {
        ConnectorSystem connectorSystem = new ConnectorSystem();
        connectorSystem.setId("connector_001");
        connectorSystem.setTenantId("tenant_001");
        connectorSystem.setSystemCode("origin_platform");
        connectorSystem.setSystemName("原业务平台");
        connectorSystem.setSystemType("CUSTOM");
        connectorSystem.setBaseUrl("https://origin.example.com");
        connectorSystem.setAuthType("TOKEN");
        connectorSystem.setStatus("ACTIVE");
        connectorSystem.setCreateTime(LocalDateTime.now());
        connectorSystem.setUpdateTime(LocalDateTime.now());
        return connectorSystem;
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
