package com.sxpt.module.user;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.user.entity.TeachUserRole;
import com.sxpt.module.user.service.TeachUserRoleService;
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
 * 教学平台用户角色关系接口测试。
 *
 * 业务功能：
 * 1. 验证授予教学角色接口遵循统一响应结构。
 * 2. 验证请求参数校验能在进入 Service 前拦截无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.user-role.controller.enabled=true")
class TeachUserRoleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachUserRoleService teachUserRoleService;

    /**
     * 校验授予教学平台角色成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void grantShouldReturnTeachUserRoleVo() throws Exception {
        TeachUserRole saved = buildSavedTeachUserRole();
        when(teachUserRoleService.grantUserRole(any(TeachUserRole.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/user-roles/grant")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"userId\":\"user_001\",\"roleId\":\"role_001\",\"grantSource\":\"MANUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("user_role_001")))
                .andExpect(jsonPath("$.result.userId", is("user_001")))
                .andExpect(jsonPath("$.result.roleId", is("role_001")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TeachUserRole> captor = ArgumentCaptor.forClass(TeachUserRole.class);
        verify(teachUserRoleService).grantUserRole(captor.capture());
        TeachUserRole requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("user_001", requestEntity.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals("role_001", requestEntity.getRoleId());
        org.junit.jupiter.api.Assertions.assertEquals("MANUAL", requestEntity.getGrantSource());
    }

    /**
     * 校验缺少用户 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void grantShouldRejectMissingUserId() throws Exception {
        mockMvc.perform(post("/api/v1/user-roles/grant")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"roleId\":\"role_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造 Service 返回的已保存用户角色关系。
     *
     * @return 用户角色关系实体。
     */
    private TeachUserRole buildSavedTeachUserRole() {
        TeachUserRole teachUserRole = new TeachUserRole();
        teachUserRole.setId("user_role_001");
        teachUserRole.setTenantId("tenant_001");
        teachUserRole.setUserId("user_001");
        teachUserRole.setRoleId("role_001");
        teachUserRole.setGrantSource("MANUAL");
        teachUserRole.setStatus("ACTIVE");
        teachUserRole.setCreateTime(LocalDateTime.now());
        teachUserRole.setUpdateTime(LocalDateTime.now());
        return teachUserRole;
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
