package com.sxpt.module.practice;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import com.sxpt.module.practice.service.PracticeScoreSummaryService;
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
 * 练习过程分汇总接口测试。
 *
 * 业务功能：
 * 1. 验证过程分汇总生成和查询接口遵循统一响应结构。
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
        "sxpt.practice.score-summary-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class PracticeScoreSummaryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private PracticeScoreSummaryService practiceScoreSummaryService;

    /**
     * 验证生成过程分汇总成功返回 VO。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void generateShouldReturnPracticeScoreSummaryVo() throws Exception {
        PracticeScoreSummary saved = buildSummary();
        when(practiceScoreSummaryService.generateSummary(any(PracticeScoreSummary.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/practice/score-summaries/generate")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"studentId\":\"student_001\",\"classId\":\"class_001\",\"courseId\":\"course_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"createBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("summary_001")))
                .andExpect(jsonPath("$.result.practiceCount", is(2)))
                .andExpect(jsonPath("$.result.completeCount", is(1)))
                .andExpect(jsonPath("$.result.finalPracticeScore", is(82.00)))
                .andExpect(jsonPath("$.result.scorePolicy", is("WEIGHTED")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<PracticeScoreSummary> captor = ArgumentCaptor.forClass(PracticeScoreSummary.class);
        verify(practiceScoreSummaryService).generateSummary(captor.capture());
        PracticeScoreSummary requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("student_001", requestEntity.getStudentId());
        assertEquals("task_001", requestEntity.getTaskId());
    }

    /**
     * 验证查询过程分汇总成功返回 VO。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void getSummaryShouldReturnPracticeScoreSummaryVo() throws Exception {
        when(practiceScoreSummaryService.getSummary("tenant_001", "student_001", "task_001", "tp_001"))
                .thenReturn(buildSummary());

        mockMvc.perform(get("/api/v1/practice/score-summaries")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("studentId", "student_001")
                        .param("taskId", "task_001")
                        .param("teachingPointId", "tp_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("summary_001")))
                .andExpect(jsonPath("$.result.completionRate", is(0.5000)))
                .andExpect(jsonPath("$.result.finalPracticeScore", is(82.00)));

        verify(practiceScoreSummaryService).getSummary("tenant_001", "student_001", "task_001", "tp_001");
    }

    /**
     * 验证缺少学生 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void generateShouldRejectMissingStudentId() throws Exception {
        mockMvc.perform(post("/api/v1/practice/score-summaries/generate")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"taskId\":\"task_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造过程分汇总。
     *
     * @return 练习过程分汇总实体。
     */
    private PracticeScoreSummary buildSummary() {
        PracticeScoreSummary summary = new PracticeScoreSummary();
        summary.setId("summary_001");
        summary.setTenantId("tenant_001");
        summary.setStudentId("student_001");
        summary.setClassId("class_001");
        summary.setCourseId("course_001");
        summary.setTaskId("task_001");
        summary.setTeachingPointId("tp_001");
        summary.setPracticeCount(2L);
        summary.setCompleteCount(1L);
        summary.setBestScore(new BigDecimal("90.00"));
        summary.setLastScore(new BigDecimal("90.00"));
        summary.setAvgScore(new BigDecimal("80.00"));
        summary.setCompletionRate(new BigDecimal("0.5000"));
        summary.setFinalPracticeScore(new BigDecimal("82.00"));
        summary.setScorePolicy("WEIGHTED");
        summary.setWeakStepJson("[{\"taskStepId\":\"step_001\"}]");
        summary.setSummaryTime(LocalDateTime.now());
        summary.setStatus("ACTIVE");
        summary.setCreateTime(LocalDateTime.now());
        return summary;
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
