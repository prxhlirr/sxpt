package com.sxpt.module.capture.service.impl;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.service.CaptureActionDraftGenerateService;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.capture.service.TeacherFilingOrchestrationService;
import com.sxpt.module.capture.vo.TeacherFilingOrchestrationResultVO;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TeachingAssetPublishCheckService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 老师备案编排服务实现。
 *
 * 业务功能：
 * 1. 串联采集事件生成草稿、已确认草稿发布教学步骤、基础评分项补齐和发布前完整性校验。
 * 2. 为教师端提供一个稳定的备案准备入口，降低前端直接串联多个底层服务的错误概率。
 *
 * 关键流程：
 * 1. 先按采集会话生成新增动作草稿，再读取会话下所有动作草稿。
 * 2. 仅发布 CONFIRMED 草稿，PENDING 草稿进入缺陷清单，避免未经老师确认的动作直接面向学生。
 * 3. 发布完成后调用教学资产发布前校验，校验失败时返回缺陷清单而不是吞掉问题。
 */
@Service
@Profile("!test")
public class TeacherFilingOrchestrationServiceImpl implements TeacherFilingOrchestrationService {

    private static final String CONFIRM_STATUS_CONFIRMED = "CONFIRMED";

    private final CaptureActionDraftGenerateService actionDraftGenerateService;

    private final CaptureActionDraftService actionDraftService;

    private final CaptureDraftPublishService draftPublishService;

    private final TeachingAssetPublishCheckService publishCheckService;

    public TeacherFilingOrchestrationServiceImpl(CaptureActionDraftGenerateService actionDraftGenerateService,
                                                 CaptureActionDraftService actionDraftService,
                                                 CaptureDraftPublishService draftPublishService,
                                                 TeachingAssetPublishCheckService publishCheckService) {
        this.actionDraftGenerateService = actionDraftGenerateService;
        this.actionDraftService = actionDraftService;
        this.draftPublishService = draftPublishService;
        this.publishCheckService = publishCheckService;
    }

    /**
     * 准备指定采集会话对应的教学资产。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param operatorId 操作人 ID。
     * @return 老师备案编排结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeacherFilingOrchestrationResultVO prepareTeachingAssets(String tenantId,
                                                                    String captureSessionId,
                                                                    String taskId,
                                                                    String teachingPointId,
                                                                    String evaluationRuleId,
                                                                    String operatorId) {
        requireText(tenantId);
        requireText(captureSessionId);
        requireText(taskId);
        requireText(teachingPointId);
        requireText(evaluationRuleId);
        requireText(operatorId);

        List<String> defectMessages = new ArrayList<>();
        List<CaptureActionDraft> generatedDrafts = actionDraftGenerateService.generateDraftsFromSession(
                tenantId, captureSessionId, operatorId);
        List<CaptureActionDraft> sessionDrafts = actionDraftService.listBySession(tenantId, captureSessionId);
        int publishedStepCount = publishConfirmedDrafts(tenantId, taskId, teachingPointId, evaluationRuleId,
                operatorId, sessionDrafts, defectMessages);
        boolean readyToPublish = validateReadyToPublish(tenantId, taskId, teachingPointId, evaluationRuleId,
                defectMessages);

        return buildResult(captureSessionId, taskId, teachingPointId, evaluationRuleId,
                safeSize(generatedDrafts), publishedStepCount, readyToPublish, defectMessages);
    }

    /**
     * 发布已确认草稿，未确认草稿只记录缺陷，避免系统替老师做语义确认。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param operatorId 操作人 ID。
     * @param sessionDrafts 会话下动作草稿。
     * @param defectMessages 缺陷消息列表。
     * @return 成功发布或已存在的教学步骤数量。
     */
    private int publishConfirmedDrafts(String tenantId,
                                       String taskId,
                                       String teachingPointId,
                                       String evaluationRuleId,
                                       String operatorId,
                                       List<CaptureActionDraft> sessionDrafts,
                                       List<String> defectMessages) {
        if (sessionDrafts == null || sessionDrafts.isEmpty()) {
            defectMessages.add("采集会话下没有可备案的动作草稿。");
            return 0;
        }
        int publishedStepCount = 0;
        for (CaptureActionDraft draft : sessionDrafts) {
            if (draft == null || !StringUtils.hasText(draft.getId())) {
                continue;
            }
            if (!CONFIRM_STATUS_CONFIRMED.equals(draft.getConfirmStatus())) {
                defectMessages.add("动作草稿未确认，draftId=" + draft.getId());
                continue;
            }
            TaskStep taskStep = draftPublishService.publishDraftToTaskStep(tenantId, draft.getId(), taskId,
                    teachingPointId, evaluationRuleId, operatorId);
            if (taskStep != null) {
                publishedStepCount++;
            }
        }
        return publishedStepCount;
    }

    /**
     * 执行发布前完整性校验，并把失败原因转换为编排结果中的缺陷清单。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param defectMessages 缺陷消息列表。
     * @return 校验通过时返回 true。
     */
    private boolean validateReadyToPublish(String tenantId,
                                           String taskId,
                                           String teachingPointId,
                                           String evaluationRuleId,
                                           List<String> defectMessages) {
        try {
            publishCheckService.validateReadyToPublish(tenantId, taskId, teachingPointId, evaluationRuleId);
            return defectMessages.isEmpty();
        } catch (BusinessException ex) {
            defectMessages.add("教学资产发布前完整性校验未通过。");
            return false;
        }
    }

    /**
     * 构造老师备案编排结果。
     *
     * @param captureSessionId 采集会话 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @param generatedDraftCount 本次新增草稿数量。
     * @param publishedStepCount 已发布步骤数量。
     * @param readyToPublish 是否满足发布条件。
     * @param defectMessages 缺陷消息列表。
     * @return 老师备案编排结果。
     */
    private TeacherFilingOrchestrationResultVO buildResult(String captureSessionId,
                                                           String taskId,
                                                           String teachingPointId,
                                                           String evaluationRuleId,
                                                           int generatedDraftCount,
                                                           int publishedStepCount,
                                                           boolean readyToPublish,
                                                           List<String> defectMessages) {
        TeacherFilingOrchestrationResultVO result = new TeacherFilingOrchestrationResultVO();
        result.setCaptureSessionId(captureSessionId);
        result.setTaskId(taskId);
        result.setTeachingPointId(teachingPointId);
        result.setEvaluationRuleId(evaluationRuleId);
        result.setGeneratedDraftCount(generatedDraftCount);
        result.setPublishedStepCount(publishedStepCount);
        result.setBackfilledEvaluationItemCount(0);
        result.setReadyToPublish(readyToPublish);
        result.setDefectMessages(defectMessages);
        return result;
    }

    /**
     * 计算列表大小，避免服务返回 null 时编排流程空指针。
     *
     * @param values 列表。
     * @return 列表大小。
     */
    private int safeSize(List<?> values) {
        if (values == null) {
            return 0;
        }
        return values.size();
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(com.sxpt.common.api.ApiResultCode.PARAM_ERROR);
        }
    }
}
