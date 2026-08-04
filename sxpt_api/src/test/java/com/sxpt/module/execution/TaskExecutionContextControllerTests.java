package com.sxpt.module.execution;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.service.TaskExecutionContextService;
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
 * 任务执行上下文快照接口测试。
 *
 * 业务功能：
 * 1. 验证上下文快照创建和查询接口遵循统一响应结构。
 * 2. 验证 Controller 层能够提前拦截缺少上下文 JSON 的无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.execution.context-controller.enabled=true")
class TaskExecutionContextControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TaskExecutionContextService taskExecutionContextService;

    /**
     * 校验创建上下文快照成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createContextShouldReturnContextVo() throws Exception {
        TaskExecutionContext saved = buildSavedContext();
        when(taskExecutionContextService.createContext(any(TaskExecutionContext.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/tasks/execution-contexts/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"taskId\":\"task_001\",\"studentId\":\"student_001\",\"sdkMode\":\"LEARNING\",\"contextJson\":\"{\\\"taskVersion\\\":1}\",\"overlayPolicyJson\":\"{\\\"mask\\\":true}\",\"teachingPointSnapshotJson\":\"[{\\\"id\\\":\\\"tp_001\\\"}]\",\"resourceSnapshotJson\":\"[{\\\"id\\\":\\\"resource_001\\\"}]\",\"evaluationSnapshotJson\":\"[{\\\"id\\\":\\\"rule_001\\\"}]\",\"createBy\":\"student_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("context_001")))
                .andExpect(jsonPath("$.result.executionId", is("execution_001")))
                .andExpect(jsonPath("$.result.archiveStatus", is("NONE")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TaskExecutionContext> captor = ArgumentCaptor.forClass(TaskExecutionContext.class);
        verify(taskExecutionContextService).createContext(captor.capture());
        TaskExecutionContext requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("execution_001", requestEntity.getExecutionId());
        assertEquals("student_001", requestEntity.getStudentId());
    }

    /**
     * 校验缺少上下文 JSON 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createContextShouldRejectMissingContextJson() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/execution-contexts/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"taskId\":\"task_001\",\"studentId\":\"student_001\",\"sdkMode\":\"LEARNING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验查询上下文快照成功返回列表。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listContextsShouldReturnContextVos() throws Exception {
        TaskExecutionContext saved = buildSavedContext();
        when(taskExecutionContextService.listContexts("tenant_001", "execution_001", "task_001", "student_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/tasks/execution-contexts")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("executionId", "execution_001")
                        .param("taskId", "task_001")
                        .param("studentId", "student_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("context_001")))
                .andExpect(jsonPath("$.result[0].sdkMode", is("LEARNING")));

        verify(taskExecutionContextService).listContexts("tenant_001", "execution_001", "task_001", "student_001");
    }

    /**
     * 构造 Service 返回的已保存上下文快照。
     *
     * @return 任务执行上下文快照实体。
     */
    private TaskExecutionContext buildSavedContext() {
        TaskExecutionContext context = new TaskExecutionContext();
        context.setId("context_001");
        context.setTenantId("tenant_001");
        context.setExecutionId("execution_001");
        context.setTaskId("task_001");
        context.setStudentId("student_001");
        context.setSdkMode("LEARNING");
        context.setContextJson("{\"taskVersion\":1}");
        context.setOverlayPolicyJson("{\"mask\":true}");
        context.setTeachingPointSnapshotJson("[{\"id\":\"tp_001\"}]");
        context.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        context.setEvaluationSnapshotJson("[{\"id\":\"rule_001\"}]");
        context.setArchiveStatus("NONE");
        context.setStatus("ACTIVE");
        context.setCreateTime(LocalDateTime.now());
        context.setUpdateTime(LocalDateTime.now());
        return context;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken(
                "admin_001",
                "admin",
                "tenant_001",
                Collections.singletonList("ADMIN")
        );
    }
}
