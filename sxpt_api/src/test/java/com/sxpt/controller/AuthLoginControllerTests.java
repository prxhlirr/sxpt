package com.sxpt.controller;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.AuthLoginService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 正式登录接口测试。
 *
 * 业务功能：
 * 1. 验证 /api/v1/auth/login 是正式登录入口且无需预先携带 JWT。
 * 2. 验证登录请求必须包含 loginType、tenantId、username、password。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用登录接口。
 * 2. 使用 MockBean 隔离 Service，聚焦 Controller 路由和参数校验。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthLoginService authLoginService;

    /**
     * 校验登录接口无需 Bearer Token 即可访问。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void loginShouldReturnTokenWithoutBearerToken() throws Exception {
        when(authLoginService.login(any(AuthLoginRequest.class))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"tenantId\":\"tenant_001\",\"username\":\"teacher001\",\"password\":\"StrongPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.token", is("jwt-token")))
                .andExpect(jsonPath("$.result.user.userId", is("user_001")))
                .andExpect(jsonPath("$.result.user.employeeNo", is("T001")));
    }

    /**
     * 校验缺少密码时在 Controller 层返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void loginShouldRejectMissingPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"tenantId\":\"tenant_001\",\"username\":\"teacher001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)));
    }

    private AuthLoginResponse buildResponse() {
        AuthLoginResponse response = new AuthLoginResponse();
        response.setToken("jwt-token");
        response.setTokenType("Bearer");
        response.setExpiresIn(7200L);
        AuthLoginResponse.UserSummary user = new AuthLoginResponse.UserSummary();
        user.setUserId("user_001");
        user.setTenantId("tenant_001");
        user.setUsername("teacher001");
        user.setDisplayName("教师一");
        user.setUserType("TEACHER");
        user.setEmployeeNo("T001");
        response.setUser(user);
        return response;
    }
}
