package com.sxpt.module.evaluation;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.evaluation.dto.CreateEvaluationItemRequest;
import com.sxpt.module.evaluation.dto.CreateEvaluationRuleRequest;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import com.sxpt.module.evaluation.mapper.EvaluationItemMapper;
import com.sxpt.module.evaluation.mapper.EvaluationRuleMapper;
import com.sxpt.module.evaluation.vo.EvaluationItemVO;
import com.sxpt.module.evaluation.vo.EvaluationRuleVO;
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
 * 评分配置模块契约测试。
 *
 * 业务功能：
 * 1. 验证评分配置 DTO 的必填、长度和分值约束。
 * 2. 验证实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、Mapper 泛型和 VO 字段边界。
 */
class EvaluationConfigContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效评价规则请求可以通过参数校验。
     */
    @Test
    void validCreateEvaluationRuleRequestShouldPassValidation() {
        Set<ConstraintViolation<CreateEvaluationRuleRequest>> violations = validator.validate(buildValidRuleRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验评价规则总分为 0 时参数校验失败。
     */
    @Test
    void createEvaluationRuleRequestShouldRejectZeroTotalScore() {
        CreateEvaluationRuleRequest request = buildValidRuleRequest();
        request.setTotalScore(BigDecimal.ZERO);

        Set<ConstraintViolation<CreateEvaluationRuleRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验最小有效评分项请求可以通过参数校验。
     */
    @Test
    void validCreateEvaluationItemRequestShouldPassValidation() {
        Set<ConstraintViolation<CreateEvaluationItemRequest>> violations = validator.validate(buildValidItemRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验断言配置超长时参数校验失败。
     */
    @Test
    void createEvaluationItemRequestShouldRejectTooLongAssertionConfig() {
        CreateEvaluationItemRequest request = buildValidItemRequest();
        request.setAssertionConfigJson(repeat("a", 32769));

        Set<ConstraintViolation<CreateEvaluationItemRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验 VO 不暴露内部软删除字段。
     */
    @Test
    void evaluationVosShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> EvaluationRuleVO.class.getDeclaredField("deleted"));
        assertThrows(NoSuchFieldException.class, () -> EvaluationItemVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void evaluationEntitiesShouldMapTables() throws NoSuchFieldException {
        TableName ruleTableName = EvaluationRule.class.getAnnotation(TableName.class);
        Field ruleIdField = EvaluationRule.class.getDeclaredField("id");
        TableName itemTableName = EvaluationItem.class.getAnnotation(TableName.class);
        Field itemIdField = EvaluationItem.class.getDeclaredField("id");

        assertEquals("evaluation_rule", ruleTableName.value());
        assertTrue(ruleIdField.isAnnotationPresent(TableId.class));
        assertEquals("evaluation_item", itemTableName.value());
        assertTrue(itemIdField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定正确实体。
     */
    @Test
    void evaluationMappersShouldBindEntities() {
        Type[] ruleInterfaces = EvaluationRuleMapper.class.getGenericInterfaces();
        ParameterizedType ruleBaseMapperType = (ParameterizedType) ruleInterfaces[0];
        Type[] itemInterfaces = EvaluationItemMapper.class.getGenericInterfaces();
        ParameterizedType itemBaseMapperType = (ParameterizedType) itemInterfaces[0];

        assertEquals(BaseMapper.class, ruleBaseMapperType.getRawType());
        assertEquals(EvaluationRule.class, ruleBaseMapperType.getActualTypeArguments()[0]);
        assertEquals(BaseMapper.class, itemBaseMapperType.getRawType());
        assertEquals(EvaluationItem.class, itemBaseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造最小有效创建评价规则请求。
     *
     * @return 创建评价规则请求。
     */
    private CreateEvaluationRuleRequest buildValidRuleRequest() {
        CreateEvaluationRuleRequest request = new CreateEvaluationRuleRequest();
        request.setTenantId("tenant_001");
        request.setRuleCode("RULE_RECORD");
        request.setRuleName("备案评分规则");
        request.setTaskId("task_001");
        request.setTeachingPointId("tp_001");
        request.setTotalScore(new BigDecimal("100.00"));
        request.setDescription("备案评分规则说明");
        request.setCreateBy("teacher_001");
        return request;
    }

    /**
     * 构造最小有效创建评分项请求。
     *
     * @return 创建评分项请求。
     */
    private CreateEvaluationItemRequest buildValidItemRequest() {
        CreateEvaluationItemRequest request = new CreateEvaluationItemRequest();
        request.setTenantId("tenant_001");
        request.setEvaluationRuleId("rule_001");
        request.setTeachingPointId("tp_001");
        request.setItemCode("ITEM_SUBMIT");
        request.setItemName("提交申请");
        request.setItemType("KEY_ACTION");
        request.setRelatedResourceId("res_001");
        request.setRelatedTaskStepId("step_001");
        request.setScore(new BigDecimal("10.00"));
        request.setRequired(Boolean.TRUE);
        request.setAssertionType("TRACE_EXISTS");
        request.setAssertionConfigJson("{\"eventType\":\"CLICK\"}");
        request.setFailPolicy("NO_SCORE");
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
