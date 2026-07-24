package com.sxpt.module.execution.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.execution.dto.IdentitySwitchRunRequest;
import com.sxpt.module.execution.service.IdentitySwitchRunService;
import com.sxpt.module.execution.vo.IdentitySwitchRunVO;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * 身份切换运行服务实现。
 *
 * 业务功能：
 * 1. 将学生运行态的下一步骤切换请求转换为原平台 launchToken。
 * 2. 使用 task_step 中的单位、角色和片段信息作为唯一可信身份来源。
 *
 * 关键流程：
 * 1. 校验请求最小字段和 SDK 模式。
 * 2. 查询任务教学点下的步骤，定位当前步骤和下一步骤。
 * 3. 将下一步骤身份要求写入 PlatformLaunchContext，交给 launchToken 服务保存。
 */
@Service
@Profile("!test")
public class IdentitySwitchRunServiceImpl implements IdentitySwitchRunService {

    private static final String LEARNING_SDK_MODE = "LEARNING";

    private static final String PRACTICE_SDK_MODE = "PRACTICE";

    private static final String EXAM_SDK_MODE = "EXAM";

    private static final String LEARN_SCENE_TYPE = "LEARN";

    private static final String PRACTICE_SCENE_TYPE = "PRACTICE";

    private static final String EXAM_SCENE_TYPE = "EXAM";

    private static final String STEP_DECISION_SOURCE = "TASK_STEP";

    private final TaskStepService taskStepService;

    private final PlatformLaunchContextService platformLaunchContextService;

    public IdentitySwitchRunServiceImpl(TaskStepService taskStepService,
                                        PlatformLaunchContextService platformLaunchContextService) {
        this.taskStepService = taskStepService;
        this.platformLaunchContextService = platformLaunchContextService;
    }

    /**
     * 创建下一步骤身份切换运行上下文。
     *
     * @param request 身份切换运行请求。
     * @return 身份切换运行结果。
     */
    @Override
    public IdentitySwitchRunVO runSwitch(IdentitySwitchRunRequest request) {
        validateRequest(request);
        List<TaskStep> taskSteps = taskStepService.listByTaskAndTeachingPoint(
                request.getTenantId(), request.getTaskId(), request.getTeachingPointId());
        TaskStep currentStep = findOptionalStep(taskSteps, request.getCurrentStepId());
        TaskStep nextStep = findRequiredStep(taskSteps, request.getNextStepId());
        validateNextStep(request, nextStep);

        PlatformLaunchContextService.CreatedLaunchContext created =
                platformLaunchContextService.createLaunchContext(toLaunchContext(request, nextStep));
        return toVO(request, currentStep, nextStep, created);
    }

