package com.sxpt.module.practice;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.service.PracticeStepResultService;
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
 * 练习步骤结果接口测试。
 *
 * 业务功能：
 * 1. 验证步骤结果上报和查询接口遵循统一响应结构。
 * 2. 验证接口不返回 deleted 等内部持久化字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "sxpt.practice.step-result-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class PracticeStepResultControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private PracticeStepResultService practiceStepResultService;

    /**
     * 验证步骤结果上报成功返回步骤结果 VO。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldReturnPracticeStepResultVo() throws Exception {
        PracticeStepResult saved = buildStepResult();
        when(practiceStepResultService.reportStepResult(any(PracticeStepResult.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/practice/step-results/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"attemptId\":\"attempt_001\",\"executionId\":\"execution_001\",\"studentId\":\"student_001\",\"taskId\":\"task_001\",\"taskStepId\":\"step_001\",\"stepCode\":\"SUBMIT_FORM\",\"sequenceNo\":1,\"resultStatus\":\"PASSED\",\"score\":10.00,\"maxScore\":10.00,\"passFlag\":true,\"startTime\":\"2026-07-16T14:00:00\",\"endTime\":\"2026-07-16T14:15:30\",\"errorCount\":0,\"hintCount\":1,\"retryCount\":0,\"evidenceJson\":\"{\\\"traceId\\\":\\\"trace_001\\\"}\",\"feedback\":\"步骤完成\",\"createBy\":\"student_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("step_result_001")))
                .andExpect(jsonPath("$.result.attemptId", is("attempt_001")))
                .andExpect(jsonPath("$.result.taskStepId", is("step_001")))
                .andExpect(jsonPath("$.result.resultStatus", is("PASSED")))
                .andExpect(jsonPath("$.result.durationSeconds", is(930)))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<PracticeStepResult> captor = ArgumentCaptor.forClass(PracticeStepResult.class);
        verify(practiceStepResultService).reportStepResult(captor.capture());
        PracticeStepResult requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("attempt_001", requestEntity.getAttemptId());
        assertEquals("step_001", requestEntity.getTaskStepId());
    }

    /**
     * 验证按练习次数查询步骤结果成功返回列表。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByAttemptShouldReturnPracticeStepResultVos() throws Exception {
        when(practiceStepResultService.listByAttempt("tenant_001", "attempt_001"))
                .thenReturn(Collections.singletonList(buildStepResult()));

        mockMvc.perform(get("/api/v1/practice/step-results")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("attemptId", "attempt_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("step_result_001")))
                .andExpect(jsonPath("$.result[0].sequenceNo", is(1)))
                .andExpect(jsonPath("$.result[0].resultStatus", is("PASSED")));

        verify(practiceStepResultService).listByAttempt("tenant_001", "attempt_001");
    }

    /**
     * 验证缺少任务步骤 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reportShouldRejectMissingTaskStepId() throws Exception {
        mockMvc.perform(post("/api/v1/practice/step-results/report")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"attemptId\":\"attempt_001\",\"sequenceNo\":1,\"resultStatus\":\"PASSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造练习步骤结果实体。
     *
     * @return 练习步骤结果实体。
     */
    private PracticeStepResult buildStepResult() {
        PracticeStepResult stepResult = new PracticeStepResult();
        stepResult.setId("step_result_001");
        stepResult.setTenantId("tenant_001");
        stepResult.setAttemptId("attempt_001");
        stepResult.setExecutionId("execution_001");
        stepResult.setStudentId("student_001");
        stepResult.setTaskId("task_001");
        stepResult.setTaskStepId("step_001");
        stepResult.setStepCode("SUBMIT_FORM");
        stepResult.setSequenceNo(1L);
        stepResult.setResultStatus("PASSED");
        stepResult.setScore(new BigDecimal("10.00"));
        stepResult.setMaxScore(new BigDecimal("10.00"));
        stepResult.setPassFlag(Boolean.TRUE);
        stepResult.setStartTime(LocalDateTime.of(2026, 7, 16, 14, 0));
        stepResult.setEndTime(LocalDateTime.of(2026, 7, 16, 14, 15, 30));
        stepResult.setDurationSeconds(930L);
        stepResult.setErrorCount(0L);
        stepResult.setHintCount(1L);
        stepResult.setRetryCount(0L);
        stepResult.setEvidenceJson("{\"traceId\":\"trace_001\"}");
        stepResult.setFeedback("步骤完成");
        stepResult.setStatus("ACTIVE");
        stepResult.setCreateTime(LocalDateTime.now());
        return stepResult;
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
