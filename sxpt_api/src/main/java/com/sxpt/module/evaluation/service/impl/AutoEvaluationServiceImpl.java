package com.sxpt.module.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.evaluation.assertion.AssertionDecision;
import com.sxpt.module.evaluation.assertion.EvaluationAssertionStrategy;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationResult;
import com.sxpt.module.evaluation.mapper.EvaluationResultMapper;
import com.sxpt.module.evaluation.service.AutoEvaluationService;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.execution.entity.ExecutionTrace;
import com.sxpt.module.execution.service.ExecutionTraceService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动评分服务实现。
 *
 * 业务功能：
 * 1. 基于评分项断言配置和执行轨迹计算自动分。
 * 2. 将每个评分项命中情况写入 evidenceJson，支撑教师后续复核。
 *
 * 关键流程：
 * 1. 加载评分项和执行轨迹。
 * 2. MVP 阶段先支持 TRACE_EXISTS，其它断言类型不自动给分，只记录未支持证据。
 * 3. 按 tenantId、executionId、evaluationRuleId 幂等写入 evaluation_result。
 */
@Service
@Profile("!test")
public class AutoEvaluationServiceImpl implements AutoEvaluationService {

    private static final String AUTO_EVALUATED = "AUTO_EVALUATED";

    private static final String REVIEWED = "REVIEWED";

    private static final String DEFAULT_ARCHIVE_STATUS = "NONE";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final EvaluationResultMapper evaluationResultMapper;

    private final EvaluationConfigService evaluationConfigService;

    private final ExecutionTraceService executionTraceService;

    private final List<EvaluationAssertionStrategy> assertionStrategies;

    private final ObjectMapper objectMapper;

    public AutoEvaluationServiceImpl(EvaluationResultMapper evaluationResultMapper,
                                     EvaluationConfigService evaluationConfigService,
                                     ExecutionTraceService executionTraceService,
                                     List<EvaluationAssertionStrategy> assertionStrategies,
                                     ObjectMapper objectMapper) {
        this.evaluationResultMapper = evaluationResultMapper;
        this.evaluationConfigService = evaluationConfigService;
        this.executionTraceService = executionTraceService;
        this.assertionStrategies = assertionStrategies;
        this.objectMapper = objectMapper;
    }

