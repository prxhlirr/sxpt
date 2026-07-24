package com.sxpt.module.capture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.service.EvaluationConfigService;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.mapper.TaskStepMapper;
import com.sxpt.module.teaching.service.TaskStepService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 动作草稿发布教学步骤服务实现。
 *
 * 业务功能：
 * 1. 将教师已确认的动作草稿发布为 task_step，打通老师备案到教学编排的主链路。
 * 2. 保留 sourceActionDraftId，让后续审计和问题排查可以追溯到原始采集证据。
 *
 * 关键流程：
 * 1. 校验租户、草稿、任务、教学点和操作人，防止无归属步骤进入教学资产。
 * 2. 只允许 CONFIRMED 草稿发布，避免系统建议草稿未经老师确认直接面向学生。
 * 3. 构造 TaskStep 后委托 TaskStepService 创建，复用教学步骤默认值和字段校验规则。
 */
@Service
@Profile("!test")
public class CaptureDraftPublishServiceImpl implements CaptureDraftPublishService {

    private static final String CONFIRM_STATUS_CONFIRMED = "CONFIRMED";

    private static final String STEP_CODE_PREFIX = "CAPTURE_DRAFT_";

    private static final String ITEM_TYPE_KEY_ACTION = "KEY_ACTION";

    private static final String ASSERTION_TYPE_TRACE_EXISTS = "TRACE_EXISTS";

    private static final String FAIL_POLICY_NO_SCORE = "NO_SCORE";

    private static final BigDecimal DEFAULT_EVALUATION_ITEM_SCORE = new BigDecimal("10.00");

    private final CaptureActionDraftMapper actionDraftMapper;

    private final TaskStepMapper taskStepMapper;

    private final TaskStepService taskStepService;

    private final EvaluationConfigService evaluationConfigService;

    public CaptureDraftPublishServiceImpl(CaptureActionDraftMapper actionDraftMapper,
                                          TaskStepMapper taskStepMapper,
                                          TaskStepService taskStepService,
                                          EvaluationConfigService evaluationConfigService) {
        this.actionDraftMapper = actionDraftMapper;
        this.taskStepMapper = taskStepMapper;
        this.taskStepService = taskStepService;
        this.evaluationConfigService = evaluationConfigService;
    }

    /**
     * 将单个已确认动作草稿发布为教学任务步骤。
     *
     * @param tenantId 租户 ID。
     * @param actionDraftId 动作草稿 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param operatorId 发布操作人 ID。
     * @return 已创建的任务步骤。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskStep publishDraftToTaskStep(String tenantId, String actionDraftId, String taskId,
                                           String teachingPointId, String operatorId) {
        return publishDraftToTaskStepInternal(tenantId, actionDraftId, taskId, teachingPointId, null, operatorId);
    }

    /**
     * 将单个已确认动作草稿发布为教学任务步骤，并生成基础 TRACE_EXISTS 评分项。
     *
     * @param tenantId 租户 ID。
     * @param actionDraftId 动作草稿 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param operatorId 发布操作人 ID。
     * @return 已创建或已存在的任务步骤。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskStep publishDraftToTaskStep(String tenantId, String actionDraftId, String taskId,
                                           String teachingPointId, String evaluationRuleId, String operatorId) {
        requireText(evaluationRuleId);
        return publishDraftToTaskStepInternal(tenantId, actionDraftId, taskId, teachingPointId,
                evaluationRuleId, operatorId);
    }

    /**
     * 统一执行草稿发布流程，用 evaluationRuleId 决定是否同步生成或补齐评分项。
     *
     * @param tenantId 租户 ID。
     * @param actionDraftId 动作草稿 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID，为空时仅发布步骤。
     * @param operatorId 发布操作人 ID。
     * @return 已创建或已存在的任务步骤。
     */
    private TaskStep publishDraftToTaskStepInternal(String tenantId, String actionDraftId, String taskId,
                                                    String teachingPointId, String evaluationRuleId,
                                                    String operatorId) {
        requireText(tenantId);
        requireText(actionDraftId);
        requireText(taskId);
        requireText(teachingPointId);
        requireText(operatorId);
        CaptureActionDraft actionDraft = loadConfirmedDraft(tenantId, actionDraftId);
        TaskStep existed = findPublishedTaskStep(tenantId, actionDraftId);
        if (existed != null) {
            ensureEvaluationItemExists(actionDraft, existed, teachingPointId, evaluationRuleId, operatorId);
            return existed;
        }
        TaskStep taskStep = taskStepService.createTaskStep(buildTaskStep(actionDraft, taskId, teachingPointId,
                operatorId));
        ensureEvaluationItemExists(actionDraft, taskStep, teachingPointId, evaluationRuleId, operatorId);
        return taskStep;
    }

