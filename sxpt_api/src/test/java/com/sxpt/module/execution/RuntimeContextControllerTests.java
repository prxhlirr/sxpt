package com.sxpt.module.execution;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SDK 运行上下文接口测试。
 *
 * 业务功能：
 * 1. 验证 SDK 可通过 HTTP 接口读取任务运行上下文。
 * 2. 验证缺少任务执行 ID 时由 Web 层拦截无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用真实路由和参数绑定。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "sxpt.execution.runtime-context-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class RuntimeContextControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private RuntimeContextService runtimeContextService;

    /**
     * 校验成功获取 SDK 运行上下文。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getRuntimeContextShouldReturnContext() throws Exception {
        RuntimeContextVO vo = buildRuntimeContextVO();
        when(runtimeContextService.getRuntimeContext(any(RuntimeContextQueryRequest.class))).thenReturn(vo);

        mockMvc.perform(get("/api/v1/sdk/runtime-context")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("executionId", "execution_001")
                        .param("taskId", "task_001")
                        .param("studentId", "student_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.contextId", is("context_001")))
                .andExpect(jsonPath("$.result.sdkMode", is("LEARNING")))
                .andExpect(jsonPath("$.result.contextJson", is("{\"taskVersion\":1}")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<RuntimeContextQueryRequest> captor =
                ArgumentCaptor.forClass(RuntimeContextQueryRequest.class);
        verify(runtimeContextService).getRuntimeContext(captor.capture());
        RuntimeContextQueryRequest request = captor.getValue();
        assertEquals("tenant_001", request.getTenantId());
        assertEquals("execution_001", request.getExecutionId());
        assertEquals("task_001", request.getTaskId());
        assertEquals("student_001", request.getStudentId());
    }

    /**
     * 校验缺少执行 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getRuntimeContextShouldRejectMissingExecutionId() throws Exception {
        mockMvc.perform(get("/api/v1/sdk/runtime-context")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)));
    }

    /**
     * 构造 SDK 运行上下文返回对象。
     *
     * @return SDK 运行上下文返回对象。
     */
    private RuntimeContextVO buildRuntimeContextVO() {
        RuntimeContextVO vo = new RuntimeContextVO();
        vo.setContextId("context_001");
        vo.setTenantId("tenant_001");
        vo.setExecutionId("execution_001");
        vo.setTaskId("task_001");
        vo.setStudentId("student_001");
        vo.setSdkMode("LEARNING");
        vo.setContextJson("{\"taskVersion\":1}");
        vo.setOverlayPolicyJson("{\"mask\":true}");
        vo.setTeachingPointSnapshotJson("[{\"id\":\"tp_001\"}]");
        vo.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        vo.setEvaluationSnapshotJson("[{\"id\":\"rule_001\"}]");
        vo.setArchiveStatus("NONE");
        return vo;
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
