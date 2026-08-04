package com.sxpt.module.course;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.AuthLoginService;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.course.entity.Task;
import com.sxpt.module.course.entity.TaskTeachingPoint;
import com.sxpt.module.course.service.TaskPublishService;
import com.sxpt.module.user.service.SystemConfigService;
import com.sxpt.module.user.vo.RuntimeUserContextVO;
import org.junit.jupiter.api.BeforeEach;
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
 * 任务发布接口测试。
 *
 * 业务功能：
 * 1. 验证教学任务创建、查询和任务教学点关联接口遵循统一响应结构。
 * 2. 验证 Controller 层能够提前拦截缺少任务目标或排序号的无效请求。
 *
 * 关键流程：
 * 1. 使用 MockMvc 发起 HTTP 请求，覆盖真实路由、参数绑定和统一异常响应。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖数据库和 MyBatis 环境。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.task.publish-controller.enabled=true")
class TaskPublishControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TaskPublishService taskPublishService;

    @MockBean
    private BusinessModuleProcessChainService businessModuleProcessChainService;

    @MockBean
    private AuthLoginService authLoginService;

    @MockBean
    private SystemConfigService systemConfigService;

    @BeforeEach
    void setUpCurrentUserRuntimeContext() {
        when(systemConfigService.getRuntimeContextForUser("teacher_001")).thenReturn(buildRuntimeContext());
    }

    /**
     * 校验创建任务成功返回统一响应，并向 Service 传入完整任务实体。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createTaskShouldReturnTaskVo() throws Exception {
        Task saved = buildSavedTask();
        when(taskPublishService.createTask(any(Task.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/tasks/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"forged_tenant\",\"courseId\":\"course_001\",\"publishOrgId\":\"org_001\",\"taskCode\":\"TASK_LEARN_RECORD\",\"taskName\":\"备案学习任务\",\"taskType\":\"LEARNING\",\"taskGoal\":\"完成备案流程学习\",\"taskDescription\":\"任务说明\",\"timeLimitMinutes\":60,\"overlayPolicyJson\":\"{\\\"mask\\\":true}\",\"createBy\":\"forged_teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("task_001")))
                .andExpect(jsonPath("$.result.taskCode", is("TASK_LEARN_RECORD")))
                .andExpect(jsonPath("$.result.taskStatus", is("DRAFT")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskPublishService).createTask(captor.capture());
        Task requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("course_001", requestEntity.getCourseId());
        assertEquals("org_001", requestEntity.getPublishOrgId());
        assertEquals("完成备案流程学习", requestEntity.getTaskGoal());
        assertEquals("teacher_001", requestEntity.getCreateBy());
        assertEquals("teacher_001", requestEntity.getUpdateBy());
    }

    /**
     * 校验缺少任务目标时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createTaskShouldRejectMissingTaskGoal() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"forged_tenant\",\"courseId\":\"course_001\",\"publishOrgId\":\"org_001\",\"taskCode\":\"TASK_LEARN_RECORD\",\"taskName\":\"备案学习任务\",\"taskType\":\"LEARNING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验查询任务成功返回任务列表。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listTasksShouldReturnTaskVos() throws Exception {
        Task saved = buildSavedTask();
        when(taskPublishService.listTasks("tenant_001", "course_001", "org_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/tasks")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "forged_tenant")
                        .param("courseId", "course_001")
                        .param("publishOrgId", "org_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("task_001")))
                .andExpect(jsonPath("$.result[0].taskCode", is("TASK_LEARN_RECORD")));

        verify(taskPublishService).listTasks("tenant_001", "course_001", "org_001");
    }

    /**
     * 校验显式发布任务教学点资产时返回已发布任务，并将发布参数完整传入 Service。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void publishTaskTeachingPointAssetsShouldReturnPublishedTaskVo() throws Exception {
        Task published = buildSavedTask();
        published.setTaskStatus("PUBLISHED");
        when(taskPublishService.publishTaskTeachingPointAssets(
                "tenant_001", "task_001", "tp_001", "rule_001", "teacher_001")).thenReturn(published);

        mockMvc.perform(post("/api/v1/tasks/teaching-points/publish")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "forged_tenant")
                        .param("taskId", "task_001")
                        .param("teachingPointId", "tp_001")
                        .param("evaluationRuleId", "rule_001")
                        .param("operatorId", "forged_teacher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("task_001")))
                .andExpect(jsonPath("$.result.taskStatus", is("PUBLISHED")));

        verify(taskPublishService).publishTaskTeachingPointAssets(
                "tenant_001", "task_001", "tp_001", "rule_001", "teacher_001");
    }

    /**
     * 校验创建任务教学点关联成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createTaskTeachingPointShouldReturnTaskTeachingPointVo() throws Exception {
        TaskTeachingPoint saved = buildSavedTaskTeachingPoint();
        when(taskPublishService.createTaskTeachingPoint(any(TaskTeachingPoint.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/tasks/teaching-points/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"forged_tenant\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\",\"requiredFlag\":true,\"sequenceNo\":1,\"createBy\":\"forged_teacher\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("task_point_001")))
                .andExpect(jsonPath("$.result.taskId", is("task_001")))
                .andExpect(jsonPath("$.result.teachingPointId", is("tp_001")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<TaskTeachingPoint> captor = ArgumentCaptor.forClass(TaskTeachingPoint.class);
        verify(taskPublishService).createTaskTeachingPoint(captor.capture());
        TaskTeachingPoint requestEntity = captor.getValue();
        assertNotNull(requestEntity.getId());
        assertEquals("tenant_001", requestEntity.getTenantId());
        assertEquals("task_001", requestEntity.getTaskId());
        assertEquals("tp_001", requestEntity.getTeachingPointId());
        assertEquals(1L, requestEntity.getSequenceNo());
        assertEquals("teacher_001", requestEntity.getCreateBy());
        assertEquals("teacher_001", requestEntity.getUpdateBy());
    }

    /**
     * 校验缺少排序号时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createTaskTeachingPointShouldRejectMissingSequenceNo() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/teaching-points/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"forged_tenant\",\"taskId\":\"task_001\",\"teachingPointId\":\"tp_001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验查询任务教学点关联成功返回列表。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listTaskTeachingPointsShouldReturnTaskTeachingPointVos() throws Exception {
        TaskTeachingPoint saved = buildSavedTaskTeachingPoint();
        when(taskPublishService.listTeachingPointsByTask("tenant_001", "task_001"))
                .thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/tasks/teaching-points")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "forged_tenant")
                        .param("taskId", "task_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("task_point_001")))
                .andExpect(jsonPath("$.result[0].sequenceNo", is(1)));

        verify(taskPublishService).listTeachingPointsByTask("tenant_001", "task_001");
    }

    /**
     * 构造 Service 返回的已保存任务。
     *
     * @return 教学任务实体。
     */
    private Task buildSavedTask() {
        Task task = new Task();
        task.setId("task_001");
        task.setTenantId("tenant_001");
        task.setCourseId("course_001");
        task.setPublishOrgId("org_001");
        task.setTaskCode("TASK_LEARN_RECORD");
        task.setTaskName("备案学习任务");
        task.setVersionNo(1L);
        task.setTaskType("LEARNING");
        task.setTaskGoal("完成备案流程学习");
        task.setTaskDescription("任务说明");
        task.setTimeLimitMinutes(60L);
        task.setOverlayPolicyJson("{\"mask\":true}");
        task.setTaskStatus("DRAFT");
        task.setStatus("ACTIVE");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        return task;
    }

    /**
     * 构造 Service 返回的已保存任务教学点关联。
     *
     * @return 任务教学点关联实体。
     */
    private TaskTeachingPoint buildSavedTaskTeachingPoint() {
        TaskTeachingPoint taskTeachingPoint = new TaskTeachingPoint();
        taskTeachingPoint.setId("task_point_001");
        taskTeachingPoint.setTenantId("tenant_001");
        taskTeachingPoint.setTaskId("task_001");
        taskTeachingPoint.setTeachingPointId("tp_001");
        taskTeachingPoint.setRequiredFlag(Boolean.TRUE);
        taskTeachingPoint.setSequenceNo(1L);
        taskTeachingPoint.setStatus("ACTIVE");
        taskTeachingPoint.setCreateTime(LocalDateTime.now());
        taskTeachingPoint.setUpdateTime(LocalDateTime.now());
        return taskTeachingPoint;
    }

    /**
     * 构造通过 JWT 拦截器所需的认证请求头。
     *
     * @return Bearer Token 请求头值。
     */
    private RuntimeUserContextVO buildRuntimeContext() {
        RuntimeUserContextVO context = new RuntimeUserContextVO();
        RuntimeUserContextVO.UserSummary user = new RuntimeUserContextVO.UserSummary();
        user.setUserId("teacher_001");
        user.setTenantId("tenant_001");
        user.setUsername("teacher001");
        user.setDisplayName("教师一");
        user.setUserType("TEACHER");
        user.setEmployeeNo("T001");
        context.setUser(user);
        context.setRoles(Collections.emptyList());
        context.setOrgs(Collections.emptyList());
        return context;
    }

    private String bearerToken() {
        return "Bearer " + jwtService.generateToken("teacher_001", "teacher001");
    }
}
