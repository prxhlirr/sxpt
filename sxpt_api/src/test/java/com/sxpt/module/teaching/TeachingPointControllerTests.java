package com.sxpt.module.teaching;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.teaching.entity.TeachingPoint;
import com.sxpt.module.teaching.service.TeachingPointService;
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
 * 教学点接口测试。
 *
 * 业务功能：
 * 1. 验证教学点发布接口遵循统一响应结构。
 * 2. 验证按原平台查询教学点接口可用，且不返回内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.teaching.point-controller.enabled=true")
class TeachingPointControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TeachingPointService teachingPointService;

    /**
     * 校验发布教学点成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnTeachingPointVo() throws Exception {
        TeachingPoint saved = buildSavedTeachingPoint();
        when(teachingPointService.createTeachingPoint(any(TeachingPoint.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/teaching/points/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"pointCode\":\"TP_RECORD\",\"pointName\":\"标准备案申请\",\"pointType\":\"SCENARIO\",\"sourceCaptureSessionId\":\"cap_001\",\"businessOverviewJson\":\"{\\\"goal\\\":\\\"掌握备案\\\"}\",\"flowFileId\":\"file_001\",\"flowFileUrl\":\"https://files.example.com/flow.png\",\"recordPathJson\":\"[{\\\"segmentNo\\\":1}]\",\"requiredExternalRoleId\":\"role_001\",\"requiredExternalRoleName\":\"经办人\",\"executionStrategy\":\"ROLE_SWITCH\",\"dataScopeJson\":\"{\\\"scope\\\":\\\"training\\\"}\",\"overlayPolicyJson\":\"{\\\"mask\\\":true}\",\"description\":\"备案教学点\",\"createBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("tp_001")))
                .andExpect(jsonPath("$.result.pointCode", is("TP_RECORD")))
                .andExpect(jsonPath("$.result.pointStatus", is("PUBLISHED")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TeachingPoint> captor = ArgumentCaptor.forClass(TeachingPoint.class);
        verify(teachingPointService).createTeachingPoint(captor.capture());
        TeachingPoint requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("TP_RECORD", requestEntity.getPointCode());
        org.junit.jupiter.api.Assertions.assertEquals("teacher_001", requestEntity.getCreateBy());
    }

    /**
     * 校验缺少教学点编码时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingPointCode() throws Exception {
        mockMvc.perform(post("/api/v1/teaching/points/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"pointName\":\"标准备案申请\",\"pointType\":\"SCENARIO\",\"executionStrategy\":\"ROLE_SWITCH\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按原平台查询教学点成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByConnectorShouldReturnTeachingPointVos() throws Exception {
        TeachingPoint saved = buildSavedTeachingPoint();
        when(teachingPointService.listByConnector("tenant_001", "connector_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/teaching/points")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("connectorSystemId", "connector_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("tp_001")))
                .andExpect(jsonPath("$.result[0].pointCode", is("TP_RECORD")));

        verify(teachingPointService).listByConnector("tenant_001", "connector_001");
    }

    @Test
    void withdrawShouldUseAuthenticatedTenantAndUser() throws Exception {
        TeachingPoint withdrawn = buildSavedTeachingPoint();
        withdrawn.setPointStatus("WITHDRAWN");
        when(teachingPointService.withdrawTeachingPoint(
                "tp_001", "tenant_001", "admin_001"))
                .thenReturn(withdrawn);

        mockMvc.perform(post("/api/v1/teaching/points/tp_001/withdraw")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.id", is("tp_001")))
                .andExpect(jsonPath("$.result.pointStatus", is("WITHDRAWN")));

        verify(teachingPointService).withdrawTeachingPoint(
                "tp_001", "tenant_001", "admin_001");
    }

    /**
     * 构造 Service 返回的已发布教学点。
     *
     * @return 教学点实体。
     */
    private TeachingPoint buildSavedTeachingPoint() {
        TeachingPoint teachingPoint = new TeachingPoint();
        teachingPoint.setId("tp_001");
        teachingPoint.setTenantId("tenant_001");
        teachingPoint.setConnectorSystemId("connector_001");
        teachingPoint.setPointCode("TP_RECORD");
        teachingPoint.setPointName("标准备案申请");
        teachingPoint.setPointType("SCENARIO");
        teachingPoint.setVersionNo(1L);
        teachingPoint.setSourceCaptureSessionId("cap_001");
        teachingPoint.setBusinessOverviewJson("{\"goal\":\"掌握备案\"}");
        teachingPoint.setFlowFileId("file_001");
        teachingPoint.setFlowFileUrl("https://files.example.com/flow.png");
        teachingPoint.setRecordPathJson("[{\"segmentNo\":1}]");
        teachingPoint.setRequiredExternalRoleId("role_001");
        teachingPoint.setRequiredExternalRoleName("经办人");
        teachingPoint.setExecutionStrategy("ROLE_SWITCH");
        teachingPoint.setDataScopeJson("{\"scope\":\"training\"}");
        teachingPoint.setOverlayPolicyJson("{\"mask\":true}");
        teachingPoint.setDescription("备案教学点");
        teachingPoint.setPointStatus("PUBLISHED");
        teachingPoint.setStatus("ACTIVE");
        teachingPoint.setCreateTime(LocalDateTime.now());
        teachingPoint.setUpdateTime(LocalDateTime.now());
        return teachingPoint;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken(
                "admin_001", "admin", "tenant_001", Collections.singletonList("ADMIN"));
    }
}
