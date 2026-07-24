package com.sxpt.module.capture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 页面动作草稿服务实现。
 *
 * 业务功能：
 * 1. 保存由采集事件整理出的页面动作草稿，支撑教师后续确认或丢弃。
 * 2. 提供按采集会话查询草稿能力，保证草稿按采集顺序展示。
 *
 * 关键流程：
 * 1. 创建草稿时校验最小业务字段，避免产生无法确认的空草稿。
 * 2. 新草稿默认进入 PENDING 状态，后续确认流程再修改确认字段。
 */
@Service
@Profile("!test")
public class CaptureActionDraftServiceImpl implements CaptureActionDraftService {

    private static final String DEFAULT_CONFIRM_STATUS = "PENDING";

    private static final String CONFIRM_STATUS_CONFIRMED = "CONFIRMED";

    private static final String CONFIRM_STATUS_DISCARDED = "DISCARDED";

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final CaptureActionDraftMapper actionDraftMapper;

    public CaptureActionDraftServiceImpl(CaptureActionDraftMapper actionDraftMapper) {
        this.actionDraftMapper = actionDraftMapper;
    }

    /**
     * 创建单条页面动作草稿。
     *
     * @param actionDraft 页面动作草稿。
     * @return 已保存的页面动作草稿。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureActionDraft createActionDraft(CaptureActionDraft actionDraft) {
        validateCreateFields(actionDraft);
        fillCreateDefaults(actionDraft);
        actionDraftMapper.insert(actionDraft);
        return actionDraft;
    }

    /**
     * 查询指定采集会话下的页面动作草稿。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 按顺序号排列的页面动作草稿列表。
     */
    @Override
    public List<CaptureActionDraft> listBySession(String tenantId, String captureSessionId) {
        requireText(tenantId);
        requireText(captureSessionId);
        return actionDraftMapper.selectList(new QueryWrapper<CaptureActionDraft>()
                .eq("tenant_id", tenantId)
                .eq("capture_session_id", captureSessionId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "create_time"));
    }

    /**
     * 教师确认页面动作草稿。
     *
     * @param id 页面动作草稿 ID。
     * @param confirmedOperationName 教师确认业务操作名称。
     * @param confirmedStepName 教师确认教学步骤名称。
     * @param guideContent 学习模式提示内容。
     * @param practiceHint 练习模式提示内容。
     * @param updateBy 更新人 ID。
     * @return 已确认的页面动作草稿。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureActionDraft confirmActionDraft(String id,
                                                 String confirmedOperationName,
                                                 String confirmedStepName,
                                                 String guideContent,
                                                 String practiceHint,
                                                 String updateBy) {
        return confirmActionDraft(id, confirmedOperationName, confirmedStepName, guideContent, practiceHint,
                null, updateBy);
    }

    /**
     * 教师确认页面动作草稿并绑定正式资源。
     *
     * @param id 页面动作草稿 ID。
     * @param confirmedOperationName 教师确认业务操作名称。
     * @param confirmedStepName 教师确认教学步骤名称。
     * @param guideContent 学习模式提示内容。
     * @param practiceHint 练习模式提示内容。
     * @param connectorResourceId 正式资源 ID，允许为空表示该动作暂不绑定资源。
     * @param updateBy 更新人 ID。
     * @return 已确认的页面动作草稿。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureActionDraft confirmActionDraft(String id,
                                                 String confirmedOperationName,
                                                 String confirmedStepName,
                                                 String guideContent,
                                                 String practiceHint,
                                                 String connectorResourceId,
                                                 String updateBy) {
        requireText(id);
        requireText(confirmedOperationName);
        requireText(confirmedStepName);
        CaptureActionDraft actionDraft = requirePendingDraft(id);
        LocalDateTime now = LocalDateTime.now();
        actionDraft.setConfirmedOperationName(confirmedOperationName);
        actionDraft.setConfirmedStepName(confirmedStepName);
        actionDraft.setGuideContent(guideContent);
        actionDraft.setPracticeHint(practiceHint);
        if (StringUtils.hasText(connectorResourceId)) {
            actionDraft.setConnectorResourceId(connectorResourceId);
        }
        actionDraft.setConfirmStatus(CONFIRM_STATUS_CONFIRMED);
        actionDraft.setUpdateBy(updateBy);
        actionDraft.setUpdateTime(now);
        actionDraftMapper.updateById(actionDraft);
        return actionDraft;
    }

    /**
     * 教师丢弃页面动作草稿。
     *
     * @param id 页面动作草稿 ID。
     * @param updateBy 更新人 ID。
     * @return 已丢弃的页面动作草稿。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CaptureActionDraft discardActionDraft(String id, String updateBy) {
        requireText(id);
        CaptureActionDraft actionDraft = requirePendingDraft(id);
        actionDraft.setConfirmStatus(CONFIRM_STATUS_DISCARDED);
        actionDraft.setUpdateBy(updateBy);
        actionDraft.setUpdateTime(LocalDateTime.now());
        actionDraftMapper.updateById(actionDraft);
        return actionDraft;
    }

    /**
     * 校验创建页面动作草稿所需的最小字段。
     *
     * @param actionDraft 页面动作草稿。
     */
    private void validateCreateFields(CaptureActionDraft actionDraft) {
        if (actionDraft == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(actionDraft.getId());
        requireText(actionDraft.getTenantId());
        requireText(actionDraft.getCaptureSessionId());
        requireText(actionDraft.getActionName());
        requireText(actionDraft.getActionType());
        if (actionDraft.getSequenceNo() == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
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
     * 读取并校验待确认的页面动作草稿。
     *
     * @param id 页面动作草稿 ID。
     * @return PENDING 状态的页面动作草稿。
     */
    private CaptureActionDraft requirePendingDraft(String id) {
        CaptureActionDraft actionDraft = actionDraftMapper.selectById(id);
        if (actionDraft == null || Boolean.TRUE.equals(actionDraft.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!DEFAULT_CONFIRM_STATUS.equals(actionDraft.getConfirmStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return actionDraft;
    }

    /**
     * 补齐草稿创建时的默认字段。
     *
     * @param actionDraft 页面动作草稿。
     */
    private void fillCreateDefaults(CaptureActionDraft actionDraft) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(actionDraft.getConfirmStatus())) {
            actionDraft.setConfirmStatus(DEFAULT_CONFIRM_STATUS);
        }
        if (actionDraft.getCreateTime() == null) {
            actionDraft.setCreateTime(now);
        }
        if (actionDraft.getUpdateTime() == null) {
            actionDraft.setUpdateTime(now);
        }
        if (!StringUtils.hasText(actionDraft.getStatus())) {
            actionDraft.setStatus(DEFAULT_STATUS);
        }
        if (actionDraft.getDeleted() == null) {
            actionDraft.setDeleted(Boolean.FALSE);
        }
    }
}
