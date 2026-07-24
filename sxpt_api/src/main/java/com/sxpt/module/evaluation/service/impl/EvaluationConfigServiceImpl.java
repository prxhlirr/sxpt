package com.sxpt.module.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import com.sxpt.module.evaluation.mapper.EvaluationItemMapper;
import com.sxpt.module.evaluation.mapper.EvaluationRuleMapper;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评分配置服务实现。
 *
 * 业务功能：
 * 1. 保存评价规则和评分项，形成后续自动评分的配置基础。
 * 2. 按任务、教学点和评价规则查询配置，支撑评分配置页面和评分执行加载。
 *
 * 关键流程：
 * 1. 创建评价规则时补齐版本、通用状态和软删除字段。
 * 2. 创建评分项时补齐是否必得、通用状态和软删除字段。
 */
@Service
@Profile("!test")
public class EvaluationConfigServiceImpl implements EvaluationConfigService {

    private static final long DEFAULT_VERSION_NO = 1L;

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final EvaluationRuleMapper evaluationRuleMapper;

    private final EvaluationItemMapper evaluationItemMapper;

    public EvaluationConfigServiceImpl(EvaluationRuleMapper evaluationRuleMapper,
                                       EvaluationItemMapper evaluationItemMapper) {
        this.evaluationRuleMapper = evaluationRuleMapper;
        this.evaluationItemMapper = evaluationItemMapper;
    }

    /**
     * 创建评价规则。
     *
     * @param evaluationRule 评价规则实体。
     * @return 已保存的评价规则。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationRule createEvaluationRule(EvaluationRule evaluationRule) {
        validateRuleCreateFields(evaluationRule);
        fillRuleCreateDefaults(evaluationRule);
        evaluationRuleMapper.insert(evaluationRule);
        return evaluationRule;
    }

    /**
     * 查询评价规则。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 评价规则列表。
     */
    @Override
    public List<EvaluationRule> listRules(String tenantId, String taskId, String teachingPointId) {
        requireText(tenantId);
        QueryWrapper<EvaluationRule> wrapper = new QueryWrapper<EvaluationRule>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE);
        if (StringUtils.hasText(taskId)) {
            wrapper.eq("task_id", taskId);
        }
        if (StringUtils.hasText(teachingPointId)) {
            wrapper.eq("teaching_point_id", teachingPointId);
        }
        return evaluationRuleMapper.selectList(wrapper.orderByAsc("rule_code", "version_no"));
    }

    /**
     * 创建评分项。
     *
     * @param evaluationItem 评分项实体。
     * @return 已保存的评分项。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationItem createEvaluationItem(EvaluationItem evaluationItem) {
        validateItemCreateFields(evaluationItem);
        fillItemCreateDefaults(evaluationItem);
        evaluationItemMapper.insert(evaluationItem);
        return evaluationItem;
    }

    /**
     * 按评价规则查询评分项。
     *
     * @param tenantId 租户 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @return 评分项列表。
     */
    @Override
    public List<EvaluationItem> listItemsByRule(String tenantId, String evaluationRuleId) {
        requireText(tenantId);
        requireText(evaluationRuleId);
        return evaluationItemMapper.selectList(new QueryWrapper<EvaluationItem>()
                .eq("tenant_id", tenantId)
                .eq("evaluation_rule_id", evaluationRuleId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("item_code"));
    }

    /**
     * 校验创建评价规则所需的最小字段。
     *
     * @param evaluationRule 评价规则实体。
     */
    private void validateRuleCreateFields(EvaluationRule evaluationRule) {
        if (evaluationRule == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(evaluationRule.getId());
        requireText(evaluationRule.getTenantId());
        requireText(evaluationRule.getRuleCode());
        requireText(evaluationRule.getRuleName());
        requirePositive(evaluationRule.getTotalScore());
    }

    /**
     * 校验创建评分项所需的最小字段。
     *
     * @param evaluationItem 评分项实体。
     */
    private void validateItemCreateFields(EvaluationItem evaluationItem) {
        if (evaluationItem == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(evaluationItem.getId());
        requireText(evaluationItem.getTenantId());
        requireText(evaluationItem.getEvaluationRuleId());
        requireText(evaluationItem.getItemCode());
        requireText(evaluationItem.getItemName());
        requireText(evaluationItem.getItemType());
        requirePositive(evaluationItem.getScore());
        requireText(evaluationItem.getAssertionType());
        requireText(evaluationItem.getAssertionConfigJson());
        requireText(evaluationItem.getFailPolicy());
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验分值为正数。
     *
     * @param value 待校验分值。
     */
    private void requirePositive(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 补齐创建评价规则时的默认字段。
     *
     * @param evaluationRule 评价规则实体。
     */
    private void fillRuleCreateDefaults(EvaluationRule evaluationRule) {
        LocalDateTime now = LocalDateTime.now();
        if (evaluationRule.getVersionNo() == null) {
            evaluationRule.setVersionNo(DEFAULT_VERSION_NO);
        }
        if (evaluationRule.getCreateTime() == null) {
            evaluationRule.setCreateTime(now);
        }
        if (evaluationRule.getUpdateTime() == null) {
            evaluationRule.setUpdateTime(now);
        }
        if (!StringUtils.hasText(evaluationRule.getStatus())) {
            evaluationRule.setStatus(DEFAULT_STATUS);
        }
        if (evaluationRule.getDeleted() == null) {
            evaluationRule.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 补齐创建评分项时的默认字段。
     *
     * @param evaluationItem 评分项实体。
     */
    private void fillItemCreateDefaults(EvaluationItem evaluationItem) {
        LocalDateTime now = LocalDateTime.now();
        if (evaluationItem.getCreateTime() == null) {
            evaluationItem.setCreateTime(now);
        }
        if (evaluationItem.getUpdateTime() == null) {
            evaluationItem.setUpdateTime(now);
        }
        if (evaluationItem.getRequired() == null) {
            evaluationItem.setRequired(Boolean.TRUE);
        }
        if (!StringUtils.hasText(evaluationItem.getStatus())) {
            evaluationItem.setStatus(DEFAULT_STATUS);
        }
        if (evaluationItem.getDeleted() == null) {
            evaluationItem.setDeleted(Boolean.FALSE);
        }
    }
}

