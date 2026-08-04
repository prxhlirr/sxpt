package com.sxpt.module.practice;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.service.PracticeAttemptService;
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
 * 练习次数接口测试。
 *
 * 业务功能：
 * 1. 验证开始练习、完成练习和查询练习次数接口遵循统一响应结构。
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
        "sxpt.practice.attempt-controller.enabled=true",
        "sxpt.capture.event-controller.enabled=false",
        "sxpt.capture.resource-snapshot-controller.enabled=false",
        "sxpt.capture.action-draft-controller.enabled=false"
})
class PracticeAttemptControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private PracticeAttemptService practiceAttemptService;

    /**
     * 校验开始练习成功返回练习次数。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void startShouldReturnPracticeAttemptVo() throws Exception {
        PracticeAttempt saved = buildRunningAttempt();
        when(practiceAttemptService.startAttempt(any(PracticeAttempt.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/practice/attempts/start")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"studentId\":\"student_001\",\"classId\":\"class_001\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"dataInstanceId\":\"data_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("attempt_001")))
                .andExpect(jsonPath("$.result.attemptNo", is(1)))
                .andExpect(jsonPath("$.result.attemptStatus", is("RUNNING")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<PracticeAttempt> captor = ArgumentCaptor.forClass(PracticeAttempt.class);
        verify(practiceAttemptService).startAttempt(captor.capture());
        PracticeAttempt requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("execution_001", requestEntity.getExecutionId());
        assertEquals("student_001", requestEntity.getStudentId());
        assertEquals("task_001", requestEntity.getTaskId());
    }

    /**
     * 校验完成练习成功返回练习统计。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void finishShouldReturnCompletedPracticeAttemptVo() throws Exception {
        PracticeAttempt saved = buildCompletedAttempt();
        when(practiceAttemptService.finishAttempt(any(PracticeAttempt.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/practice/attempts/finish")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"attempt_001\",\"attemptStatus\":\"COMPLETED\",\"endTime\":\"2026-07-16T14:15:30\",\"score\":85.50,\"maxScore\":100.00,\"passFlag\":true,\"errorCount\":2,\"hintCount\":1,\"rollbackCount\":0,\"updateBy\":\"student_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("attempt_001")))
                .andExpect(jsonPath("$.result.attemptStatus", is("COMPLETED")))
                .andExpect(jsonPath("$.result.durationSeconds", is(930)))
                .andExpect(jsonPath("$.result.errorCount", is(2)))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<PracticeAttempt> captor = ArgumentCaptor.forClass(PracticeAttempt.class);
        verify(practiceAttemptService).finishAttempt(captor.capture());
        PracticeAttempt requestEntity = captor.getValue();
        assertEquals("attempt_001", requestEntity.getId());
        assertEquals("COMPLETED", requestEntity.getAttemptStatus());
        assertEquals(Long.valueOf(2), requestEntity.getErrorCount());
    }

    /**
     * 校验按学生和任务查询练习次数成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listByStudentAndTaskShouldReturnPracticeAttemptVos() throws Exception {
        when(practiceAttemptService.listByStudentAndTask("tenant_001", "student_001", "task_001"))
                .thenReturn(Collections.singletonList(buildCompletedAttempt()));

        mockMvc.perform(get("/api/v1/practice/attempts")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("studentId", "student_001")
                        .param("taskId", "task_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("attempt_001")))
                .andExpect(jsonPath("$.result[0].studentId", is("student_001")))
                .andExpect(jsonPath("$.result[0].attemptStatus", is("COMPLETED")));

        verify(practiceAttemptService).listByStudentAndTask("tenant_001", "student_001", "task_001");
    }

    /**
     * 校验缺少学生 ID 时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void startShouldRejectMissingStudentId() throws Exception {
        mockMvc.perform(post("/api/v1/practice/attempts/start")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"executionId\":\"execution_001\",\"taskId\":\"task_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 构造运行中的练习次数。
     *
     * @return 练习次数实体。
     */
    private PracticeAttempt buildRunningAttempt() {
        PracticeAttempt attempt = new PracticeAttempt();
        attempt.setId("attempt_001");
        attempt.setTenantId("tenant_001");
        attempt.setExecutionId("execution_001");
        attempt.setStudentId("student_001");
        attempt.setClassId("class_001");
        attempt.setTaskId("task_001");
        attempt.setTeachingPointId("tp_001");
        attempt.setDataInstanceId("data_001");
        attempt.setAttemptNo(1L);
        attempt.setStartTime(LocalDateTime.of(2026, 7, 16, 14, 0));
        attempt.setAttemptStatus("RUNNING");
        attempt.setErrorCount(0L);
        attempt.setHintCount(0L);
        attempt.setRollbackCount(0L);
        attempt.setStatus("ACTIVE");
        attempt.setCreateTime(LocalDateTime.now());
        return attempt;
    }

    /**
     * 构造已完成的练习次数。
     *
     * @return 练习次数实体。
     */
    private PracticeAttempt buildCompletedAttempt() {
        PracticeAttempt attempt = buildRunningAttempt();
        attempt.setEndTime(LocalDateTime.of(2026, 7, 16, 14, 15, 30));
        attempt.setDurationSeconds(930L);
        attempt.setAttemptStatus("COMPLETED");
        attempt.setScore(new BigDecimal("85.50"));
        attempt.setMaxScore(new BigDecimal("100.00"));
        attempt.setPassFlag(Boolean.TRUE);
        attempt.setErrorCount(2L);
        attempt.setHintCount(1L);
        attempt.setRollbackCount(0L);
        return attempt;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private String bearerToken() {
        return "Bearer " + jwtService.generateToken(
                "student_001",
                "student",
                "tenant_001",
                Collections.singletonList("STUDENT")
        );
    }
}
