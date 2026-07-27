package com.sxpt.module.capture.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.capture.dto.ConfirmCaptureActionDraftRequest;
import com.sxpt.module.capture.dto.CreateCaptureActionDraftRequest;
import com.sxpt.module.capture.dto.DiscardCaptureActionDraftRequest;
import com.sxpt.module.capture.dto.PublishCaptureActionDraftRequest;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.service.CaptureActionDraftGenerateService;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import com.sxpt.module.capture.service.CaptureDraftPublishService;
import com.sxpt.module.capture.vo.CaptureActionDraftGenerateResultVO;
import com.sxpt.module.capture.vo.CaptureActionDraftVO;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.vo.TaskStepVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 页面动作草稿接口。
 *
 * 业务功能：
 * 1. 提供系统生成页面动作草稿的创建入口。
 * 2. 提供按采集会话查询草稿入口，为教师确认草稿页面提供数据。
 * 3. 提供按采集会话自动生成草稿入口，支撑老师备案链路自动化闭环。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截缺少动作名称或顺序号的无效草稿。
 * 2. 将 DTO 转换为 CaptureActionDraft 实体并生成应用层主键。
 * 3. 调用 Service 写入 PENDING 草稿，再转换为 VO 返回前端。
 * 4. 自动生成入口只负责编排请求和响应，具体事件过滤、幂等和草稿构造规则由 Service 保持单一职责。
 */
@RestController
@RequestMapping("/api/v1/capture/action-drafts")
@ConditionalOnProperty(name = "sxpt.capture.action-draft-controller.enabled", havingValue = "true", matchIfMissing = true)
public class CaptureActionDraftController {

    private final CaptureActionDraftService actionDraftService;

    private final CaptureActionDraftGenerateService actionDraftGenerateService;

    private final CaptureDraftPublishService draftPublishService;

    public CaptureActionDraftController(CaptureActionDraftService actionDraftService,
                                        CaptureActionDraftGenerateService actionDraftGenerateService,
                                        CaptureDraftPublishService draftPublishService) {
        this.actionDraftService = actionDraftService;
        this.actionDraftGenerateService = actionDraftGenerateService;
        this.draftPublishService = draftPublishService;
    }