    /**
     * 生成或重算自动评分结果。
     *
     * @param result 评分结果范围。
     * @return 已保存的评分结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationResult generateAutoEvaluation(EvaluationResult result) {
        validateGenerateFields(result);
        List<EvaluationItem> items = evaluationConfigService.listItemsByRule(
                result.getTenantId(), result.getEvaluationRuleId());
        List<ExecutionTrace> traces = executionTraceService.listByExecution(
                result.getTenantId(), result.getExecutionId());
        EvaluationResult calculated = calculateResult(result, items, traces);
        EvaluationResult existed = findExistingResult(result);
        if (existed != null) {
            copyCalculatedFields(existed, calculated);
            evaluationResultMapper.updateById(existed);
            return existed;
        }
        fillCreateDefaults(calculated);
        evaluationResultMapper.insert(calculated);
        return calculated;
    }

    /**
     * 查询自动评分结果。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @return 评分结果。
     */
    @Override
    public EvaluationResult getEvaluationResult(String tenantId, String executionId, String evaluationRuleId) {
        requireText(tenantId);
        requireText(executionId);
        requireText(evaluationRuleId);
        EvaluationResult result = evaluationResultMapper.selectOne(resultScopeWrapper(
                tenantId, executionId, evaluationRuleId));
        if (result == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return result;
    }

    /**
     * 提交教师复核结果。
     *
     * @param result 复核范围、人工分和复核人。
     * @return 已更新的评分结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvaluationResult reviewEvaluationResult(EvaluationResult result) {
        validateReviewFields(result);
        EvaluationResult existed = getEvaluationResult(
                result.getTenantId(), result.getExecutionId(), result.getEvaluationRuleId());
        BigDecimal manualScore = result.getManualScore().setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now();
        existed.setManualScore(manualScore);
        existed.setFinalScore(manualScore);
        existed.setReviewedBy(result.getReviewedBy());
        existed.setReviewedTime(now);
        existed.setEvaluationStatus(REVIEWED);
        existed.setUpdateBy(result.getReviewedBy());
        existed.setUpdateTime(now);
        evaluationResultMapper.updateById(existed);
        return existed;
    }

    /**
     * 校验自动评分生成最小字段。
     *
     * @param result 评分结果范围。
     */
    private void validateGenerateFields(EvaluationResult result) {
        if (result == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(result.getId());
        requireText(result.getTenantId());
        requireText(result.getExecutionId());
        requireText(result.getEvaluationRuleId());
    }

    /**
     * 校验教师复核提交的最小字段。
     *
     * @param result 复核范围和人工分。
     */
    private void validateReviewFields(EvaluationResult result) {
        if (result == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(result.getTenantId());
        requireText(result.getExecutionId());
        requireText(result.getEvaluationRuleId());
        requireText(result.getReviewedBy());
        if (result.getManualScore() == null || BigDecimal.ZERO.compareTo(result.getManualScore()) > 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 计算自动评分结果。
     *
     * @param source 请求传入的评分范围。
     * @param items 评分项列表。
     * @param traces 执行轨迹列表。
     * @return 已计算但尚未持久化的评分结果。
     */
    private EvaluationResult calculateResult(EvaluationResult source, List<EvaluationItem> items,
                                             List<ExecutionTrace> traces) {
        BigDecimal autoScore = BigDecimal.ZERO;
        List<Map<String, Object>> evidenceItems = new ArrayList<>();
        for (EvaluationItem item : items) {
            AssertionDecision decision = evaluateItem(item, traces);
            if (decision.isMatched()) {
                autoScore = autoScore.add(defaultScore(item.getScore()));
            }
            evidenceItems.add(toEvidenceMap(item, decision));
        }
        EvaluationResult result = new EvaluationResult();
        result.setId(source.getId());
        result.setTenantId(source.getTenantId());
        result.setExecutionId(source.getExecutionId());
        result.setEvaluationRuleId(source.getEvaluationRuleId());
        result.setAutoScore(autoScore.setScale(2, RoundingMode.HALF_UP));
        result.setFinalScore(result.getAutoScore());
        result.setEvaluationSummary(summaryText(items.size(), evidenceItems, result.getAutoScore()));
        result.setEvidenceJson(toEvidenceJson(evidenceItems));
        result.setEvaluationStatus(AUTO_EVALUATED);
        result.setCreateBy(source.getCreateBy());
        result.setUpdateBy(source.getCreateBy());
        return result;
    }

    /**
     * 评价单个评分项。
     *
     * @param item 评分项。
     * @param traces 执行轨迹列表。
     * @return 评分项判定结果。
     */
    private AssertionDecision evaluateItem(EvaluationItem item, List<ExecutionTrace> traces) {
        Map<String, Object> config = parseAssertionConfig(item.getAssertionConfigJson());
        for (EvaluationAssertionStrategy strategy : assertionStrategies) {
            if (strategy.supports(item.getAssertionType())) {
                return strategy.evaluate(item, config, traces);
            }
        }
        return AssertionDecision.notMatched("UNSUPPORTED_ASSERTION");
    }

    /**
     * 解析断言配置 JSON。
     *
     * @param assertionConfigJson 断言配置 JSON。
     * @return 配置 Map。
     */
    private Map<String, Object> parseAssertionConfig(String assertionConfigJson) {
        if (!StringUtils.hasText(assertionConfigJson)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(assertionConfigJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 构造评分项证据摘要。
     *
     * @param item 评分项。
     * @param decision 判定结果。
     * @return 证据摘要。
     */
    private Map<String, Object> toEvidenceMap(EvaluationItem item, AssertionDecision decision) {
        Map<String, Object> evidence = new HashMap<>();
        evidence.put("itemId", item.getId());
        evidence.put("itemCode", item.getItemCode());
        evidence.put("assertionType", item.getAssertionType());
        evidence.put("matched", decision.isMatched());
        evidence.put("score", decision.isMatched() ? defaultScore(item.getScore()) : BigDecimal.ZERO);
        evidence.put("hitTraceId", decision.getHitTraceId());
        evidence.put("apiResourceId", decision.getApiResourceId());
        evidence.put("reason", decision.getReason());
        return evidence;
    }

    /**
     * 序列化评分证据摘要。
     *
     * @param evidenceItems 评分项证据列表。
     * @return 证据 JSON。
     */
    private String toEvidenceJson(List<Map<String, Object>> evidenceItems) {
        try {
            Map<String, Object> root = new HashMap<>();
            root.put("items", evidenceItems);
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 生成评价摘要文本。
     *
     * @param itemCount 评分项总数。
     * @param evidenceItems 证据列表。
     * @param autoScore 自动分。
     * @return 评价摘要。
     */
    private String summaryText(int itemCount, List<Map<String, Object>> evidenceItems, BigDecimal autoScore) {
        int matchedCount = 0;
        for (Map<String, Object> evidence : evidenceItems) {
            if (Boolean.TRUE.equals(evidence.get("matched"))) {
                matchedCount++;
            }
        }
        return "自动评分完成：命中 " + matchedCount + "/" + itemCount + " 个评分项，自动分 " + autoScore + "。";
    }

    /**
     * 查询已有评分结果。
     *
     * @param result 评分结果范围。
     * @return 已有评分结果；未命中时返回 null。
     */
    private EvaluationResult findExistingResult(EvaluationResult result) {
        return evaluationResultMapper.selectOne(resultScopeWrapper(
                result.getTenantId(), result.getExecutionId(), result.getEvaluationRuleId()));
    }

    /**
     * 构造评分结果唯一范围查询条件。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @return 查询条件。
     */
    private QueryWrapper<EvaluationResult> resultScopeWrapper(String tenantId, String executionId,
                                                              String evaluationRuleId) {
        return new QueryWrapper<EvaluationResult>()
                .eq("tenant_id", tenantId)
                .eq("execution_id", executionId)
                .eq("evaluation_rule_id", evaluationRuleId)
                .eq("deleted", Boolean.FALSE);
    }

    /**
     * 将计算结果复制到已有评分结果。
     *
     * @param target 已有评分结果。
     * @param source 新计算的评分结果。
     */
    private void copyCalculatedFields(EvaluationResult target, EvaluationResult source) {
        target.setAutoScore(source.getAutoScore());
        target.setFinalScore(source.getFinalScore());
        target.setEvaluationSummary(source.getEvaluationSummary());
        target.setEvidenceJson(source.getEvidenceJson());
        target.setEvaluationStatus(source.getEvaluationStatus());
        target.setUpdateBy(source.getUpdateBy());
        target.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 补齐新建评分结果默认字段。
     *
     * @param result 评分结果。
     */
    private void fillCreateDefaults(EvaluationResult result) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(result.getArchiveStatus())) {
            result.setArchiveStatus(DEFAULT_ARCHIVE_STATUS);
        }
        if (result.getCreateTime() == null) {
            result.setCreateTime(now);
        }
        if (result.getUpdateTime() == null) {
            result.setUpdateTime(now);
        }
        if (!StringUtils.hasText(result.getStatus())) {
            result.setStatus(DEFAULT_STATUS);
        }
        if (result.getDeleted() == null) {
            result.setDeleted(Boolean.FALSE);
        }
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
     * 将空分数归零。
     *
     * @param value 原始分数。
     * @return 非空分数。
     */
    private BigDecimal defaultScore(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}

