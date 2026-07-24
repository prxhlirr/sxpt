package com.sxpt.module.capture;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.service.CaptureEventService;
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
 * SDK 采集事件接口测试。
 *
 * 业务功能：
 * 1. 验证 SDK 事件上报接口遵循统一响应结构。
 * 2. 验证按采集会话查询事件接口可用，且不返回内部摘要 JSON 和软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.capture.event-controller.enabled=true")
class CaptureEventControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CaptureEventService captureEventService;

    /**
     * 校验事件上报成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldReturnCaptureEventVo() throws Exception {
        CaptureEvent saved = buildSavedEvent();
        when(captureEventService.reportCaptureEvent(any(CaptureEvent.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/capture/events/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"captureSessionId\":\"cap_001\",\"sdkSessionId\":\"sdk_001\",\"clientEventId\":\"client_evt_001\",\"eventType\":\"CLICK\",\"eventTime\":\"2026-07-15T19:45:00\",\"sequenceNo\":1,\"retryCount\":0,\"pageUrl\":\"https://origin.example.com/record/start\",\"targetText\":\"提交\",\"targetLocator\":\"#submit\",\"targetStableKey\":\"submit_button\",\"eventPayloadJson\":\"{\\\"source\\\":\\\"sdk\\\"}\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("evt_001")))
                .andExpect(jsonPath("$.result.clientEventId", is("client_evt_001")))
                .andExpect(jsonPath("$.result.eventType", is("CLICK")))
                .andExpect(jsonPath("$.result.sequenceNo", is(1)))
                .andExpect(jsonPath("$.result.eventPayloadJson").doesNotExist())
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<CaptureEvent> captor = ArgumentCaptor.forClass(CaptureEvent.class);
        verify(captureEventService).reportCaptureEvent(captor.capture());
        CaptureEvent requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("cap_001", requestEntity.getCaptureSessionId());
        org.junit.jupiter.api.Assertions.assertEquals("client_evt_001", requestEntity.getClientEventId());
        org.junit.jupiter.api.Assertions.assertEquals("CLICK", requestEntity.getEventType());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1), requestEntity.getSequenceNo());
    }

    /**
     * 校验缺少客户端事件 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldRejectMissingClientEventId() throws Exception {
        mockMvc.perform(post("/api/v1/capture/events/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"captureSessionId\":\"cap_001\",\"eventType\":\"CLICK\",\"eventTime\":\"2026-07-15T19:45:00\",\"sequenceNo\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按采集会话查询事件列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listBySessionShouldReturnCaptureEventVos() throws Exception {
        CaptureEvent saved = buildSavedEvent();
        when(captureEventService.listBySession("tenant_001", "cap_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/capture/events")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("captureSessionId", "cap_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("evt_001")))
                .andExpect(jsonPath("$.result[0].captureSessionId", is("cap_001")))
                .andExpect(jsonPath("$.result[0].clientEventId", is("client_evt_001")));

        verify(captureEventService).listBySession("tenant_001", "cap_001");
    }

    /**
     * 构造 Service 返回的已保存事件。
     *
     * @return SDK 采集事件实体。
     */
    private CaptureEvent buildSavedEvent() {
        CaptureEvent captureEvent = new CaptureEvent();
        captureEvent.setId("evt_001");
        captureEvent.setTenantId("tenant_001");
        captureEvent.setCaptureSessionId("cap_001");
        captureEvent.setSdkSessionId("sdk_001");
        captureEvent.setClientEventId("client_evt_001");
        captureEvent.setEventType("CLICK");
        captureEvent.setEventTime(LocalDateTime.of(2026, 7, 15, 19, 45));
        captureEvent.setSequenceNo(1L);
        captureEvent.setRetryCount(0L);
        captureEvent.setPageUrl("https://origin.example.com/record/start");
        captureEvent.setTargetText("提交");
        captureEvent.setTargetLocator("#submit");
        captureEvent.setTargetStableKey("submit_button");
        captureEvent.setArchiveStatus("NONE");
        captureEvent.setStatus("ACTIVE");
        captureEvent.setCreateTime(LocalDateTime.now());
        return captureEvent;
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
