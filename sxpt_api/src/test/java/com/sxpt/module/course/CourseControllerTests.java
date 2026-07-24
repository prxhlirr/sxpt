package com.sxpt.module.course;

import com.sxpt.SxptApiApplication;
import com.sxpt.common.security.JwtService;
import com.sxpt.module.course.entity.Course;
import com.sxpt.module.course.service.CourseService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 课程接口测试。
 *
 * 业务功能：
 * 1. 验证课程创建接口遵循统一响应结构。
 * 2. 验证课程查询接口可用，且不返回内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 MockMvc 调用 HTTP 接口。
 * 2. 使用 MockBean 替代 Service，避免接口测试依赖真实数据库。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "sxpt.course.controller.enabled=true")
class CourseControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private CourseService courseService;

    /**
     * 校验创建课程成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldReturnCourseVo() throws Exception {
        Course saved = buildSavedCourse();
        when(courseService.createCourse(any(Course.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/courses/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"courseCode\":\"COURSE_DIGITAL_TWIN\",\"courseName\":\"业务数字孪生实训\",\"targetOrgId\":\"org_001\",\"description\":\"课程说明\",\"createBy\":\"teacher_001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.id", is("course_001")))
                .andExpect(jsonPath("$.result.courseCode", is("COURSE_DIGITAL_TWIN")))
                .andExpect(jsonPath("$.result.courseStatus", is("PUBLISHED")))
                .andExpect(jsonPath("$.result.deleted").doesNotExist());

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseService).createCourse(captor.capture());
        Course requestEntity = captor.getValue();
        org.junit.jupiter.api.Assertions.assertNotNull(requestEntity.getId());
        org.junit.jupiter.api.Assertions.assertEquals("tenant_001", requestEntity.getTenantId());
        org.junit.jupiter.api.Assertions.assertEquals("COURSE_DIGITAL_TWIN", requestEntity.getCourseCode());
        org.junit.jupiter.api.Assertions.assertEquals("org_001", requestEntity.getTargetOrgId());
    }

    /**
     * 校验缺少课程编码时返回参数错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void createShouldRejectMissingCourseCode() throws Exception {
        mockMvc.perform(post("/api/v1/courses/create")
                        .header("Authorization", bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"tenant_001\",\"courseName\":\"业务数字孪生实训\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * 校验查询课程成功返回统一响应。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void listShouldReturnCourseVos() throws Exception {
        Course saved = buildSavedCourse();
        when(courseService.listCourses("tenant_001", "org_001")).thenReturn(Collections.singletonList(saved));

        mockMvc.perform(get("/api/v1/courses")
                        .header("Authorization", bearerToken())
                        .param("tenantId", "tenant_001")
                        .param("targetOrgId", "org_001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result[0].id", is("course_001")))
                .andExpect(jsonPath("$.result[0].courseCode", is("COURSE_DIGITAL_TWIN")));

        verify(courseService).listCourses("tenant_001", "org_001");
    }

    /**
     * 构造 Service 返回的已保存课程。
     *
     * @return 课程实体。
     */
    private Course buildSavedCourse() {
        Course course = new Course();
        course.setId("course_001");
        course.setTenantId("tenant_001");
        course.setCourseCode("COURSE_DIGITAL_TWIN");
        course.setCourseName("业务数字孪生实训");
        course.setVersionNo(1L);
        course.setTargetOrgId("org_001");
        course.setDescription("课程说明");
        course.setCourseStatus("PUBLISHED");
        course.setStatus("ACTIVE");
        course.setCreateTime(LocalDateTime.now());
        course.setUpdateTime(LocalDateTime.now());
        return course;
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
