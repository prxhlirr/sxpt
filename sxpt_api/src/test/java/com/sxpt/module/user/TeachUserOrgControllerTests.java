package com.sxpt.module.user;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.user.entity.TeachUserOrg;
import com.sxpt.module.user.service.TeachUserOrgService;
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
 * 教学用户组织关系接口测试。
 *
 * 业务功能：
 * 1. 验证添加用户到教学组织接口遵循统一响应结构。
 * 2. 验证请求参数校验能在进入 Service 前拦截无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.user-org.controller.enabled=true")
class TeachUserOrgControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachUserOrgService teachUserOrgService;

    /**
     * 校验添加用户到教学组织成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void addShouldReturnTeachUserOrgVo() throws Exception {
        TeachUserOrg saved = buildSavedTeachUserOrg();
        when(teachUserOrgService.addUserToOrg(any(TeachUserOrg.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/orgs/users/add")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"orgId\":\"org_001\",\"userId\":\"user_001\",\"relationType\":\"STUDENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("user_org_001")))
                .andExpect(jsonPath("$.result.orgId", is("org_001")))
                .andExpect(jsonPath("$.result.userId", is("user_001")))
                .andExpect(jsonPath("$.result.relationType", is("STUDENT")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TeachUserOrg> captor = ArgumentCaptor.forClass(TeachUserOrg.class);
        verify(teachUserOrgService).addUserToOrg(captor.capture());
        TeachUserOrg requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("org_001", requestEntity.getOrgId());
        org.junit.jupiter.api.Assertions.assertEquals("user_001", requestEntity.getUserId());
        org.junit.jupiter.api.Assertions.assertEquals("STUDENT", requestEntity.getRelationType());
    }

    /**
     * 校验缺少组织 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void addShouldRejectMissingOrgId() throws Exception {
        mockMvc.perform(post("/api/v1/orgs/users/add")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"userId\":\"user_001\",\"relationType\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验从教学组织移除用户成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void removeShouldReturnTeachUserOrgVo() throws Exception {
        TeachUserOrg removed = buildSavedTeachUserOrg();
        removed.setStatus("DISABLED");
        when(teachUserOrgService.removeUserFromOrg("tenant_001", "org_001", "user_001")).thenReturn(removed);

        mockMvc.perform(post("/api/v1/orgs/users/remove")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"orgId\":\"org_001\",\"userId\":\"user_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.orgId", is("org_001")))
                .andExpect(jsonPath("$.result.userId", is("user_001")))
                .andExpect(jsonPath("$.result.status", is("DISABLED")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        verify(teachUserOrgService).removeUserFromOrg("tenant_001", "org_001", "user_001");
    }

    /**
     * 校验移除成员时缺少用户 ID 返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void removeShouldRejectMissingUserId() throws Exception {
        mockMvc.perform(post("/api/v1/orgs/users/remove")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"orgId\":\"org_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造 Service 返回的已保存用户组织关系。
     *
     * @return 用户组织关系实体。
     */
    private TeachUserOrg buildSavedTeachUserOrg() {
        TeachUserOrg teachUserOrg = new TeachUserOrg();
        teachUserOrg.setId("user_org_001");
        teachUserOrg.setTenantId("tenant_001");
        teachUserOrg.setOrgId("org_001");
        teachUserOrg.setUserId("user_001");
        teachUserOrg.setRelationType("STUDENT");
        teachUserOrg.setStatus("ACTIVE");
        teachUserOrg.setCreateTime(LocalDateTime.now());
        teachUserOrg.setUpdateTime(LocalDateTime.now());
        return teachUserOrg;
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
