package com.sxpt.module.course;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.course.entity.Course;
import com.sxpt.module.course.mapper.CourseMapper;
import com.sxpt.module.course.service.CourseService;
import com.sxpt.module.course.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 课程服务测试。
 *
 * 业务功能：
 * 1. 验证课程创建会补齐版本号、课程状态和通用生命周期字段。
 * 2. 验证课程查询通过 Service 附加租户和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class CourseServiceImplTests {

    private final CourseMapper mapper = mock(CourseMapper.class);

    private final CourseService service = new CourseServiceImpl(mapper);

    /**
     * 校验创建课程时插入 Mapper 并补齐默认值。
     */
    @Test
    void createCourseShouldInsertAndFillDefaults() {
        Course course = buildValidCourse();

        Course saved = service.createCourse(course);

        assertSame(course, saved);
        assertEquals(1L, saved.getVersionNo());
        assertEquals("PUBLISHED", saved.getCourseStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少课程编码时拒绝创建，避免任务无法稳定引用课程。
     */
    @Test
    void createCourseShouldRejectMissingCourseCode() {
        Course course = buildValidCourse();
        course.setCourseCode(" ");

        assertThrows(BusinessException.class, () -> service.createCourse(course));
        verify(mapper, times(0)).insert(course);
    }

    /**
     * 校验查询课程时返回 Mapper 结果。
     */
    @Test
    void listCoursesShouldReturnMapperResult() {
        Course course = buildValidCourse();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(course));

        List<Course> result = service.listCourses("tenant_001", "org_001");

        assertEquals(1, result.size());
        assertSame(course, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效课程。
     *
     * @return 课程实体。
     */
    private Course buildValidCourse() {
        Course course = new Course();
        course.setId("course_001");
        course.setTenantId("tenant_001");
        course.setCourseCode("COURSE_DIGITAL_TWIN");
        course.setCourseName("业务数字孪生实训");
        course.setTargetOrgId("org_001");
        return course;
    }
}