    /**
     * 校验身份切换运行请求。
     *
     * @param request 身份切换运行请求。
     */
    private void validateRequest(IdentitySwitchRunRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getStudentId());
        requireText(request.getConnectorSystemId());
        requireText(request.getTaskId());
        requireText(request.getTeachingPointId());
        requireText(request.getExecutionId());
        requireText(request.getNextStepId());
        requireText(request.getSdkMode());
        requireText(request.getTargetUrl());
        if (request.getNextStepId().equals(request.getCurrentStepId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        resolveSceneType(request.getSdkMode());
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
     * 查找可为空的当前步骤。
     *
     * @param taskSteps 当前任务教学点步骤列表。
     * @param stepId 当前步骤 ID。
     * @return 当前步骤，未传入时返回 null。
     */
    private TaskStep findOptionalStep(List<TaskStep> taskSteps, String stepId) {
        if (!StringUtils.hasText(stepId)) {
            return null;
        }
        return findRequiredStep(taskSteps, stepId);
    }

    /**
     * 查找必须存在的任务步骤。
     *
     * @param taskSteps 当前任务教学点步骤列表。
     * @param stepId 步骤 ID。
     * @return 匹配的任务步骤。
     */
    private TaskStep findRequiredStep(List<TaskStep> taskSteps, String stepId) {
        if (taskSteps == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        for (TaskStep taskStep : taskSteps) {
            if (taskStep != null && stepId.equals(taskStep.getId())) {
                return taskStep;
            }
        }
        throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
    }

    /**
     * 校验下一步骤具备可执行的原平台身份要求。
     *
     * @param request 身份切换运行请求。
     * @param nextStep 下一步骤。
     */
    private void validateNextStep(IdentitySwitchRunRequest request, TaskStep nextStep) {
        if (!request.getTenantId().equals(nextStep.getTenantId())
                || !request.getTaskId().equals(nextStep.getTaskId())
                || !request.getTeachingPointId().equals(nextStep.getTeachingPointId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (!StringUtils.hasText(nextStep.getRequiredExternalOrgId())
                || !StringUtils.hasText(nextStep.getRequiredExternalRoleId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    /**
     * 将身份切换请求转换为原平台启动上下文。
     *
     * @param request 身份切换运行请求。
     * @param nextStep 下一步骤。
     * @return 原平台启动上下文。
     */
    private PlatformLaunchContext toLaunchContext(IdentitySwitchRunRequest request, TaskStep nextStep) {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId(generateId());
        launchContext.setTenantId(request.getTenantId());
        launchContext.setUserId(request.getStudentId());
        launchContext.setConnectorSystemId(request.getConnectorSystemId());
        launchContext.setTaskId(request.getTaskId());
        launchContext.setTeachingPointId(request.getTeachingPointId());
        launchContext.setExecutionId(request.getExecutionId());
        launchContext.setDataInstanceId(request.getDataInstanceId());
        launchContext.setSceneType(resolveSceneType(request.getSdkMode()));
        launchContext.setSdkMode(request.getSdkMode());
        launchContext.setTargetUrl(request.getTargetUrl());
        launchContext.setSegmentNo(nextStep.getSegmentNo());
        launchContext.setActorType(nextStep.getActorType());
        launchContext.setRequiredExternalOrgId(nextStep.getRequiredExternalOrgId());
        launchContext.setRequiredExternalOrgName(nextStep.getRequiredExternalOrgName());
        launchContext.setRequiredExternalRoleId(nextStep.getRequiredExternalRoleId());
        launchContext.setRequiredExternalRoleName(nextStep.getRequiredExternalRoleName());
        launchContext.setExternalBusinessId(request.getExternalBusinessId());
        launchContext.setExternalBusinessNo(request.getExternalBusinessNo());
        launchContext.setDataScopeJson(request.getDataScopeJson());
        launchContext.setCreateBy(request.getStudentId());
        return launchContext;
    }

    /**
     * 根据 SDK 模式解析原平台启动场景。
     *
     * @param sdkMode SDK 模式。
     * @return 原平台场景类型。
     */
    private String resolveSceneType(String sdkMode) {
        if (LEARNING_SDK_MODE.equals(sdkMode)) {
            return LEARN_SCENE_TYPE;
        }
        if (PRACTICE_SDK_MODE.equals(sdkMode)) {
            return PRACTICE_SCENE_TYPE;
        }
        if (EXAM_SDK_MODE.equals(sdkMode)) {
            return EXAM_SCENE_TYPE;
        }
        throw new BusinessException(ApiResultCode.PARAM_ERROR);
    }

    /**
     * 将启动上下文创建结果转换为身份切换返回对象。
     *
     * @param request 身份切换运行请求。
     * @param currentStep 当前步骤，可为空。
     * @param nextStep 下一步骤。
     * @param created 已创建的启动上下文和明文 token。
     * @return 身份切换运行返回对象。
     */
    private IdentitySwitchRunVO toVO(IdentitySwitchRunRequest request, TaskStep currentStep, TaskStep nextStep,
                                     PlatformLaunchContextService.CreatedLaunchContext created) {
        PlatformLaunchContext launchContext = created.getLaunchContext();
        IdentitySwitchRunVO vo = new IdentitySwitchRunVO();
        vo.setLaunchContextId(launchContext.getId());
        vo.setLaunchToken(created.getLaunchToken());
        vo.setTenantId(launchContext.getTenantId());
        vo.setStudentId(launchContext.getUserId());
        vo.setConnectorSystemId(launchContext.getConnectorSystemId());
        vo.setTaskId(launchContext.getTaskId());
        vo.setTeachingPointId(launchContext.getTeachingPointId());
        vo.setExecutionId(launchContext.getExecutionId());
        vo.setCurrentStepId(request.getCurrentStepId());
        vo.setNextStepId(nextStep.getId());
        vo.setCurrentSegmentNo(currentStep == null ? null : currentStep.getSegmentNo());
        vo.setNextSegmentNo(nextStep.getSegmentNo());
        vo.setSdkMode(launchContext.getSdkMode());
        vo.setSceneType(launchContext.getSceneType());
        vo.setActorType(launchContext.getActorType());
        vo.setRequiredExternalOrgId(launchContext.getRequiredExternalOrgId());
        vo.setRequiredExternalOrgName(launchContext.getRequiredExternalOrgName());
        vo.setRequiredExternalRoleId(launchContext.getRequiredExternalRoleId());
        vo.setRequiredExternalRoleName(launchContext.getRequiredExternalRoleName());
        vo.setTargetUrl(launchContext.getTargetUrl());
        vo.setSwitchConfirmRequired(Boolean.TRUE.equals(nextStep.getSwitchConfirmRequired()));
        vo.setSwitchDecisionSource(StringUtils.hasText(nextStep.getSwitchDecisionSource())
                ? nextStep.getSwitchDecisionSource() : STEP_DECISION_SOURCE);
        vo.setSwitchStrategy(nextStep.getSwitchStrategy());
        vo.setExpireTime(launchContext.getExpireTime());
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

