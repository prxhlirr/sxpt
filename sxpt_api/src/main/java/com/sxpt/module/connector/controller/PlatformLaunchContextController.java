package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.dto.CreatePlatformLaunchContextRequest;
import com.sxpt.module.connector.dto.MarkPlatformLaunchFailedRequest;
import com.sxpt.module.connector.dto.MarkPlatformLaunchUsedRequest;
import com.sxpt.module.connector.dto.VerifyPlatformLaunchTokenRequest;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.vo.PlatformLaunchContextVO;
import com.sxpt.module.connector.vo.VerifiedPlatformLaunchContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

/**
 * 原平台启动上下文接口。
 *
 * 业务功能：
 * 1. 提供教学平台跳转原平台前创建 launchToken 的入口。
 * 2. 返回本次明文 launchToken 和过期时间，数据库只保存 token hash。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation。
 * 2. 将 DTO 转换为 PlatformLaunchContext 实体，并生成应用层主键。
 * 3. 调用 Service 生成明文 token、保存 hash 和启动上下文。
 * 4. 将结果转换为 VO，避免向前端暴露 launchTokenHash 和软删除字段。
 */
@RestController
@RequestMapping("/api/v1/connector/launch-contexts")
@ConditionalOnProperty(name = "sxpt.connector.launch-context-controller.enabled", havingValue = "true", matchIfMissing = true)
public class PlatformLaunchContextController {

    private final PlatformLaunchContextService platformLaunchContextService;

    public PlatformLaunchContextController(PlatformLaunchContextService platformLaunchContextService) {
        this.platformLaunchContextService = platformLaunchContextService;
    }

    /**
     * 创建原平台启动上下文。
     *
     * @param request 创建启动上下文请求。
     * @return 已创建的启动上下文和本次明文 launchToken。
     */
    @PostMapping("/create")
    public ApiResult<PlatformLaunchContextVO> create(@Valid @RequestBody CreatePlatformLaunchContextRequest request) {
        PlatformLaunchContextService.CreatedLaunchContext created =
                platformLaunchContextService.createLaunchContext(toEntity(request));
        return ApiResult.success(toVO(created));
    }

    /**
     * 校验原平台启动令牌。
     *
     * @param request 原平台启动令牌校验请求。
     * @return 已校验的启动上下文。
     */
    @PostMapping("/verify")
    public ApiResult<VerifiedPlatformLaunchContextVO> verify(@Valid @RequestBody VerifyPlatformLaunchTokenRequest request) {
        PlatformLaunchContext launchContext =
                platformLaunchContextService.verifyLaunchToken(request.getTenantId(), request.getLaunchToken());
        return ApiResult.success(toVerifiedVO(launchContext));
    }

    /**
     * 标记原平台启动上下文已被使用。
     *
     * @param request 标记已使用请求。
     * @return 已标记使用的启动上下文。
     */
    @PostMapping("/used")
    public ApiResult<VerifiedPlatformLaunchContextVO> markUsed(@Valid @RequestBody MarkPlatformLaunchUsedRequest request) {
        PlatformLaunchContext launchContext = platformLaunchContextService.markLaunchContextUsed(request.getId());
        return ApiResult.success(toVerifiedVO(launchContext));
    }

    /**
     * 标记原平台启动失败。
     *
     * @param request 标记失败请求。
     * @return 已标记失败的启动上下文。
     */
    @PostMapping("/failed")
    public ApiResult<VerifiedPlatformLaunchContextVO> markFailed(@Valid @RequestBody MarkPlatformLaunchFailedRequest request) {
        PlatformLaunchContext launchContext =
                platformLaunchContextService.markLaunchContextFailed(request.getId(), request.getErrorMessage());
        return ApiResult.success(toVerifiedVO(launchContext));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建启动上下文请求。
     * @return 原平台启动上下文实体。
     */
    private PlatformLaunchContext toEntity(CreatePlatformLaunchContextRequest request) {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId(generateId());
        launchContext.setTenantId(currentUser.getTenantId());
        launchContext.setUserId(currentUser.getUserId());
        launchContext.setConnectorSystemId(request.getConnectorSystemId());
        launchContext.setTaskId(request.getTaskId());
        launchContext.setTeachingPointId(request.getTeachingPointId());
        launchContext.setExecutionId(request.getExecutionId());
        launchContext.setDataInstanceId(request.getDataInstanceId());
        launchContext.setSceneType(request.getSceneType());
        launchContext.setSdkMode(request.getSdkMode());
        launchContext.setTargetUrl(request.getTargetUrl());
        launchContext.setSegmentNo(request.getSegmentNo());
        launchContext.setActorType(request.getActorType());
        launchContext.setRequiredExternalOrgId(request.getRequiredExternalOrgId());
        launchContext.setRequiredExternalOrgName(request.getRequiredExternalOrgName());
        launchContext.setRequiredExternalRoleId(request.getRequiredExternalRoleId());
        launchContext.setRequiredExternalRoleName(request.getRequiredExternalRoleName());
        launchContext.setExternalBusinessId(request.getExternalBusinessId());
        launchContext.setExternalBusinessNo(request.getExternalBusinessNo());
        launchContext.setDataScopeJson(request.getDataScopeJson());
        launchContext.setCreateBy(currentUser.getUserId());
        launchContext.setUpdateBy(currentUser.getUserId());
        return launchContext;
    }

    /**
     * 将 Service 创建结果转换为前端返回对象。
     *
     * @param created 已创建启动上下文和明文 token。
     * @return 原平台启动上下文返回对象。
     */
    private PlatformLaunchContextVO toVO(PlatformLaunchContextService.CreatedLaunchContext created) {
        PlatformLaunchContext launchContext = created.getLaunchContext();
        PlatformLaunchContextVO vo = new PlatformLaunchContextVO();
        vo.setId(launchContext.getId());
        vo.setTenantId(launchContext.getTenantId());
        vo.setLaunchToken(created.getLaunchToken());
        vo.setUserId(launchContext.getUserId());
        vo.setConnectorSystemId(launchContext.getConnectorSystemId());
        vo.setTaskId(launchContext.getTaskId());
        vo.setTeachingPointId(launchContext.getTeachingPointId());
        vo.setExecutionId(launchContext.getExecutionId());
        vo.setSceneType(launchContext.getSceneType());
        vo.setSdkMode(launchContext.getSdkMode());
        vo.setTargetUrl(launchContext.getTargetUrl());
        vo.setLaunchStatus(launchContext.getLaunchStatus());
        vo.setExpireTime(launchContext.getExpireTime());
        vo.setCreateTime(launchContext.getCreateTime());
        return vo;
    }

    /**
     * 将已校验启动上下文转换为原平台返回对象。
     *
     * @param launchContext 已校验的启动上下文。
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
        vo.setSdkConfigSnapshotJson(launchContext.getSdkConfigSnapshotJson());
        vo.setDataInstanceValidationSnapshotJson(launchContext.getDataInstanceValidationSnapshotJson());
        vo.setLaunchStatus(launchContext.getLaunchStatus());
        vo.setVerifiedTime(launchContext.getVerifiedTime());
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
