package com.sxpt.module.connector;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.entity.IdentityBinding;
import com.sxpt.module.connector.service.IdentityBindingService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 原平台身份绑定接口测试。
 *
 * 业务功能：
 * 1. 验证身份绑定创建接口遵循统一响应结构。
 * 2. 验证按教学用户和原平台配置查询绑定身份的接口契约。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 * 3. 校验 VO 不暴露原平台角色和组织快照，保持外部平台结构隔离。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.connector.identity-binding-controller.enabled=true")
class IdentityBindingControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private IdentityBindingService identityBindingService;

    /**
     * 校验创建身份绑定成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnIdentityBindingVo() throws Exception {
        IdentityBinding saved = buildSavedIdentityBinding();
        when(identityBindingService.createIdentityBinding(any(IdentityBinding.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/connector/identity-bindings/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"userId\":\"user_001\",\"connectorSystemId\":\"connector_001\",\"externalUserId\":\"ext_user_001\",\"externalUsername\":\"origin_user\",\"externalRoleJson\":\"[]\",\"externalOrgJson\":\"[]\",\"bindingType\":\"MANUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("binding_001")))
                .andExpect(jsonPath("$.result.tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result.userId", is("user_001")))
                .andExpect(jsonPath("$.result.connectorSystemId", is("connector_001")))
                .andExpect(jsonPath("$.result.externalUserId", is("ext_user_001")))
                .andExpect(jsonPath("$.result.bindingType", is("MANUAL")))
                .andExpect(jsonPath("$.result.externalRoleJson").doesNotExist())
                .andExpect(jsonPath("$.result.externalOrgJson").doesNotExist())
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<IdentityBinding> captor = ArgumentCaptor.forClass(IdentityBinding.class);
        verify(identityBindingService).createIdentityBinding(captor.capture());
        IdentityBinding requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("user_001", requestEntity.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("ext_user_001", requestEntity.getExternalUserId());
        org.junit.jupiter.api.Assertions.assertEquals("origin_user", requestEntity.getExternalUsername());
        org.junit.jupiter.api.Assertions.assertEquals("[]", requestEntity.getExternalRoleJson());
        org.junit.jupiter.api.Assertions.assertEquals("[]", requestEntity.getExternalOrgJson());
        org.junit.jupiter.api.Assertions.assertEquals("MANUAL", requestEntity.getBindingType());
    }

    /**
     * 校验缺少原平台用户 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingExternalUserId() throws Exception {
        mockMvc.perform(post("/api/v1/connector/identity-bindings/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"userId\":\"user_001\",\"connectorSystemId\":\"connector_001\",\"bindingType\":\"MANUAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按教学用户和原平台配置查询身份绑定成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getByUserAndConnectorShouldReturnIdentityBindingVo() throws Exception {
        IdentityBinding saved = buildSavedIdentityBinding();
        when(identityBindingService.getBindingByUserAndConnector("tenant_001", "user_001", "connector_001"))
                .thenReturn(saved);

        mockMvc.perform(get("/api/v1/connector/identity-bindings/user")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("userId", "user_001")
                        .param("connectorSystemId", "connector_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("binding_001")))
                .andExpect(jsonPath("$.result.externalUsername", is("origin_user")))
                .andExpect(jsonPath("$.result.status", is("ACTIVE")))
                .andExpect(jsonPath("$.result.externalRoleJson").doesNotExist())
                .andExpect(jsonPath("$.result.externalOrgJson").doesNotExist());

        verify(identityBindingService).getBindingByUserAndConnector("tenant_001", "user_001", "connector_001");
    }

    /**
     * 构造 Service 返回的已保存身份绑定。
     *
     * @return 身份绑定实体。
     */
    private IdentityBinding buildSavedIdentityBinding() {
        IdentityBinding binding = new IdentityBinding();
        binding.setId("binding_001");
        binding.setTenantId("tenant_001");
        binding.setUserId("user_001");
        binding.setConnectorSystemId("connector_001");
        binding.setExternalUserId("ext_user_001");
        binding.setExternalUsername("origin_user");
        binding.setExternalRoleJson("[]");
        binding.setExternalOrgJson("[]");
        binding.setBindingType("MANUAL");
        binding.setStatus("ACTIVE");
        binding.setCreateTime(LocalDateTime.now());
        binding.setUpdateTime(LocalDateTime.now());
        return binding;
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
