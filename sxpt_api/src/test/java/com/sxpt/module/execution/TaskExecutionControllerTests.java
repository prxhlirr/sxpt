package com.sxpt.module.execution;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.AuthLoginService;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.service.TaskExecutionService;
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

import java.math.BigDecimal;
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
 * 学生任务执行主入口接口测试。
 *
 * 业务功能：
 * 1. 验证开始、提交和查询接口遵循统一响应结构。
 * 2. 验证接口不返回 deleted、executionIdentityJson 等内部字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "sxpt.execution.task-execution-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class TaskExecutionControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TaskExecutionService taskExecutionService;

    @MockBean
    private AuthLoginService authLoginService;

    /**
     * 验证开始执行成功返回学生任务执行主记录。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void startShouldReturnTaskExecutionVo() throws Exception {
        TaskExecution saved = buildRunningExecution();
        when(taskExecutionService.startExecution(any(TaskExecution.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/student/task-executions/start")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"taskId\":\"task_001\",\"studentId\":\"forged_student\",\"connectorSystemId\":\"connector_001\",\"executionMode\":\"PRACTICE\",\"sdkMode\":\"PRACTICE\",\"executionIdentityJson\":\"{\\\"identityMode\\\":\\\"STUDENT\\\"}\",\"createBy\":\"forged_operator\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("execution_001")))
                .andExpect(jsonPath("$.result.executionStatus", is("RUNNING")))
                .andExpect(jsonPath("$.result.executionIdentityStatus", is("READY")))
                .andExpect(jsonPath("$.result.executionIdentityJson").doesNotExist())
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TaskExecution> captor = ArgumentCaptor.forClass(TaskExecution.class);
        verify(taskExecutionService).startExecution(captor.capture());
        TaskExecution requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("task_001", requestEntity.getTaskId());
        assertEquals("student_001", requestEntity.getStudentId());
        assertEquals("student_001", requestEntity.getCreateBy());
        assertEquals("student_001", requestEntity.getUpdateBy());
        assertEquals("PRACTICE", requestEntity.getExecutionMode());
    }

    /**
     * 验证缺少学生 ID 时仍可开始执行，因为学生身份来自 JWT 当前用户上下文。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void startShouldUseCurrentUserWhenStudentIdMissing() throws Exception {
        when(taskExecutionService.startExecution(any(TaskExecution.class))).thenReturn(buildRunningExecution());

        mockMvc.perform(post("/api/v1/student/task-executions/start")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"taskId\":\"task_001\",\"connectorSystemId\":\"connector_001\",\"executionMode\":\"PRACTICE\",\"sdkMode\":\"PRACTICE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        ArgumentCaptor<TaskExecution> captor = ArgumentCaptor.forClass(TaskExecution.class);
        verify(taskExecutionService).startExecution(captor.capture());
        assertEquals("student_001", captor.getValue().getStudentId());
    }

    /**
     * 验证提交执行成功返回 SUBMITTED 状态。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void submitShouldReturnSubmittedTaskExecutionVo() throws Exception {
        TaskExecution saved = buildSubmittedExecution();
        when(taskExecutionService.submitExecution("tenant_001", "execution_001", "student_001"))
                .thenReturn(saved);

        mockMvc.perform(post("/api/v1/student/task-executions/submit")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"operatorId\":\"forged_operator\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("execution_001")))
                .andExpect(jsonPath("$.result.executionStatus", is("SUBMITTED")))
                .andExpect(jsonPath("$.result.score", is(85.5)));

        verify(taskExecutionService).submitExecution("tenant_001", "execution_001", "student_001");
    }

    /**
     * 验证查询单次执行成功返回主记录。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getExecutionShouldReturnTaskExecutionVo() throws Exception {
        TaskExecution saved = buildRunningExecution();
        when(taskExecutionService.getExecution("tenant_001", "execution_001")).thenReturn(saved);

        mockMvc.perform(get("/api/v1/student/task-executions/execution_001")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("execution_001")))
                .andExpect(jsonPath("$.result.studentId", is("student_001")));

        verify(taskExecutionService).getExecution("tenant_001", "execution_001");
    }

    /**
     * 验证按学生和任务查询执行历史成功返回列表。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByStudentAndTaskShouldReturnTaskExecutionVos() throws Exception {
        when(taskExecutionService.listByStudentAndTask("tenant_001", "student_001", "task_001"))
                .thenReturn(Collections.singletonList(buildRunningExecution()));

        mockMvc.perform(get("/api/v1/student/task-executions")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("studentId", "forged_student")
                        .param("taskId", "task_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("execution_001")))
                .andExpect(jsonPath("$.result[0].executionStatus", is("RUNNING")));

        verify(taskExecutionService).listByStudentAndTask("tenant_001", "student_001", "task_001");
    }

    /**
     * 构造运行中的学生任务执行。
     *
     * @return 学生任务执行实体。
     */
    private TaskExecution buildRunningExecution() {
        TaskExecution execution = new TaskExecution();
        execution.setId("execution_001");
        execution.setTenantId("tenant_001");
        execution.setTaskId("task_001");
        execution.setStudentId("student_001");
        execution.setConnectorSystemId("connector_001");
        execution.setExecutionMode("PRACTICE");
        execution.setSdkMode("PRACTICE");
        execution.setExecutionIdentityStatus("READY");
        execution.setStartTime(LocalDateTime.of(2026, 7, 21, 9, 0));
        execution.setExecutionStatus("RUNNING");
        execution.setStatus("ACTIVE");
        execution.setCreateTime(LocalDateTime.now());
        return execution;
    }

    /**
     * 构造已提交的学生任务执行。
     *
     * @return 学生任务执行实体。
     */
    private TaskExecution buildSubmittedExecution() {
        TaskExecution execution = buildRunningExecution();
        execution.setEndTime(LocalDateTime.of(2026, 7, 21, 9, 20));
        execution.setExecutionStatus("SUBMITTED");
        execution.setScore(new BigDecimal("85.50"));
        execution.setResultSummary("自动评分处理中。");
        return execution;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("student_001", "student001");
    }
}
