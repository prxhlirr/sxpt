package com.sxpt.module.evaluation;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.AuthLoginService;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.evaluation.entity.EvaluationResult;
import com.sxpt.module.evaluation.service.AutoEvaluationService;
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
 * 自动评分接口测试。
 *
 * 业务功能：
 * 1. 验证自动评分生成和查询接口遵循统一响应结构。
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
        "sxpt.evaluation.auto-evaluation-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class AutoEvaluationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AutoEvaluationService autoEvaluationService;

    @MockBean
    private AuthLoginService authLoginService;

    /**
     * 验证生成自动评分成功返回评分结果。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void generateShouldReturnEvaluationResultVo() throws Exception {
        when(autoEvaluationService.generateAutoEvaluation(any(EvaluationResult.class))).thenReturn(buildResult());

        mockMvc.perform(post("/api/v1/evaluation/results/auto-generate")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"evaluationRuleId\":\"rule_001\",\"createBy\":\"system\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("result_001")))
                .andExpect(jsonPath("$.result.autoScore", is(10.00)))
                .andExpect(jsonPath("$.result.evaluationStatus", is("AUTO_EVALUATED")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<EvaluationResult> captor = ArgumentCaptor.forClass(EvaluationResult.class);
        verify(autoEvaluationService).generateAutoEvaluation(captor.capture());
        EvaluationResult requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("execution_001", requestEntity.getExecutionId());
        assertEquals("rule_001", requestEntity.getEvaluationRuleId());
    }

    /**
     * 验证查询自动评分结果成功返回评分结果。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getResultShouldReturnEvaluationResultVo() throws Exception {
        when(autoEvaluationService.getEvaluationResult("tenant_001", "execution_001", "rule_001"))
                .thenReturn(buildResult());

        mockMvc.perform(get("/api/v1/evaluation/results")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("executionId", "execution_001")
                        .param("evaluationRuleId", "rule_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("result_001")))
                .andExpect(jsonPath("$.result.finalScore", is(10.00)));

        verify(autoEvaluationService).getEvaluationResult("tenant_001", "execution_001", "rule_001");
    }

    /**
     * 验证缺少执行 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    /**
     * 验证提交教师复核成功返回已复核评分结果。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void reviewShouldReturnReviewedEvaluationResultVo() throws Exception {
        EvaluationResult reviewed = buildResult();
        reviewed.setManualScore(new BigDecimal("8.50"));
        reviewed.setFinalScore(new BigDecimal("8.50"));
        reviewed.setReviewedBy("teacher_001");
        reviewed.setReviewedTime(LocalDateTime.now());
        reviewed.setEvaluationStatus("REVIEWED");
        when(autoEvaluationService.reviewEvaluationResult(any(EvaluationResult.class))).thenReturn(reviewed);

        mockMvc.perform(post("/api/v1/evaluation/results/review")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\","
                                + "\"evaluationRuleId\":\"rule_001\",\"manualScore\":8.50,"
                                + "\"reviewedBy\":\"forged_teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.manualScore", is(8.50)))
                .andExpect(jsonPath("$.result.finalScore", is(8.50)))
                .andExpect(jsonPath("$.result.reviewedBy", is("teacher_001")))
                .andExpect(jsonPath("$.result.evaluationStatus", is("REVIEWED")));

        ArgumentCaptor<EvaluationResult> captor = ArgumentCaptor.forClass(EvaluationResult.class);
        verify(autoEvaluationService).reviewEvaluationResult(captor.capture());
        EvaluationResult requestEntity = captor.getValue();
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("execution_001", requestEntity.getExecutionId());
        assertEquals("rule_001", requestEntity.getEvaluationRuleId());
        assertEquals(new BigDecimal("8.50"), requestEntity.getManualScore());
        assertEquals("teacher_001", requestEntity.getReviewedBy());
    }

    @Test
    void generateShouldRejectMissingExecutionId() throws Exception {
        mockMvc.perform(post("/api/v1/evaluation/results/auto-generate")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"evaluationRuleId\":\"rule_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造评分结果。
     *
     * @return 评分结果实体。
     */
    private EvaluationResult buildResult() {
        EvaluationResult result = new EvaluationResult();
        result.setId("result_001");
        result.setTenantId("tenant_001");
        result.setExecutionId("execution_001");
        result.setEvaluationRuleId("rule_001");
        result.setAutoScore(new BigDecimal("10.00"));
        result.setFinalScore(new BigDecimal("10.00"));
        result.setEvaluationSummary("自动评分完成");
        result.setEvidenceJson("{\"items\":[]}");
        result.setArchiveStatus("NONE");
        result.setEvaluationStatus("AUTO_EVALUATED");
        result.setStatus("ACTIVE");
        result.setCreateTime(LocalDateTime.now());
        return result;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("teacher_001", "teacher001");
    }
}
