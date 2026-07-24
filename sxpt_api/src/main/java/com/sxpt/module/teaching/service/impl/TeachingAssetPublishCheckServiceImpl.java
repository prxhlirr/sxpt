package com.sxpt.module.teaching.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import com.sxpt.module.teaching.service.TeachingAssetPublishCheckService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 教学资产发布前完整性校验服务实现。
 *
 * 业务功能：
 * 1. 校验教学点下是否已经存在可执行的教学步骤。
 * 2. 校验评分规则下是否存在关联当前教学步骤的评分项，避免学生提交后无法自动评分。
 *
 * 关键流程：
 * 1. 先校验租户、任务、教学点和评分规则参数，保证发布校验有明确归属。
 * 2. 再读取教学步骤，缺步骤时直接阻断发布。
 * 3. 最后读取评分项，只要存在一个关联当前教学点步骤的评分项，即认为 MVP 发布闸门通过。
 */
@Service
@Profile("!test")
public class TeachingAssetPublishCheckServiceImpl implements TeachingAssetPublishCheckService {

    private static final String TRACE_EXISTS = "TRACE_EXISTS";

    private final TaskStepService taskStepService;

    private final EvaluationConfigService evaluationConfigService;

    public TeachingAssetPublishCheckServiceImpl(TaskStepService taskStepService,
                                                EvaluationConfigService evaluationConfigService) {
        this.taskStepService = taskStepService;
        this.evaluationConfigService = evaluationConfigService;
    }

