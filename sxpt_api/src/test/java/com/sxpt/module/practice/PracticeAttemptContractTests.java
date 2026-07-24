package com.sxpt.module.practice;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.practice.dto.FinishPracticeAttemptRequest;
import com.sxpt.module.practice.dto.StartPracticeAttemptRequest;
import com.sxpt.module.practice.entity.PracticeAttempt;
import com.sxpt.module.practice.mapper.PracticeAttemptMapper;
import com.sxpt.module.practice.vo.PracticeAttemptVO;
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
 * 练习次数契约测试。
 *
 * 业务功能：
 * 1. 验证练习次数 DTO、VO、实体和 Mapper 的接口契约稳定。
 * 2. 防止软删除字段泄露给 SDK 或教师端。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、主键和 VO 字段边界。
 */
class PracticeAttemptContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效开始练习请求可以通过参数校验。
     */
    @Test
    void validStartPracticeAttemptRequestShouldPassValidation() {
        Set<ConstraintViolation<StartPracticeAttemptRequest>> violations =
                validator.validate(buildValidStartRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少学生 ID 时开始练习参数校验失败。
     */
    @Test
    void startPracticeAttemptRequestShouldRejectMissingStudentId() {
        StartPracticeAttemptRequest request = buildValidStartRequest();
        request.setStudentId(" ");

        Set<ConstraintViolation<StartPracticeAttemptRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验最小有效完成练习请求可以通过参数校验。
     */
    @Test
    void validFinishPracticeAttemptRequestShouldPassValidation() {
        FinishPracticeAttemptRequest request = new FinishPracticeAttemptRequest();
        request.setId("attempt_001");
        request.setAttemptStatus("COMPLETED");

        Set<ConstraintViolation<FinishPracticeAttemptRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验 VO 不暴露内部软删除字段。
     */
    @Test
    void practiceAttemptVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> PracticeAttemptVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void practiceAttemptEntityShouldMapPracticeAttemptTable() throws NoSuchFieldException {
        TableName tableName = PracticeAttempt.class.getAnnotation(TableName.class);
        Field idField = PracticeAttempt.class.getDeclaredField("id");

        assertEquals("practice_attempt", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 PracticeAttempt。
     */
    @Test
    void practiceAttemptMapperShouldBindPracticeAttemptEntity() {
        Type[] interfaces = PracticeAttemptMapper.class.getGenericInterfaces();
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(PracticeAttempt.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造最小有效开始练习请求。
     *
     * @return 开始练习请求。
     */
    private StartPracticeAttemptRequest buildValidStartRequest() {
        StartPracticeAttemptRequest request = new StartPracticeAttemptRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setStudentId("student_001");
        request.setTaskId("task_001");
        return request;
    }
}
