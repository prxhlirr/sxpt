package com.sxpt.module.execution;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.execution.entity.ExecutionTrace;
import com.sxpt.module.execution.service.ExecutionTraceService;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 学生执行轨迹接口测试。
 *
 * 业务功能：
 * 1. 验证 SDK 执行轨迹上报接口遵循统一响应结构。
 * 2. 验证按任务执行查询轨迹接口可用，且不返回内部证据 JSON 和软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "sxpt.execution.trace-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class ExecutionTraceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ExecutionTraceService executionTraceService;

    /**
     * 校验轨迹上报成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldReturnExecutionTraceVo() throws Exception {
        ExecutionTrace saved = buildSavedTrace();
        when(executionTraceService.reportExecutionTrace(any(ExecutionTrace.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/execution/traces/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"sdkSessionId\":\"sdk_001\",\"clientTraceId\":\"client_trace_001\",\"taskStepId\":\"step_001\",\"teachingPointId\":\"tp_001\",\"resourceId\":\"res_001\",\"traceType\":\"CLICK\",\"traceTime\":\"2026-07-16T13:20:00\",\"sequenceNo\":1,\"retryCount\":0,\"inputDataJson\":\"{\\\"masked\\\":true}\",\"evidenceJson\":\"{\\\"resource\\\":\\\"res_001\\\"}\",\"success\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("trace_001")))
                .andExpect(jsonPath("$.result.clientTraceId", is("client_trace_001")))
                .andExpect(jsonPath("$.result.traceType", is("CLICK")))
                .andExpect(jsonPath("$.result.sequenceNo", is(1)))
                .andExpect(jsonPath("$.result.inputDataJson").doesNotExist())
                .andExpect(jsonPath("$.result.evidenceJson").doesNotExist())
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<ExecutionTrace> captor = ArgumentCaptor.forClass(ExecutionTrace.class);
        verify(executionTraceService).reportExecutionTrace(captor.capture());
        ExecutionTrace requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("execution_001", requestEntity.getExecutionId());
        assertEquals("client_trace_001", requestEntity.getClientTraceId());
        assertEquals("CLICK", requestEntity.getTraceType());
        assertEquals(Long.valueOf(1), requestEntity.getSequenceNo());
    }

    /**
     * 校验缺少客户端轨迹 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldRejectMissingClientTraceId() throws Exception {
        mockMvc.perform(post("/api/v1/execution/traces/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"traceType\":\"CLICK\",\"traceTime\":\"2026-07-16T13:20:00\",\"sequenceNo\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验按任务执行查询轨迹列表成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByExecutionShouldReturnExecutionTraceVos() throws Exception {
        ExecutionTrace saved = buildSavedTrace();
        when(executionTraceService.listByExecution("tenant_001", "execution_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/execution/traces")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("executionId", "execution_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("trace_001")))
                .andExpect(jsonPath("$.result[0].executionId", is("execution_001")))
                .andExpect(jsonPath("$.result[0].clientTraceId", is("client_trace_001")));

        verify(executionTraceService).listByExecution("tenant_001", "execution_001");
    }

    /**
     * 构造 Service 返回的已保存轨迹。
     *
     * @return 学生执行轨迹实体。
     */
    private ExecutionTrace buildSavedTrace() {
        ExecutionTrace executionTrace = new ExecutionTrace();
        executionTrace.setId("trace_001");
        executionTrace.setTenantId("tenant_001");
        executionTrace.setExecutionId("execution_001");
        executionTrace.setSdkSessionId("sdk_001");
        executionTrace.setClientTraceId("client_trace_001");
        executionTrace.setTaskStepId("step_001");
        executionTrace.setTeachingPointId("tp_001");
        executionTrace.setResourceId("res_001");
        executionTrace.setTraceType("CLICK");
        executionTrace.setTraceTime(LocalDateTime.of(2026, 7, 16, 13, 20));
        executionTrace.setSequenceNo(1L);
        executionTrace.setRetryCount(0L);
        executionTrace.setSuccess(Boolean.TRUE);
        executionTrace.setArchiveStatus("NONE");
        executionTrace.setStatus("ACTIVE");
        executionTrace.setCreateTime(LocalDateTime.now());
        return executionTrace;
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
