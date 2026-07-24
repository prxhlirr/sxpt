package com.sxpt.module.evaluation;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.evaluation.dto.GenerateAutoEvaluationRequest;
import com.sxpt.module.evaluation.dto.ReviewEvaluationResultRequest;
import com.sxpt.module.evaluation.entity.EvaluationResult;
import com.sxpt.module.evaluation.mapper.EvaluationResultMapper;
import com.sxpt.module.evaluation.vo.EvaluationResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自动评分契约测试。
 *
 * 业务功能：
 * 1. 验证自动评分请求的参数约束稳定。
 * 2. 验证 EvaluationResult 实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 检查 DTO 必填字段。
 * 2. 使用反射检查表名、主键注解、Mapper 泛型和 VO 字段边界。
 */
class AutoEvaluationContractTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * 校验合法自动评分请求可以通过参数校验。
     */
    @Test
    void validGenerateAutoEvaluationRequestShouldPassValidation() {
        GenerateAutoEvaluationRequest request = buildValidRequest();

        Set<ConstraintViolation<GenerateAutoEvaluationRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少执行 ID 会被参数校验拦截。
     */
    @Test
    void generateAutoEvaluationRequestShouldRejectMissingExecutionId() {
        GenerateAutoEvaluationRequest request = buildValidRequest();
        request.setExecutionId(null);

        Set<ConstraintViolation<GenerateAutoEvaluationRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验合法教师复核请求可以通过参数校验。
     */
    @Test
    void validReviewEvaluationResultRequestShouldPassValidation() {
        ReviewEvaluationResultRequest request = buildValidReviewRequest();

        Set<ConstraintViolation<ReviewEvaluationResultRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少复核人会被参数校验拦截。
     */
    @Test
    void reviewEvaluationResultRequestShouldRejectMissingReviewer() {
        ReviewEvaluationResultRequest request = buildValidReviewRequest();
        request.setReviewedBy(null);

        Set<ConstraintViolation<ReviewEvaluationResultRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验返回对象不暴露逻辑删除字段。
     */
    @Test
    void evaluationResultVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> EvaluationResultVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体映射到 evaluation_result 表并声明主键。
     *
     * @throws NoSuchFieldException 字段不存在时由测试失败暴露。
     */
    @Test
    void evaluationResultEntityShouldMapEvaluationResultTable() throws NoSuchFieldException {
        TableName tableName = EvaluationResult.class.getAnnotation(TableName.class);
        Field idField = EvaluationResult.class.getDeclaredField("id");

        assertEquals("evaluation_result", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 EvaluationResult。
     */
    @Test
    void evaluationResultMapperShouldBindEvaluationResultEntity() {
        Type[] interfaces = EvaluationResultMapper.class.getGenericInterfaces();

        assertEquals(1, interfaces.length);
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];
        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(EvaluationResult.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造合法自动评分请求。
     *
     * @return 自动评分请求。
     */
    private GenerateAutoEvaluationRequest buildValidRequest() {
        GenerateAutoEvaluationRequest request = new GenerateAutoEvaluationRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setEvaluationRuleId("rule_001");
        return request;
    }

    /**
     * 构造合法教师复核请求。
     *
     * @return 教师复核请求。
     */
    private ReviewEvaluationResultRequest buildValidReviewRequest() {
        ReviewEvaluationResultRequest request = new ReviewEvaluationResultRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setEvaluationRuleId("rule_001");
        request.setManualScore(new BigDecimal("8.50"));
        request.setReviewedBy("teacher_001");
        return request;
    }
}
