package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.vo.VerifiedPlatformLaunchContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 原平台最小改造联调探针。
 *
 * 业务功能：
 * 1. 模拟原平台提供的 /teaching-launch?token=xxx 入口，用于验证教学平台到原平台的跳转闭环。
 * 2. 串联 token 校验、原平台 session 建立成功回写和上下文返回，便于在真实原平台接入前完成后端契约验证。
 *
 * 关键流程：
 * 1. 接收 tenantId 和明文 launchToken。
 * 2. 调用 verify 校验 token 并拿到原平台运行上下文。
 * 3. 模拟原平台已成功建立 session，将启动上下文标记为 USED。
 * 4. 返回原平台可用于切换单位、角色、业务数据和 SDK 模式的上下文摘要。
 */
@RestController
@ConditionalOnProperty(name = "sxpt.connector.origin-launch-probe-controller.enabled", havingValue = "true", matchIfMissing = true)
public class OriginPlatformLaunchProbeController {

    private final PlatformLaunchContextService platformLaunchContextService;

    public OriginPlatformLaunchProbeController(PlatformLaunchContextService platformLaunchContextService) {
        this.platformLaunchContextService = platformLaunchContextService;
    }

    /**
     * 模拟原平台教学入口。
     *
     * @param tenantId 租户 ID，用于隔离不同租户下的启动令牌。
     * @param token 明文 launchToken。
     * @return 已建立 session 的启动上下文摘要。
     */
    @GetMapping("/teaching-launch")
    public ApiResult<VerifiedPlatformLaunchContextVO> teachingLaunch(@RequestParam String tenantId,
                                                                     @RequestParam("token") String token) {
        PlatformLaunchContext verified = platformLaunchContextService.verifyLaunchToken(tenantId, token);
        PlatformLaunchContext used = platformLaunchContextService.markLaunchContextUsed(verified.getId());
        return ApiResult.success(toVerifiedVO(used));
    }

    /**
     * 将已使用启动上下文转换为原平台返回对象。
     *
     * @param launchContext 已使用的启动上下文。
     * @return 原平台可用于建立 session 的上下文摘要。
     */
    private VerifiedPlatformLaunchContextVO toVerifiedVO(PlatformLaunchContext launchContext) {
        VerifiedPlatformLaunchContextVO vo = new VerifiedPlatformLaunchContextVO();
        vo.setId(launchContext.getId());
        vo.setTenantId(launchContext.getTenantId());
        vo.setUserId(launchContext.getUserId());
        vo.setConnectorSystemId(launchContext.getConnectorSystemId());
        vo.setTaskId(launchContext.getTaskId());
        vo.setTeachingPointId(launchContext.getTeachingPointId());
        vo.setExecutionId(launchContext.getExecutionId());
        vo.setDataInstanceId(launchContext.getDataInstanceId());
        vo.setSceneType(launchContext.getSceneType());
        vo.setSdkMode(launchContext.getSdkMode());
        vo.setTargetUrl(launchContext.getTargetUrl());
        vo.setSegmentNo(launchContext.getSegmentNo());
        vo.setActorType(launchContext.getActorType());
        vo.setRequiredExternalOrgId(launchContext.getRequiredExternalOrgId());
        vo.setRequiredExternalOrgName(launchContext.getRequiredExternalOrgName());
        vo.setRequiredExternalRoleId(launchContext.getRequiredExternalRoleId());
        vo.setRequiredExternalRoleName(launchContext.getRequiredExternalRoleName());
        vo.setExternalBusinessId(launchContext.getExternalBusinessId());
        vo.setExternalBusinessNo(launchContext.getExternalBusinessNo());
        vo.setDataScopeJson(launchContext.getDataScopeJson());
        vo.setLaunchStatus(launchContext.getLaunchStatus());
        vo.setVerifiedTime(launchContext.getVerifiedTime());
        vo.setExpireTime(launchContext.getExpireTime());
        return vo;
    }
}
