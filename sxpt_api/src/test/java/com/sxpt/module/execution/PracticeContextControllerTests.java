package com.sxpt.module.execution;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.execution.dto.PracticeContextQueryRequest;
import com.sxpt.module.execution.service.PracticeContextService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.vo.PracticeContextVO;
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
 * 练习模式上下文接口测试。
 *
 * 业务功能：
 * 1. 验证 SDK 可通过 HTTP 接口读取练习模式上下文。
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
        "sxpt.execution.practice-context-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class PracticeContextControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private PracticeContextService practiceContextService;

    @MockBean
    private RuntimeContextService runtimeContextService;

    /**
     * 校验成功获取练习模式上下文。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getPracticeContextShouldReturnContext() throws Exception {
        PracticeContextVO vo = buildPracticeContextVO();
        when(practiceContextService.getPracticeContext(any(PracticeContextQueryRequest.class))).thenReturn(vo);

        mockMvc.perform(get("/api/v1/sdk/practice/context")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("executionId", "execution_001")
                        .param("taskId", "task_001")
                        .param("studentId", "student_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.contextId", is("context_001")))
                .andExpect(jsonPath("$.result.sdkMode", is("PRACTICE")))
                .andExpect(jsonPath("$.result.practiceContextJson", is("{\"steps\":[{\"name\":\"练习步骤\"}]}")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<PracticeContextQueryRequest> captor =
                ArgumentCaptor.forClass(PracticeContextQueryRequest.class);
        verify(practiceContextService).getPracticeContext(captor.capture());
        PracticeContextQueryRequest request = captor.getValue();
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
    void getPracticeContextShouldRejectMissingExecutionId() throws Exception {
        mockMvc.perform(get("/api/v1/sdk/practice/context")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)));
    }

    /**
     * 构造练习模式上下文返回对象。
     *
     * @return 练习模式上下文返回对象。
     */
    private PracticeContextVO buildPracticeContextVO() {
        PracticeContextVO vo = new PracticeContextVO();
        vo.setContextId("context_001");
        vo.setTenantId("tenant_001");
        vo.setExecutionId("execution_001");
        vo.setTaskId("task_001");
        vo.setStudentId("student_001");
        vo.setSdkMode("PRACTICE");
        vo.setPracticeContextJson("{\"steps\":[{\"name\":\"练习步骤\"}]}");
        vo.setOverlayPolicyJson("{\"retry\":true}");
        vo.setTeachingPointSnapshotJson("[{\"id\":\"tp_001\"}]");
        vo.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        vo.setHintPolicyJson("[{\"id\":\"tp_001\"}]");
        vo.setRetryPolicyJson("{\"retry\":true}");
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
