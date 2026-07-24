package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.service.ConnectorResourceService;
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
 * 原平台正式资源接口测试。
 *
 * 业务功能：
 * 1. 验证正式资源创建接口遵循统一响应结构。
 * 2. 验证按页面查询正式资源接口可用，且不返回内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.resource-controller.enabled=true")
class ConnectorResourceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ConnectorResourceService connectorResourceService;

    /**
     * 校验创建正式资源成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnConnectorResourceVo() throws Exception {
        ConnectorResource saved = buildSavedResource();
        when(connectorResourceService.createConnectorResource(any(ConnectorResource.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/resources/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"resourceCode\":\"BTN_SUBMIT\",\"resourceName\":\"提交按钮\",\"resourceType\":\"BUTTON\",\"pageUrl\":\"https://origin.example.com/record/start\",\"locator\":\"#submit\",\"stableKey\":\"submit_button\",\"metadataJson\":\"{\\\"text\\\":\\\"提交\\\"}\",\"sourceCaptureId\":\"cap_001\",\"createBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("res_001")))
                .andExpect(jsonPath("$.result.resourceCode", is("BTN_SUBMIT")))
                .andExpect(jsonPath("$.result.resourceType", is("BUTTON")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<ConnectorResource> captor = ArgumentCaptor.forClass(ConnectorResource.class);
        verify(connectorResourceService).createConnectorResource(captor.capture());
        ConnectorResource requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("BTN_SUBMIT", requestEntity.getResourceCode());
        org.junit.jupiter.api.Assertions.assertEquals("teacher_001", requestEntity.getCreateBy());
    }

    /**
     * 校验缺少资源编码时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingResourceCode() throws Exception {
        mockMvc.perform(post("/api/v1/connector/resources/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"resourceName\":\"提交按钮\",\"resourceType\":\"BUTTON\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按页面查询正式资源成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByPageShouldReturnConnectorResourceVos() throws Exception {
        ConnectorResource saved = buildSavedResource();
        when(connectorResourceService.listByPage(
                "tenant_001", "connector_001", "https://origin.example.com/record/start"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/connector/resources")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001")
                        .param("pageUrl", "https://origin.example.com/record/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("res_001")))
                .andExpect(jsonPath("$.result[0].resourceCode", is("BTN_SUBMIT")));

        verify(connectorResourceService).listByPage(
                "tenant_001", "connector_001", "https://origin.example.com/record/start");
    }

    /**
     * 构造 Service 返回的已保存正式资源。
     *
     * @return 原平台正式资源实体。
     */
    private ConnectorResource buildSavedResource() {
        ConnectorResource resource = new ConnectorResource();
        resource.setId("res_001");
        resource.setTenantId("tenant_001");
        resource.setConnectorSystemId("connector_001");
        resource.setResourceCode("BTN_SUBMIT");
        resource.setResourceName("提交按钮");
        resource.setResourceType("BUTTON");
        resource.setPageUrl("https://origin.example.com/record/start");
        resource.setLocator("#submit");
        resource.setStableKey("submit_button");
        resource.setMetadataJson("{\"text\":\"提交\"}");
        resource.setSourceCaptureId("cap_001");
        resource.setStatus("ACTIVE");
        resource.setCreateTime(LocalDateTime.now());
        resource.setUpdateTime(LocalDateTime.now());
        return resource;
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