    /**
     * 创建页面动作草稿。
     *
     * @param request 创建页面动作草稿请求。
     * @return 已保存的页面动作草稿。
     */
    @PostMapping("/create")
    public ApiResult<CaptureActionDraftVO> create(@Valid @RequestBody CreateCaptureActionDraftRequest request) {
        CaptureActionDraft saved = actionDraftService.createActionDraft(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询指定采集会话下的页面动作草稿。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 页面动作草稿列表。
     */
    @GetMapping
    public ApiResult<List<CaptureActionDraftVO>> listBySession(@RequestParam String tenantId,
                                                               @RequestParam String captureSessionId) {
        return ApiResult.success(toVOList(actionDraftService.listBySession(tenantId, captureSessionId)));
    }

    /**
     * 按采集会话自动生成页面动作草稿。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @param operatorId 触发生成的老师或系统操作人 ID。
     * @return 本次新生成的草稿数量和草稿列表。
     */
    @PostMapping("/generate")
    public ApiResult<CaptureActionDraftGenerateResultVO> generateFromSession(@RequestParam String tenantId,
                                                                             @RequestParam String captureSessionId,
                                                                             @RequestParam String operatorId) {
        List<CaptureActionDraft> generatedDrafts = actionDraftGenerateService.generateDraftsFromSession(
                tenantId, captureSessionId, operatorId);
        CaptureActionDraftGenerateResultVO result = new CaptureActionDraftGenerateResultVO();
        result.setGeneratedCount(generatedDrafts.size());
        result.setDrafts(toVOList(generatedDrafts));
        return ApiResult.success(result);
    }

    /**
     * 教师确认页面动作草稿。
     *
     * @param id 页面动作草稿 ID。
     * @param request 确认页面动作草稿请求。
     * @return 已确认的页面动作草稿。
     */
    @PostMapping("/{id}/confirm")
    public ApiResult<CaptureActionDraftVO> confirm(@PathVariable String id,
                                                   @Valid @RequestBody ConfirmCaptureActionDraftRequest request) {
        CaptureActionDraft confirmed = actionDraftService.confirmActionDraft(
                id,
                request.getConfirmedOperationName(),
                request.getConfirmedStepName(),
                request.getGuideContent(),
                request.getPracticeHint(),
                request.getConnectorResourceId(),
                request.getUpdateBy());
        return ApiResult.success(toVO(confirmed));
    }

    /**
     * 发布页面动作草稿为教学任务步骤。
     *
     * @param id 页面动作草稿 ID。
     * @param request 发布页面动作草稿请求。
     * @return 已发布或已存在的教学任务步骤。
     */
    @PostMapping("/{id}/publish")
    public ApiResult<TaskStepVO> publish(@PathVariable String id,
                                         @Valid @RequestBody PublishCaptureActionDraftRequest request) {
        String currentUserId = CurrentUserContext.getRequiredUser().getUserId();
        TaskStep taskStep = draftPublishService.publishDraftToTaskStep(
                request.getTenantId(),
                id,
                request.getTaskId(),
                request.getTeachingPointId(),
                request.getEvaluationRuleId(),
                currentUserId);
        return ApiResult.success(toTaskStepVO(taskStep));
    }

    /**
     * 教师丢弃页面动作草稿。
     *
     * @param id 页面动作草稿 ID。
     * @param request 丢弃页面动作草稿请求。
     * @return 已丢弃的页面动作草稿。
     */
    @PostMapping("/{id}/discard")
    public ApiResult<CaptureActionDraftVO> discard(@PathVariable String id,
                                                   @Valid @RequestBody DiscardCaptureActionDraftRequest request) {
        return ApiResult.success(toVO(actionDraftService.discardActionDraft(id, request.getUpdateBy())));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建页面动作草稿请求。
     * @return 页面动作草稿实体。
     */
    private CaptureActionDraft toEntity(CreateCaptureActionDraftRequest request) {
        CaptureActionDraft actionDraft = new CaptureActionDraft();
        actionDraft.setId(generateId());
        actionDraft.setTenantId(request.getTenantId());
        actionDraft.setCaptureSessionId(request.getCaptureSessionId());
        actionDraft.setEventId(request.getEventId());
        actionDraft.setActionName(request.getActionName());
        actionDraft.setActionType(request.getActionType());
        actionDraft.setSequenceNo(request.getSequenceNo());
        actionDraft.setConnectorResourceId(request.getConnectorResourceId());
        actionDraft.setSuggestedOperationName(request.getSuggestedOperationName());
        actionDraft.setConfirmedOperationName(request.getConfirmedOperationName());
        actionDraft.setConfirmedStepName(request.getConfirmedStepName());
        actionDraft.setGuideContent(request.getGuideContent());
        actionDraft.setPracticeHint(request.getPracticeHint());
        actionDraft.setConfirmStatus(request.getConfirmStatus());
        return actionDraft;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param actionDrafts 页面动作草稿实体列表。
     * @return 页面动作草稿展示对象列表。
     */
    private List<CaptureActionDraftVO> toVOList(List<CaptureActionDraft> actionDrafts) {
        List<CaptureActionDraftVO> result = new ArrayList<>();
        for (CaptureActionDraft actionDraft : actionDrafts) {
            result.add(toVO(actionDraft));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param actionDraft 页面动作草稿实体。
     * @return 页面动作草稿展示对象。
     */
    private CaptureActionDraftVO toVO(CaptureActionDraft actionDraft) {
        CaptureActionDraftVO vo = new CaptureActionDraftVO();
        vo.setId(actionDraft.getId());
        vo.setTenantId(actionDraft.getTenantId());
        vo.setCaptureSessionId(actionDraft.getCaptureSessionId());
        vo.setEventId(actionDraft.getEventId());
        vo.setActionName(actionDraft.getActionName());
        vo.setActionType(actionDraft.getActionType());
        vo.setSequenceNo(actionDraft.getSequenceNo());
        vo.setConnectorResourceId(actionDraft.getConnectorResourceId());
        vo.setSuggestedOperationName(actionDraft.getSuggestedOperationName());
        vo.setConfirmedOperationName(actionDraft.getConfirmedOperationName());
        vo.setConfirmedStepName(actionDraft.getConfirmedStepName());
        vo.setGuideContent(actionDraft.getGuideContent());
        vo.setPracticeHint(actionDraft.getPracticeHint());
        vo.setConfirmStatus(actionDraft.getConfirmStatus());
        vo.setStatus(actionDraft.getStatus());
        vo.setCreateTime(actionDraft.getCreateTime());
        vo.setUpdateTime(actionDraft.getUpdateTime());
        return vo;
    }

    /**
     * 将教学步骤实体转换为前端展示对象。
     *
     * @param taskStep 教学步骤实体。
     * @return 教学步骤展示对象。
     */
    private TaskStepVO toTaskStepVO(TaskStep taskStep) {
        TaskStepVO vo = new TaskStepVO();
        vo.setId(taskStep.getId());
        vo.setTenantId(taskStep.getTenantId());
        vo.setTaskId(taskStep.getTaskId());
        vo.setTeachingPointId(taskStep.getTeachingPointId());
        vo.setStepCode(taskStep.getStepCode());
        vo.setStepName(taskStep.getStepName());
        vo.setStepDescription(taskStep.getStepDescription());
        vo.setSequenceNo(taskStep.getSequenceNo());
        vo.setSegmentNo(taskStep.getSegmentNo());
        vo.setActorType(taskStep.getActorType());
        vo.setRequiredExternalOrgId(taskStep.getRequiredExternalOrgId());
        vo.setRequiredExternalOrgName(taskStep.getRequiredExternalOrgName());
        vo.setRequiredExternalRoleId(taskStep.getRequiredExternalRoleId());
        vo.setRequiredExternalRoleName(taskStep.getRequiredExternalRoleName());
        vo.setSwitchStrategy(taskStep.getSwitchStrategy());
        vo.setNextSegmentNo(taskStep.getNextSegmentNo());
        vo.setSwitchConfirmRequired(taskStep.getSwitchConfirmRequired());
        vo.setSwitchDecisionSource(taskStep.getSwitchDecisionSource());
        vo.setSwitchReason(taskStep.getSwitchReason());
        vo.setRollbackPolicy(taskStep.getRollbackPolicy());
        vo.setRelatedResourceIds(taskStep.getRelatedResourceIds());
        vo.setGuideContent(taskStep.getGuideContent());
        vo.setPracticeHint(taskStep.getPracticeHint());
        vo.setRequired(taskStep.getRequired());
        vo.setAllowSkip(taskStep.getAllowSkip());
        vo.setSourceActionDraftId(taskStep.getSourceActionDraftId());
        vo.setStatus(taskStep.getStatus());
        vo.setCreateTime(taskStep.getCreateTime());
        vo.setUpdateTime(taskStep.getUpdateTime());
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
