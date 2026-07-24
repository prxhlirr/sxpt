package com.sxpt.module.capture;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.capture.entity.CaptureSession;
import com.sxpt.module.capture.service.CaptureSessionService;
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
 * 备案采集会话接口测试。
 *
 * 业务功能：
 * 1. 验证教师开始备案接口遵循统一响应结构。
 * 2. 验证教师会话查询和结束采集接口可用。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.capture.session-controller.enabled=true")
class CaptureSessionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CaptureSessionService captureSessionService;

    /**
     * 校验创建备案采集会话成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnCaptureSessionVo() throws Exception {
        CaptureSession saved = buildSavedSession();
        when(captureSessionService.createCaptureSession(any(CaptureSession.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/capture/sessions/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"teacherId\":\"teacher_001\",\"sessionName\":\"标准备案申请采集\",\"businessName\":\"标准备案申请\",\"captureMode\":\"STANDARD\",\"startUrl\":\"https://origin.example.com/record/start\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("cap_001")))
                .andExpect(jsonPath("$.result.sessionStatus", is("RUNNING")))
                .andExpect(jsonPath("$.result.startUrl", is("https://origin.example.com/record/start")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<CaptureSession> captor = ArgumentCaptor.forClass(CaptureSession.class);
        verify(captureSessionService).createCaptureSession(captor.capture());
        CaptureSession requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("connector_001", requestEntity.getConnectorSystemId());
        org.junit.jupiter.api.Assertions.assertEquals("teacher_001", requestEntity.getTeacherId());
        org.junit.jupiter.api.Assertions.assertEquals("标准备案申请采集", requestEntity.getSessionName());
        org.junit.jupiter.api.Assertions.assertEquals("https://origin.example.com/record/start", requestEntity.getStartUrl());
        org.junit.jupiter.api.Assertions.assertEquals("teacher_001", requestEntity.getCreateBy());
    }

    /**
     * 校验缺少开始地址时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingStartUrl() throws Exception {
        mockMvc.perform(post("/api/v1/capture/sessions/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"connectorSystemId\":\"connector_001\",\"teacherId\":\"teacher_001\",\"sessionName\":\"标准备案申请采集\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按教师查询采集会话列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByTeacherShouldReturnCaptureSessionVos() throws Exception {
        CaptureSession saved = buildSavedSession();
        when(captureSessionService.listByTeacher("tenant_001", "teacher_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/capture/sessions")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("teacherId", "teacher_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("cap_001")))
                .andExpect(jsonPath("$.result[0].teacherId", is("teacher_001")));

        verify(captureSessionService).listByTeacher("tenant_001", "teacher_001");
    }

    /**
     * 校验结束采集会话成功返回结束状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void finishShouldReturnFinishedCaptureSessionVo() throws Exception {
        CaptureSession saved = buildSavedSession();
        saved.setSessionStatus("FINISHED");
        saved.setEndTime(LocalDateTime.now());
        when(captureSessionService.finishCaptureSession("cap_001")).thenReturn(saved);

        mockMvc.perform(post("/api/v1/capture/sessions/cap_001/finish")
                        .header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("cap_001")))
                .andExpect(jsonPath("$.result.sessionStatus", is("FINISHED")))
                .andExpect(jsonPath("$.result.endTime", notNullValue()));

        verify(captureSessionService).finishCaptureSession("cap_001");
    }

    /**
     * 构造 Service 返回的已保存会话。
     *
     * @return 备案采集会话实体。
     */
    private CaptureSession buildSavedSession() {
        CaptureSession captureSession = new CaptureSession();
        captureSession.setId("cap_001");
        captureSession.setTenantId("tenant_001");
        captureSession.setConnectorSystemId("connector_001");
        captureSession.setTeacherId("teacher_001");
        captureSession.setSessionName("标准备案申请采集");
        captureSession.setBusinessName("标准备案申请");
        captureSession.setCaptureMode("STANDARD");
        captureSession.setStartUrl("https://origin.example.com/record/start");
        captureSession.setStartTime(LocalDateTime.now());
        captureSession.setSessionStatus("RUNNING");
        captureSession.setStatus("ACTIVE");
        captureSession.setCreateTime(LocalDateTime.now());
        captureSession.setUpdateTime(LocalDateTime.now());
        return captureSession;
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