    /**
     * 读取并校验已确认动作草稿。
     *
     * @param tenantId 租户 ID。
     * @param actionDraftId 动作草稿 ID。
     * @return 已确认动作草稿。
     */
    private CaptureActionDraft loadConfirmedDraft(String tenantId, String actionDraftId) {
        CaptureActionDraft actionDraft = actionDraftMapper.selectOne(new QueryWrapper<CaptureActionDraft>()
                .eq("tenant_id", tenantId)
                .eq("id", actionDraftId)
                .eq("deleted", Boolean.FALSE));
        if (actionDraft == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!CONFIRM_STATUS_CONFIRMED.equals(actionDraft.getConfirmStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return actionDraft;
    }

    /**
     * 查询动作草稿是否已经发布为教学步骤。
     *
     * @param tenantId 租户 ID。
     * @param actionDraftId 动作草稿 ID。
     * @return 已发布教学步骤；没有发布时返回 null。
     */
    private TaskStep findPublishedTaskStep(String tenantId, String actionDraftId) {
        return taskStepMapper.selectOne(new QueryWrapper<TaskStep>()
                .eq("tenant_id", tenantId)
                .eq("source_action_draft_id", actionDraftId)
                .eq("deleted", Boolean.FALSE));
    }

    /**
     * 将动作草稿转换为教学任务步骤。
     *
     * @param actionDraft 已确认动作草稿。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param operatorId 发布操作人 ID。
     * @return 待创建任务步骤。
     */
    private TaskStep buildTaskStep(CaptureActionDraft actionDraft, String taskId, String teachingPointId,
                                   String operatorId) {
        TaskStep taskStep = new TaskStep();
        taskStep.setId(generateId());
        taskStep.setTenantId(actionDraft.getTenantId());
        taskStep.setTaskId(taskId);
        taskStep.setTeachingPointId(teachingPointId);
        taskStep.setStepCode(buildStepCode(actionDraft));
        taskStep.setStepName(buildStepName(actionDraft));
        taskStep.setStepDescription(actionDraft.getConfirmedOperationName());
        taskStep.setSequenceNo(actionDraft.getSequenceNo());
        taskStep.setRelatedResourceIds(buildRelatedResourceIds(actionDraft.getConnectorResourceId()));
        taskStep.setGuideContent(actionDraft.getGuideContent());
        taskStep.setPracticeHint(actionDraft.getPracticeHint());
        taskStep.setSourceActionDraftId(actionDraft.getId());
        taskStep.setCreateBy(operatorId);
        taskStep.setUpdateBy(operatorId);
        return taskStep;
    }

    /**
     * 生成教学步骤编码。
     *
     * @param actionDraft 已确认动作草稿。
     * @return 步骤编码。
     */
    private String buildStepCode(CaptureActionDraft actionDraft) {
        return STEP_CODE_PREFIX + actionDraft.getId();
    }

    /**
     * 生成教学步骤名称。
     *
     * @param actionDraft 已确认动作草稿。
     * @return 步骤名称。
     */
    private String buildStepName(CaptureActionDraft actionDraft) {
        if (StringUtils.hasText(actionDraft.getConfirmedStepName())) {
            return actionDraft.getConfirmedStepName();
        }
        return actionDraft.getActionName();
    }

    /**
     * 构造关联资源 JSON。
     *
     * @param connectorResourceId 正式资源 ID。
     * @return JSON 数组字符串；没有资源时返回 null。
     */
    private String buildRelatedResourceIds(String connectorResourceId) {
        if (!StringUtils.hasText(connectorResourceId)) {
            return null;
        }
        return "[\"" + connectorResourceId + "\"]";
    }

    /**
     * 已有教学步骤时补齐评分项，新建教学步骤时创建评分项。
     *
     * @param actionDraft 已确认动作草稿。
     * @param taskStep 已创建或已存在的教学步骤。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID，为空时不处理评分项。
     * @param operatorId 发布操作人 ID。
     */
    private void ensureEvaluationItemExists(CaptureActionDraft actionDraft, TaskStep taskStep,
                                            String teachingPointId, String evaluationRuleId,
                                            String operatorId) {
        if (!StringUtils.hasText(evaluationRuleId)) {
            return;
        }
        if (hasExistingEvaluationItem(actionDraft.getTenantId(), evaluationRuleId, taskStep.getId(),
                buildStepCode(actionDraft))) {
            return;
        }
        evaluationConfigService.createEvaluationItem(buildEvaluationItem(actionDraft, taskStep, teachingPointId,
                evaluationRuleId, operatorId));
    }

    /**
     * 判断指定评价规则下是否已经存在当前教学步骤对应的评分项。
     *
     * @param tenantId 租户 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param taskStepId 教学步骤 ID。
     * @param itemCode 评分项编码。
     * @return 存在时返回 true。
     */
    private boolean hasExistingEvaluationItem(String tenantId, String evaluationRuleId, String taskStepId,
                                              String itemCode) {
        List<EvaluationItem> items = evaluationConfigService.listItemsByRule(tenantId, evaluationRuleId);
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream().anyMatch(item -> item != null
                && itemCode.equals(item.getItemCode())
                && taskStepId.equals(item.getRelatedTaskStepId()));
    }

    /**
     * 根据已发布步骤生成 MVP 基础评分项。
     *
     * @param actionDraft 已确认动作草稿。
     * @param taskStep 已创建的教学步骤。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param operatorId 发布操作人 ID。
     * @return 待创建的评分项。
     */
    private EvaluationItem buildEvaluationItem(CaptureActionDraft actionDraft, TaskStep taskStep,
                                               String teachingPointId, String evaluationRuleId,
                                               String operatorId) {
        EvaluationItem item = new EvaluationItem();
        item.setId(generateId());
        item.setTenantId(actionDraft.getTenantId());
        item.setEvaluationRuleId(evaluationRuleId);
        item.setTeachingPointId(teachingPointId);
        item.setItemCode(buildStepCode(actionDraft));
        item.setItemName(taskStep.getStepName());
        item.setItemType(ITEM_TYPE_KEY_ACTION);
        item.setRelatedResourceId(actionDraft.getConnectorResourceId());
        item.setRelatedTaskStepId(taskStep.getId());
        item.setScore(DEFAULT_EVALUATION_ITEM_SCORE);
        item.setRequired(Boolean.TRUE);
        item.setAssertionType(ASSERTION_TYPE_TRACE_EXISTS);
        item.setAssertionConfigJson(buildTraceExistsAssertionConfig(actionDraft.getActionType()));
        item.setFailPolicy(FAIL_POLICY_NO_SCORE);
        item.setCreateBy(operatorId);
        item.setUpdateBy(operatorId);
        return item;
    }

    /**
     * 构造轨迹证据断言配置。
     *
     * @param actionType 动作类型。
     * @return TRACE_EXISTS 断言 JSON。
     */
    private String buildTraceExistsAssertionConfig(String actionType) {
        return "{\"traceType\":\"" + normalizeTraceType(actionType) + "\"}";
    }

    /**
     * 将采集动作类型映射为学生轨迹类型。
     *
     * @param actionType 动作类型。
     * @return 轨迹类型。
     */
    private String normalizeTraceType(String actionType) {
        if ("VERIFY_STATE".equals(actionType)) {
            return "STATE";
        }
        return actionType;
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
     * 生成教学步骤主键。
     *
     * @return 32 位无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
