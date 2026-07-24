package com.sxpt.module.capture.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.dto.ConfirmCaptureSegmentSwitchRequest;
import com.sxpt.module.capture.service.CaptureSegmentSwitchService;
import com.sxpt.module.capture.vo.CaptureSegmentSwitchVO;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 备案片段切换服务实现。
 *
 * 业务功能：
 * 1. 将教师确认的下一片段路径转换为原平台启动上下文。
 * 2. 通过已有 launchToken 服务生成下一片段跳转令牌，避免重复实现 token 安全逻辑。
 *
 * 关键流程：
 * 1. 校验当前片段和下一片段编号，防止无意义的同片段切换。
 * 2. 固定 sceneType=RECORD、sdkMode=CAPTURE，表达教师备案采集场景。
 * 3. 返回下一片段的 token 和角色单位摘要，前端据此跳转原平台。
 */
@Service
@Profile("!test")
@ConditionalOnProperty(name = "sxpt.capture.segment-switch-service.enabled", havingValue = "true", matchIfMissing = true)
public class CaptureSegmentSwitchServiceImpl implements CaptureSegmentSwitchService {

    private static final String RECORD_SCENE_TYPE = "RECORD";

    private static final String CAPTURE_SDK_MODE = "CAPTURE";

    private static final String TEACHER_PATH_DECISION_SOURCE = "TEACHER_PATH";

    private final PlatformLaunchContextService platformLaunchContextService;

    public CaptureSegmentSwitchServiceImpl(PlatformLaunchContextService platformLaunchContextService) {
        this.platformLaunchContextService = platformLaunchContextService;
    }

    /**
     * 确认并创建下一备案片段的启动上下文。
     *
     * @param request 确认备案片段切换请求。
     * @return 下一片段切换结果。
     */
    @Override
    public CaptureSegmentSwitchVO confirmSwitch(ConfirmCaptureSegmentSwitchRequest request) {
        validateRequest(request);
        PlatformLaunchContextService.CreatedLaunchContext created =
                platformLaunchContextService.createLaunchContext(toLaunchContext(request));
        return toVO(request, created);
    }

    /**
     * 校验教师确认切换请求。
     *
     * @param request 确认备案片段切换请求。
     */
    private void validateRequest(ConfirmCaptureSegmentSwitchRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getTeacherId());
        requireText(request.getConnectorSystemId());
        requireText(request.getRequiredExternalOrgId());
        requireText(request.getRequiredExternalRoleId());
        requireText(request.getTargetUrl());
        if (request.getCurrentSegmentNo() == null || request.getNextSegmentNo() == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (request.getCurrentSegmentNo().equals(request.getNextSegmentNo())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
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
     * 将教师确认请求转换为原平台启动上下文。
     *
     * @param request 确认备案片段切换请求。
     * @return 原平台启动上下文实体。
     */
    private PlatformLaunchContext toLaunchContext(ConfirmCaptureSegmentSwitchRequest request) {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId(generateId());
        launchContext.setTenantId(request.getTenantId());
        launchContext.setUserId(request.getTeacherId());
        launchContext.setConnectorSystemId(request.getConnectorSystemId());
        launchContext.setDataInstanceId(request.getDataInstanceId());
        launchContext.setSceneType(RECORD_SCENE_TYPE);
        launchContext.setSdkMode(CAPTURE_SDK_MODE);
        launchContext.setTargetUrl(request.getTargetUrl());
        launchContext.setSegmentNo(request.getNextSegmentNo());
        launchContext.setActorType(request.getActorType());
        launchContext.setRequiredExternalOrgId(request.getRequiredExternalOrgId());
        launchContext.setRequiredExternalOrgName(request.getRequiredExternalOrgName());
        launchContext.setRequiredExternalRoleId(request.getRequiredExternalRoleId());
        launchContext.setRequiredExternalRoleName(request.getRequiredExternalRoleName());
        launchContext.setExternalBusinessId(request.getExternalBusinessId());
        launchContext.setExternalBusinessNo(request.getExternalBusinessNo());
        launchContext.setDataScopeJson(request.getDataScopeJson());
        launchContext.setCreateBy(request.getTeacherId());
        return launchContext;
    }

    /**
     * 将启动上下文创建结果转换为片段切换返回对象。
     *
     * @param request 确认备案片段切换请求。
     * @param created 已创建的启动上下文和明文 token。
     * @return 备案片段切换返回对象。
     */
    private CaptureSegmentSwitchVO toVO(ConfirmCaptureSegmentSwitchRequest request,
                                        PlatformLaunchContextService.CreatedLaunchContext created) {
        PlatformLaunchContext launchContext = created.getLaunchContext();
        CaptureSegmentSwitchVO vo = new CaptureSegmentSwitchVO();
        vo.setLaunchContextId(launchContext.getId());
        vo.setLaunchToken(created.getLaunchToken());
        vo.setTenantId(launchContext.getTenantId());
        vo.setTeacherId(launchContext.getUserId());
        vo.setConnectorSystemId(launchContext.getConnectorSystemId());
        vo.setCurrentSegmentNo(request.getCurrentSegmentNo());
        vo.setNextSegmentNo(launchContext.getSegmentNo());
        vo.setActorType(launchContext.getActorType());
        vo.setRequiredExternalOrgId(launchContext.getRequiredExternalOrgId());
        vo.setRequiredExternalOrgName(launchContext.getRequiredExternalOrgName());
        vo.setRequiredExternalRoleId(launchContext.getRequiredExternalRoleId());
        vo.setRequiredExternalRoleName(launchContext.getRequiredExternalRoleName());
        vo.setTargetUrl(launchContext.getTargetUrl());
        vo.setSwitchConfirmRequired(Boolean.TRUE);
        vo.setSwitchDecisionSource(TEACHER_PATH_DECISION_SOURCE);
        vo.setSwitchReason(request.getSwitchReason());
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
