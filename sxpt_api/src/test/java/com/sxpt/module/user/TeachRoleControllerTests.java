package com.sxpt.module.user;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.user.entity.TeachRole;
import com.sxpt.module.user.service.TeachRoleService;
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
 * 教学平台角色接口测试。
 *
 * 业务功能：
 * 1. 验证创建教学角色接口遵循统一响应结构。
 * 2. 验证请求参数校验能在进入 Service 前拦截无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.role.controller.enabled=true")
class TeachRoleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachRoleService teachRoleService;

    /**
     * 校验创建教学平台角色成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnTeachRoleVo() throws Exception {
        TeachRole saved = buildSavedTeachRole();
        when(teachRoleService.createTeachRole(any(TeachRole.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/roles/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"roleCode\":\"TEACHER\",\"roleName\":\"教师\",\"description\":\"授课角色\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("role_001")))
                .andExpect(jsonPath("$.result.roleCode", is("TEACHER")))
                .andExpect(jsonPath("$.result.roleName", is("教师")));

        ArgumentCaptor<TeachRole> captor = ArgumentCaptor.forClass(TeachRole.class);
        verify(teachRoleService).createTeachRole(captor.capture());
        TeachRole requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("TEACHER", requestEntity.getRoleCode());
        org.junit.jupiter.api.Assertions.assertEquals("教师", requestEntity.getRoleName());
        org.junit.jupiter.api.Assertions.assertEquals("授课角色", requestEntity.getDescription());
    }

    /**
     * 校验缺少角色编码时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingRoleCode() throws Exception {
        mockMvc.perform(post("/api/v1/roles/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"roleName\":\"教师\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按租户查询教学角色列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listShouldReturnTeachRoleVos() throws Exception {
        TeachRole saved = buildSavedTeachRole();
        when(teachRoleService.listTeachRolesByTenantId("tenant_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result[0].roleCode", is("TEACHER")))
                .andExpect(jsonPath("$.result[0].roleName", is("教师")));

        verify(teachRoleService).listTeachRolesByTenantId("tenant_001");
    }

    /**
     * 构造 Service 返回的已保存教学角色。
     *
     * @return 教学平台角色实体。
     */
    private TeachRole buildSavedTeachRole() {
        TeachRole teachRole = new TeachRole();
        teachRole.setId("role_001");
        teachRole.setTenantId("tenant_001");
        teachRole.setRoleCode("TEACHER");
        teachRole.setRoleName("教师");
        teachRole.setDescription("授课角色");
        teachRole.setStatus("ACTIVE");
        teachRole.setCreateTime(LocalDateTime.now());
        teachRole.setUpdateTime(LocalDateTime.now());
        return teachRole;
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
