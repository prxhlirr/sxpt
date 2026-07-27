package com.sxpt.module.user;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.AuthLoginService;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.service.TeachUserService;
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
 * 教学平台用户接口测试。
 *
 * 业务功能：
 * 1. 验证创建教学用户接口遵循统一响应结构。
 * 2. 验证请求参数校验能在进入 Service 前拦截无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.user.controller.enabled=true")
class TeachUserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachUserService teachUserService;

    @MockBean
    private AuthLoginService authLoginService;

    /**
     * 校验创建教学平台用户成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnTeachUserVo() throws Exception {
        TeachUser saved = buildSavedTeachUser();
        when(teachUserService.createTeachUser(any(TeachUser.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/user/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"username\":\"teacher001\",\"realName\":\"教师一\",\"phone\":\"13800000000\",\"email\":\"teacher001@example.com\",\"userType\":\"TEACHER\",\"sourceType\":\"LOCAL\",\"externalInfoJson\":\"{}\",\"employeeNo\":\"T001\",\"initialPassword\":\"StrongPassword123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("user_001")))
                .andExpect(jsonPath("$.result.username", is("teacher001")))
                .andExpect(jsonPath("$.result.employeeNo", is("T001")))
                .andExpect(jsonPath("$.result.externalInfoJson").doesNotExist());

        ArgumentCaptor<TeachUser> captor = ArgumentCaptor.forClass(TeachUser.class);
        verify(teachUserService).createTeachUser(captor.capture());
        TeachUser requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("teacher001", requestEntity.getUsername());
        org.junit.jupiter.api.Assertions.assertEquals("{}", requestEntity.getExternalInfoJson());
        org.junit.jupiter.api.Assertions.assertEquals("T001", requestEntity.getEmployeeNo());
        org.junit.jupiter.api.Assertions.assertNotEquals("StrongPassword123", requestEntity.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getPasswordSalt());
        org.junit.jupiter.api.Assertions.assertEquals("PBKDF2WithHmacSHA256", requestEntity.getPasswordAlgorithm());
        org.junit.jupiter.api.Assertions.assertEquals("NORMAL", requestEntity.getPasswordStatus());
    }

    /**
     * 校验缺少用户账号时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingUsername() throws Exception {
        mockMvc.perform(post("/api/v1/user/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"realName\":\"教师一\",\"userType\":\"TEACHER\",\"sourceType\":\"LOCAL\",\"initialPassword\":\"StrongPassword123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验本地用户必须提供初始密码，因为本地账号没有外部身份源可校验。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectLocalUserWithoutInitialPassword() throws Exception {
        mockMvc.perform(post("/api/v1/user/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"username\":\"teacher001\",\"realName\":\"教师一\",\"userType\":\"TEACHER\",\"sourceType\":\"LOCAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)));
    }

    /**
     * 构造 Service 返回的已保存教学用户。
     *
     * @return 教学平台用户实体。
     */
    private TeachUser buildSavedTeachUser() {
        TeachUser teachUser = new TeachUser();
        teachUser.setId("user_001");
        teachUser.setTenantId("tenant_001");
        teachUser.setUsername("teacher001");
        teachUser.setRealName("教师一");
        teachUser.setPhone("13800000000");
        teachUser.setEmail("teacher001@example.com");
        teachUser.setUserType("TEACHER");
        teachUser.setSourceType("LOCAL");
        teachUser.setEmployeeNo("T001");
        teachUser.setStatus("ACTIVE");
        teachUser.setCreateTime(LocalDateTime.now());
        teachUser.setUpdateTime(LocalDateTime.now());
        return teachUser;
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
