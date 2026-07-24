package com.sxpt.module.evaluation.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.evaluation.dto.CreateEvaluationItemRequest;
import com.sxpt.module.evaluation.dto.CreateEvaluationRuleRequest;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.evaluation.vo.EvaluationItemVO;
import com.sxpt.module.evaluation.vo.EvaluationRuleVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 评分配置接口。
 *
 * 业务功能：
 * 1. 提供评价规则和评分项配置入口。
 * 2. 提供按任务、教学点和评价规则查询评分配置的入口。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截缺少断言配置或分值的无效配置。
 * 2. 将 DTO 转换为实体并生成应用层主键。
 * 3. 调用 Service 写入评分配置，再转换为 VO 返回前端。
 */
@RestController
@RequestMapping("/api/v1/evaluation/config")
@ConditionalOnProperty(name = "sxpt.evaluation.config-controller.enabled", havingValue = "true", matchIfMissing = true)
public class EvaluationConfigController {

    private final EvaluationConfigService evaluationConfigService;

    public EvaluationConfigController(EvaluationConfigService evaluationConfigService) {
        this.evaluationConfigService = evaluationConfigService;
    }

    /**
     * 创建评价规则。
     *
     * @param request 创建评价规则请求。
     * @return 已保存的评价规则。
     */
    @PostMapping("/rules/create")
    public ApiResult<EvaluationRuleVO> createRule(@Valid @RequestBody CreateEvaluationRuleRequest request) {
        return ApiResult.success(toRuleVO(evaluationConfigService.createEvaluationRule(toRuleEntity(request))));
    }

    /**
     * 查询评价规则。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 评价规则列表。
     */
    @GetMapping("/rules")
    public ApiResult<List<EvaluationRuleVO>> listRules(@RequestParam String tenantId,
                                                       @RequestParam(required = false) String taskId,
                                                       @RequestParam(required = false) String teachingPointId) {
        return ApiResult.success(toRuleVOList(evaluationConfigService.listRules(tenantId, taskId, teachingPointId)));
    }

    /**
     * 创建评分项。
     *
     * @param request 创建评分项请求。
     * @return 已保存的评分项。
     */
    @PostMapping("/items/create")
    public ApiResult<EvaluationItemVO> createItem(@Valid @RequestBody CreateEvaluationItemRequest request) {
        return ApiResult.success(toItemVO(evaluationConfigService.createEvaluationItem(toItemEntity(request))));
    }

    /**
     * 按评价规则查询评分项。
     *
     * @param tenantId 租户 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @return 评分项列表。
     */
    @GetMapping("/items")
    public ApiResult<List<EvaluationItemVO>> listItems(@RequestParam String tenantId,
                                                       @RequestParam String evaluationRuleId) {
        return ApiResult.success(toItemVOList(evaluationConfigService.listItemsByRule(tenantId, evaluationRuleId)));
    }

    /**
     * 将创建评价规则请求转换为实体。
     *
     * @param request 创建评价规则请求。
     * @return 评价规则实体。
     */
    private EvaluationRule toRuleEntity(CreateEvaluationRuleRequest request) {
        EvaluationRule rule = new EvaluationRule();
        rule.setId(generateId());
        rule.setTenantId(request.getTenantId());
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setTaskId(request.getTaskId());
        rule.setTeachingPointId(request.getTeachingPointId());
        rule.setTotalScore(request.getTotalScore());
        rule.setDescription(request.getDescription());
        rule.setCreateBy(request.getCreateBy());
        rule.setUpdateBy(request.getCreateBy());
        return rule;
    }

    /**
     * 将创建评分项请求转换为实体。
     *
     * @param request 创建评分项请求。
     * @return 评分项实体。
     */
    private EvaluationItem toItemEntity(CreateEvaluationItemRequest request) {
        EvaluationItem item = new EvaluationItem();
        item.setId(generateId());
        item.setTenantId(request.getTenantId());
        item.setEvaluationRuleId(request.getEvaluationRuleId());
        item.setTeachingPointId(request.getTeachingPointId());
        item.setItemCode(request.getItemCode());
        item.setItemName(request.getItemName());
        item.setItemType(request.getItemType());
        item.setRelatedResourceId(request.getRelatedResourceId());
        item.setRelatedApiResourceId(request.getRelatedApiResourceId());
        item.setRelatedTaskStepId(request.getRelatedTaskStepId());
        item.setScore(request.getScore());
        item.setRequired(request.getRequired());
        item.setAssertionType(request.getAssertionType());
        item.setAssertionConfigJson(request.getAssertionConfigJson());
        item.setFailPolicy(request.getFailPolicy());
        item.setCreateBy(request.getCreateBy());
        item.setUpdateBy(request.getCreateBy());
        return item;
    }

    /**
     * 将评价规则实体列表转换为 VO 列表。
     *
     * @param rules 评价规则实体列表。
     * @return 评价规则 VO 列表。
     */
    private List<EvaluationRuleVO> toRuleVOList(List<EvaluationRule> rules) {
        List<EvaluationRuleVO> result = new ArrayList<>();
        for (EvaluationRule rule : rules) {
            result.add(toRuleVO(rule));
        }
        return result;
    }

    /**
     * 将评分项实体列表转换为 VO 列表。
     *
     * @param items 评分项实体列表。
     * @return 评分项 VO 列表。
     */
    private List<EvaluationItemVO> toItemVOList(List<EvaluationItem> items) {
        List<EvaluationItemVO> result = new ArrayList<>();
        for (EvaluationItem item : items) {
            result.add(toItemVO(item));
        }
        return result;
    }

    /**
     * 将评价规则实体转换为 VO。
     *
     * @param rule 评价规则实体。
     * @return 评价规则 VO。
     */
    private EvaluationRuleVO toRuleVO(EvaluationRule rule) {
        EvaluationRuleVO vo = new EvaluationRuleVO();
        vo.setId(rule.getId());
        vo.setTenantId(rule.getTenantId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setVersionNo(rule.getVersionNo());
        vo.setTaskId(rule.getTaskId());
        vo.setTeachingPointId(rule.getTeachingPointId());
        vo.setTotalScore(rule.getTotalScore());
        vo.setDescription(rule.getDescription());
        vo.setStatus(rule.getStatus());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }

    /**
     * 将评分项实体转换为 VO。
     *
     * @param item 评分项实体。
     * @return 评分项 VO。
     */
    private EvaluationItemVO toItemVO(EvaluationItem item) {
        EvaluationItemVO vo = new EvaluationItemVO();
        vo.setId(item.getId());
        vo.setTenantId(item.getTenantId());
        vo.setEvaluationRuleId(item.getEvaluationRuleId());
        vo.setTeachingPointId(item.getTeachingPointId());
        vo.setItemCode(item.getItemCode());
        vo.setItemName(item.getItemName());
        vo.setItemType(item.getItemType());
        vo.setRelatedResourceId(item.getRelatedResourceId());
        vo.setRelatedApiResourceId(item.getRelatedApiResourceId());
        vo.setRelatedTaskStepId(item.getRelatedTaskStepId());
        vo.setScore(item.getScore());
        vo.setRequired(item.getRequired());
        vo.setAssertionType(item.getAssertionType());
        vo.setAssertionConfigJson(item.getAssertionConfigJson());
        vo.setFailPolicy(item.getFailPolicy());
        vo.setStatus(item.getStatus());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
