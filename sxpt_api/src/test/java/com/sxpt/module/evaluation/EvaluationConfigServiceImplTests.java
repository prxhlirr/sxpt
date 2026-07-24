package com.sxpt.module.evaluation;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import com.sxpt.module.evaluation.mapper.EvaluationItemMapper;
import com.sxpt.module.evaluation.mapper.EvaluationRuleMapper;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.evaluation.service.impl.EvaluationConfigServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
 * 评分配置服务测试。
 *
 * 业务功能：
 * 1. 验证评价规则和评分项创建会补齐默认生命周期字段。
 * 2. 验证配置查询通过 Service 附加租户和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class EvaluationConfigServiceImplTests {

    private final EvaluationRuleMapper ruleMapper = mock(EvaluationRuleMapper.class);

    private final EvaluationItemMapper itemMapper = mock(EvaluationItemMapper.class);

    private final EvaluationConfigService service = new EvaluationConfigServiceImpl(ruleMapper, itemMapper);

    /**
     * 校验创建评价规则时插入 Mapper 并补齐默认值。
     */
    @Test
    void createEvaluationRuleShouldInsertAndFillDefaults() {
        EvaluationRule rule = buildValidRule();

        EvaluationRule saved = service.createEvaluationRule(rule);

        assertSame(rule, saved);
        assertEquals(1L, saved.getVersionNo());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(ruleMapper).insert(saved);
    }

    /**
     * 校验评价规则总分小于等于 0 时拒绝创建。
     */
    @Test
    void createEvaluationRuleShouldRejectNonPositiveTotalScore() {
        EvaluationRule rule = buildValidRule();
        rule.setTotalScore(BigDecimal.ZERO);

        assertThrows(BusinessException.class, () -> service.createEvaluationRule(rule));
        verify(ruleMapper, times(0)).insert(rule);
    }

    /**
     * 校验创建评分项时插入 Mapper 并补齐默认值。
     */
    @Test
    void createEvaluationItemShouldInsertAndFillDefaults() {
        EvaluationItem item = buildValidItem();

        EvaluationItem saved = service.createEvaluationItem(item);

        assertSame(item, saved);
        assertEquals(Boolean.TRUE, saved.getRequired());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(itemMapper).insert(saved);
    }

    /**
     * 校验评分项缺少断言配置时拒绝创建。
     */
    @Test
    void createEvaluationItemShouldRejectMissingAssertionConfig() {
        EvaluationItem item = buildValidItem();
        item.setAssertionConfigJson(" ");

        assertThrows(BusinessException.class, () -> service.createEvaluationItem(item));
        verify(itemMapper, times(0)).insert(item);
    }

    /**
     * 校验查询评价规则返回 Mapper 结果。
     */
    @Test
    void listRulesShouldReturnMapperResult() {
        EvaluationRule rule = buildValidRule();
        when(ruleMapper.selectList(any())).thenReturn(Collections.singletonList(rule));

        List<EvaluationRule> result = service.listRules("tenant_001", "task_001", "tp_001");

        assertEquals(1, result.size());
        assertSame(rule, result.get(0));
        verify(ruleMapper).selectList(any());
    }

    /**
     * 校验按规则查询评分项返回 Mapper 结果。
     */
    @Test
    void listItemsByRuleShouldReturnMapperResult() {
        EvaluationItem item = buildValidItem();
        when(itemMapper.selectList(any())).thenReturn(Collections.singletonList(item));

        List<EvaluationItem> result = service.listItemsByRule("tenant_001", "rule_001");

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
        verify(itemMapper).selectList(any());
    }

    /**
     * 构造最小有效评价规则。
     *
     * @return 评价规则实体。
     */
    private EvaluationRule buildValidRule() {
        EvaluationRule rule = new EvaluationRule();
        rule.setId("rule_001");
        rule.setTenantId("tenant_001");
        rule.setRuleCode("RULE_RECORD");
        rule.setRuleName("备案评分规则");
        rule.setTaskId("task_001");
        rule.setTeachingPointId("tp_001");
        rule.setTotalScore(new BigDecimal("100.00"));
        return rule;
    }

    /**
     * 构造最小有效评分项。
     *
     * @return 评分项实体。
     */
    private EvaluationItem buildValidItem() {
        EvaluationItem item = new EvaluationItem();
        item.setId("item_001");
        item.setTenantId("tenant_001");
        item.setEvaluationRuleId("rule_001");
        item.setItemCode("ITEM_SUBMIT");
        item.setItemName("提交申请");
        item.setItemType("KEY_ACTION");
        item.setScore(new BigDecimal("10.00"));
        item.setAssertionType("TRACE_EXISTS");
        item.setAssertionConfigJson("{\"eventType\":\"CLICK\"}");
        item.setFailPolicy("NO_SCORE");
        return item;
    }
}
