package com.sxpt.module.execution;

import com.sxpt.module.execution.dto.ExamContextQueryRequest;
import com.sxpt.module.execution.vo.ExamContextVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 考试模式上下文契约测试。
 *
 * 业务功能：
 * 1. 验证考试模式查询 DTO 的必填约束稳定。
 * 2. 验证考试模式 VO 不暴露内部软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界。
 */
class ExamContextContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效查询请求可以通过参数校验。
     */
    @Test
    void validQueryRequestShouldPassValidation() {
        Set<ConstraintViolation<ExamContextQueryRequest>> violations = validator.validate(buildValidRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少执行 ID 时参数校验失败。
     */
    @Test
    void queryRequestShouldRejectMissingExecutionId() {
        ExamContextQueryRequest request = buildValidRequest();
        request.setExecutionId(" ");

        Set<ConstraintViolation<ExamContextQueryRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验 VO 不暴露内部软删除字段。
     */
    @Test
    void examContextVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> ExamContextVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效查询请求。
     *
     * @return 考试模式上下文查询请求。
     */
    private ExamContextQueryRequest buildValidRequest() {
        ExamContextQueryRequest request = new ExamContextQueryRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        return request;
    }
}
