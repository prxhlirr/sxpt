package com.sxpt.module.practice;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.practice.dto.ReportPracticeStepResultRequest;
import com.sxpt.module.practice.entity.PracticeStepResult;
import com.sxpt.module.practice.mapper.PracticeStepResultMapper;
import com.sxpt.module.practice.vo.PracticeStepResultVO;
import org.junit.jupiter.api.BeforeEach;
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
 * 练习步骤结果契约测试。
 *
 * 业务功能：
 * 1. 验证步骤结果上报请求的参数约束稳定。
 * 2. 验证 PracticeStepResult 实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 检查 DTO 必填字段。
 * 2. 使用反射检查表名、主键注解、Mapper 泛型和 VO 字段边界。
 */
class PracticeStepResultContractTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * 校验合法步骤结果上报请求可以通过参数校验。
     */
    @Test
    void validReportPracticeStepResultRequestShouldPassValidation() {
        ReportPracticeStepResultRequest request = buildValidRequest();

        Set<ConstraintViolation<ReportPracticeStepResultRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少任务步骤 ID 会被参数校验拦截。
     */
    @Test
    void reportPracticeStepResultRequestShouldRejectMissingTaskStepId() {
        ReportPracticeStepResultRequest request = buildValidRequest();
        request.setTaskStepId(null);

        Set<ConstraintViolation<ReportPracticeStepResultRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验步骤序号必须从 1 开始。
     */
    @Test
    void reportPracticeStepResultRequestShouldRejectInvalidSequenceNo() {
        ReportPracticeStepResultRequest request = buildValidRequest();
        request.setSequenceNo(0L);

        Set<ConstraintViolation<ReportPracticeStepResultRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验返回对象不暴露逻辑删除字段。
     */
    @Test
    void practiceStepResultVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> PracticeStepResultVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体映射到 practice_step_result 表并声明主键。
     *
     * @throws NoSuchFieldException 字段不存在时由测试失败暴露。
     */
    @Test
    void practiceStepResultEntityShouldMapPracticeStepResultTable() throws NoSuchFieldException {
        TableName tableName = PracticeStepResult.class.getAnnotation(TableName.class);
        Field idField = PracticeStepResult.class.getDeclaredField("id");

        assertEquals("practice_step_result", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 PracticeStepResult。
     */
    @Test
    void practiceStepResultMapperShouldBindPracticeStepResultEntity() {
        Type[] interfaces = PracticeStepResultMapper.class.getGenericInterfaces();

        assertEquals(1, interfaces.length);
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];
        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(PracticeStepResult.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造合法步骤结果上报请求。
     *
     * @return 步骤结果上报请求。
     */
    private ReportPracticeStepResultRequest buildValidRequest() {
        ReportPracticeStepResultRequest request = new ReportPracticeStepResultRequest();
        request.setTenantId("tenant_001");
        request.setAttemptId("attempt_001");
        request.setTaskStepId("step_001");
        request.setSequenceNo(1L);
        request.setResultStatus("PASSED");
        return request;
    }
}
