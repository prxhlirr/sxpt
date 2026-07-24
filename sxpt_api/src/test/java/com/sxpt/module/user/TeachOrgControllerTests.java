package com.sxpt.module.user;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.user.entity.TeachOrg;
import com.sxpt.module.user.service.TeachOrgService;
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
 * 教学组织接口测试。
 *
 * 业务功能：
 * 1. 验证创建教学组织接口遵循统一响应结构。
 * 2. 验证组织列表接口能按租户查询。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.org.controller.enabled=true")
class TeachOrgControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachOrgService teachOrgService;

    /**
     * 校验创建教学组织成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnTeachOrgVo() throws Exception {
        TeachOrg saved = buildSavedTeachOrg();
        when(teachOrgService.createTeachOrg(any(TeachOrg.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/orgs/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"orgCode\":\"class_2026_01\",\"orgName\":\"2026级实训1班\",\"orgType\":\"CLASS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("org_001")))
                .andExpect(jsonPath("$.result.orgCode", is("class_2026_01")))
                .andExpect(jsonPath("$.result.orgName", is("2026级实训1班")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TeachOrg> captor = ArgumentCaptor.forClass(TeachOrg.class);
        verify(teachOrgService).createTeachOrg(captor.capture());
        TeachOrg requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("class_2026_01", requestEntity.getOrgCode());
        org.junit.jupiter.api.Assertions.assertEquals("CLASS", requestEntity.getOrgType());
    }

    /**
     * 校验缺少组织编码时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingOrgCode() throws Exception {
        mockMvc.perform(post("/api/v1/orgs/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"orgName\":\"2026级实训1班\",\"orgType\":\"CLASS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按租户查询教学组织列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listShouldReturnTeachOrgVos() throws Exception {
        TeachOrg saved = buildSavedTeachOrg();
        when(teachOrgService.listTeachOrgsByTenantId("tenant_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/orgs")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].tenantId", is("tenant_001")))
                .andExpect(jsonPath("$.result[0].orgCode", is("class_2026_01")))
                .andExpect(jsonPath("$.result[0].orgType", is("CLASS")));

        verify(teachOrgService).listTeachOrgsByTenantId("tenant_001");
    }

    /**
     * 构造 Service 返回的已保存教学组织。
     *
     * @return 教学组织实体。
     */
    private TeachOrg buildSavedTeachOrg() {
        TeachOrg teachOrg = new TeachOrg();
        teachOrg.setId("org_001");
        teachOrg.setTenantId("tenant_001");
        teachOrg.setOrgCode("class_2026_01");
        teachOrg.setOrgName("2026级实训1班");
        teachOrg.setOrgType("CLASS");
        teachOrg.setStatus("ACTIVE");
        teachOrg.setCreateTime(LocalDateTime.now());
        teachOrg.setUpdateTime(LocalDateTime.now());
        return teachOrg;
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
