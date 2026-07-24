package com.sxpt.module.course;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.course.dto.CreateCourseRequest;
import com.sxpt.module.course.entity.Course;
import com.sxpt.module.course.mapper.CourseMapper;
import com.sxpt.module.course.vo.CourseVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 课程模块契约测试。
 *
 * 业务功能：
 * 1. 验证创建课程请求 DTO 的必填和长度约束。
 * 2. 验证 Course 实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、Mapper 泛型和 VO 字段边界。
 */
class CourseContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建课程请求可以通过参数校验。
     */
    @Test
    void validCreateCourseRequestShouldPassValidation() {
        Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(buildValidRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少课程编码时参数校验失败。
     */
    @Test
    void createCourseRequestShouldRejectMissingCourseCode() {
        CreateCourseRequest request = buildValidRequest();
        request.setCourseCode(" ");

        Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验课程说明超长时参数校验失败。
     */
    @Test
    void createCourseRequestShouldRejectTooLongDescription() {
        CreateCourseRequest request = buildValidRequest();
        request.setDescription(repeat("a", 4097));

        Set<ConstraintViolation<CreateCourseRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验课程 VO 不暴露内部软删除字段。
     */
    @Test
    void courseVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> CourseVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void courseEntityShouldMapCourseTable() throws NoSuchFieldException {
        TableName tableName = Course.class.getAnnotation(TableName.class);
        Field idField = Course.class.getDeclaredField("id");

        assertEquals("course", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 Course。
     */
    @Test
    void courseMapperShouldBindCourseEntity() {
        Type[] interfaces = CourseMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(Course.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造最小有效创建课程请求。
     *
     * @return 创建课程请求。
     */
    private CreateCourseRequest buildValidRequest() {
        CreateCourseRequest request = new CreateCourseRequest();
        request.setTenantId("tenant_001");
        request.setCourseCode("COURSE_DIGITAL_TWIN");
        request.setCourseName("业务数字孪生实训");
        request.setTargetOrgId("org_001");
        request.setDescription("课程说明");
        request.setCreateBy("teacher_001");
        return request;
    }

    /**
     * 构造指定长度字符串。
     *
     * @param value 重复字符。
     * @param count 重复次数。
     * @return 指定长度字符串。
     */
    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