    /**
     * 校验指定教学点资产是否具备发布给学生执行的最小完整性。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     */
    @Override
    public void validateReadyToPublish(String tenantId, String taskId, String teachingPointId,
                                       String evaluationRuleId) {
        requireText(tenantId);
        requireText(taskId);
        requireText(teachingPointId);
        requireText(evaluationRuleId);

        List<TaskStep> taskSteps = taskStepService.listByTaskAndTeachingPoint(tenantId, taskId, teachingPointId);
        if (taskSteps == null || taskSteps.isEmpty()) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        validateTaskStepCompleteness(taskSteps);
        validateResourceAndIdentitySwitch(taskSteps);
        List<EvaluationItem> evaluationItems = evaluationConfigService.listItemsByRule(tenantId, evaluationRuleId);
        validateEvaluationItems(evaluationItems);
        if (!hasEvaluationItemForTaskSteps(teachingPointId, taskSteps, evaluationItems)) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    /**
     * 校验教学步骤是否满足学生端可执行的最小字段完整性。
     *
     * @param taskSteps 当前教学点下的教学步骤。
     */
    private void validateTaskStepCompleteness(List<TaskStep> taskSteps) {
        for (TaskStep taskStep : taskSteps) {
            if (isRequiredTaskStep(taskStep)
                    && (!StringUtils.hasText(taskStep.getId())
                    || !StringUtils.hasText(taskStep.getStepName())
                    || taskStep.getSequenceNo() == null)) {
                throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
            }
        }
    }

    /**
     * 判断教学步骤是否属于发布前必须完整校验的必做步骤。
     *
     * @param taskStep 教学步骤。
     * @return 必做步骤返回 true。
     */
    private boolean isRequiredTaskStep(TaskStep taskStep) {
        return taskStep != null && !Boolean.FALSE.equals(taskStep.getRequired());
    }

    /**
     * 校验资源引用和身份切换字段是否完整。
     *
     * @param taskSteps 当前教学点下的教学步骤。
     */
    private void validateResourceAndIdentitySwitch(List<TaskStep> taskSteps) {
        for (TaskStep taskStep : taskSteps) {
            if (hasEmptyResourceReference(taskStep) || hasIncompleteIdentitySwitch(taskStep)) {
                throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
            }
        }
    }

    /**
     * 判断步骤是否填写了空资源引用。
     *
     * @param taskStep 教学步骤。
     * @return 填写空资源引用时返回 true。
     */
    private boolean hasEmptyResourceReference(TaskStep taskStep) {
        if (taskStep == null || !StringUtils.hasText(taskStep.getRelatedResourceIds())) {
            return false;
        }
        String resourceIds = taskStep.getRelatedResourceIds().trim();
        return "[]".equals(resourceIds);
    }

    /**
     * 判断步骤是否声明了身份切换但缺少运行态必要字段。
     *
     * @param taskStep 教学步骤。
     * @return 身份切换配置不完整时返回 true。
     */
    private boolean hasIncompleteIdentitySwitch(TaskStep taskStep) {
        if (taskStep == null || !requiresIdentitySwitch(taskStep)) {
            return false;
        }
        return !StringUtils.hasText(taskStep.getActorType())
                || !StringUtils.hasText(taskStep.getRequiredExternalOrgId())
                || !StringUtils.hasText(taskStep.getRequiredExternalRoleId());
    }

    /**
     * 判断步骤是否已经表达身份切换意图。
     *
     * @param taskStep 教学步骤。
     * @return 需要身份切换时返回 true。
     */
    private boolean requiresIdentitySwitch(TaskStep taskStep) {
        return StringUtils.hasText(taskStep.getSwitchStrategy())
                || taskStep.getNextSegmentNo() != null
                || Boolean.TRUE.equals(taskStep.getSwitchConfirmRequired())
                || StringUtils.hasText(taskStep.getActorType())
                || StringUtils.hasText(taskStep.getRequiredExternalOrgId())
                || StringUtils.hasText(taskStep.getRequiredExternalRoleId());
    }

    /**
     * 校验评分项是否满足自动评分读取的最小字段完整性。
     *
     * @param evaluationItems 指定评分规则下的评分项。
     */
    private void validateEvaluationItems(List<EvaluationItem> evaluationItems) {
        if (evaluationItems == null || evaluationItems.isEmpty()) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        for (EvaluationItem evaluationItem : evaluationItems) {
            if (!isSupportedEvaluationItem(evaluationItem)) {
                throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
            }
        }
    }

    /**
     * 判断评分项是否为 MVP 自动评分可读取的有效评分项。
     *
     * @param evaluationItem 评分项。
     * @return 可读取时返回 true。
     */
    private boolean isSupportedEvaluationItem(EvaluationItem evaluationItem) {
        return evaluationItem != null
                && StringUtils.hasText(evaluationItem.getId())
                && StringUtils.hasText(evaluationItem.getItemCode())
                && StringUtils.hasText(evaluationItem.getRelatedTaskStepId())
                && evaluationItem.getScore() != null
                && evaluationItem.getScore().compareTo(BigDecimal.ZERO) > 0
                && TRACE_EXISTS.equals(evaluationItem.getAssertionType())
                && hasTraceExistsConfig(evaluationItem.getAssertionConfigJson());
    }

    /**
     * 判断 TRACE_EXISTS 断言配置是否包含轨迹类型。
     *
     * @param assertionConfigJson 断言配置 JSON。
     * @return 包含 traceType 或 eventType 时返回 true。
     */
    private boolean hasTraceExistsConfig(String assertionConfigJson) {
        if (!StringUtils.hasText(assertionConfigJson)) {
            return false;
        }
        return assertionConfigJson.contains("\"traceType\"") || assertionConfigJson.contains("\"eventType\"");
    }

    /**
     * 判断评分规则下是否存在关联当前教学点步骤的评分项。
     *
     * @param teachingPointId 教学点 ID。
     * @param taskSteps 当前教学点下的教学步骤。
     * @param evaluationItems 指定评分规则下的评分项。
     * @return 存在关联评分项时返回 true。
     */
    private boolean hasEvaluationItemForTaskSteps(String teachingPointId, List<TaskStep> taskSteps,
                                                  List<EvaluationItem> evaluationItems) {
        if (evaluationItems == null || evaluationItems.isEmpty()) {
            return false;
        }
        Set<String> taskStepIds = new HashSet<>();
        for (TaskStep taskStep : taskSteps) {
            if (StringUtils.hasText(taskStep.getId())) {
                taskStepIds.add(taskStep.getId());
            }
        }
        for (EvaluationItem evaluationItem : evaluationItems) {
            if (teachingPointId.equals(evaluationItem.getTeachingPointId())
                    && taskStepIds.contains(evaluationItem.getRelatedTaskStepId())) {
                return true;
            }
        }
        return false;
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
}
