package com.sxpt.module.practice;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.practice.dto.GeneratePracticeScoreSummaryRequest;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import com.sxpt.module.practice.mapper.PracticeScoreSummaryMapper;
import com.sxpt.module.practice.vo.PracticeScoreSummaryVO;
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
 * 练习过程分汇总契约测试。
 *
 * 业务功能：
 * 1. 验证过程分汇总生成请求的参数约束稳定。
 * 2. 验证 PracticeScoreSummary 实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 检查 DTO 必填字段。
 * 2. 使用反射检查表名、主键注解、Mapper 泛型和 VO 字段边界。
 */
class PracticeScoreSummaryContractTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * 校验合法汇总生成请求可以通过参数校验。
     */
    @Test
    void validGeneratePracticeScoreSummaryRequestShouldPassValidation() {
        GeneratePracticeScoreSummaryRequest request = buildValidRequest();

        Set<ConstraintViolation<GeneratePracticeScoreSummaryRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少学生 ID 会被参数校验拦截。
     */
    @Test
    void generatePracticeScoreSummaryRequestShouldRejectMissingStudentId() {
        GeneratePracticeScoreSummaryRequest request = buildValidRequest();
        request.setStudentId(null);

        Set<ConstraintViolation<GeneratePracticeScoreSummaryRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验返回对象不暴露逻辑删除字段。
     */
    @Test
    void practiceScoreSummaryVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> PracticeScoreSummaryVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体映射到 practice_score_summary 表并声明主键。
     *
     * @throws NoSuchFieldException 字段不存在时由测试失败暴露。
     */
    @Test
    void practiceScoreSummaryEntityShouldMapPracticeScoreSummaryTable() throws NoSuchFieldException {
        TableName tableName = PracticeScoreSummary.class.getAnnotation(TableName.class);
        Field idField = PracticeScoreSummary.class.getDeclaredField("id");

        assertEquals("practice_score_summary", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定 PracticeScoreSummary。
     */
    @Test
    void practiceScoreSummaryMapperShouldBindPracticeScoreSummaryEntity() {
        Type[] interfaces = PracticeScoreSummaryMapper.class.getGenericInterfaces();

        assertEquals(1, interfaces.length);
        ParameterizedType baseMapperType = (ParameterizedType) interfaces[0];
        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(PracticeScoreSummary.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造合法汇总生成请求。
     *
     * @return 汇总生成请求。
     */
    private GeneratePracticeScoreSummaryRequest buildValidRequest() {
        GeneratePracticeScoreSummaryRequest request = new GeneratePracticeScoreSummaryRequest();
        request.setTenantId("tenant_001");
        request.setStudentId("student_001");
        request.setTaskId("task_001");
        return request;
    }
}
