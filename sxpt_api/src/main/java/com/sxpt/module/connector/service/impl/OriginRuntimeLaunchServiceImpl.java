package com.sxpt.module.connector.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.dto.CreateOriginRuntimeLaunchRequest;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.ConnectorSystemService;
import com.sxpt.module.connector.service.OriginRuntimeLaunchService;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.vo.OriginRuntimeLaunchVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

/**
 * 原平台运行时启动服务实现。
 *
 * 业务功能：
 * 1. 创建不绑定原平台业务数据的平台首页启动上下文。
 * 2. 使用原平台基础配置的 baseUrl 作为首页地址，统一拼接教学入口。
 *
 * 关键流程：
 * 1. 校验当前用户和原平台配置。
 * 2. 将老师/学生当前教学场景写入 PlatformLaunchContext。
 * 3. 调用 PlatformLaunchContextService 生成 launchToken。
 * 4. 返回 `/teaching/launch?token=xxx`，后续页面识别、造数和 DataSession 注册由原平台完成。
 */
@Service
@Profile("!test")
public class OriginRuntimeLaunchServiceImpl implements OriginRuntimeLaunchService {

    private static final String TEACHING_LAUNCH_PATH = "/teaching/launch";

    private final ConnectorSystemService connectorSystemService;

    private final PlatformLaunchContextService platformLaunchContextService;

    public OriginRuntimeLaunchServiceImpl(ConnectorSystemService connectorSystemService,
                                          PlatformLaunchContextService platformLaunchContextService) {
        this.connectorSystemService = connectorSystemService;
        this.platformLaunchContextService = platformLaunchContextService;
    }

    /**
     * 创建进入原平台首页的运行时启动地址。
     *
     * @param request 创建运行时启动请求。
     * @return 原平台运行时启动返回对象。
     */
    @Override
    public OriginRuntimeLaunchVO createLaunch(CreateOriginRuntimeLaunchRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        ConnectorSystem connectorSystem = connectorSystemService.getConnectorSystemById(request.getConnectorSystemId());
        if (!currentUser.getTenantId().equals(connectorSystem.getTenantId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        String originHomeUrl = normalizeHomeUrl(connectorSystem.getBaseUrl());
        PlatformLaunchContext launchContext = buildLaunchContext(request, currentUser, originHomeUrl);
        PlatformLaunchContextService.CreatedLaunchContext created =
                platformLaunchContextService.createPlatformHomeLaunchContext(launchContext);
        return toVO(created, originHomeUrl);
    }

    /**
     * 构造平台首页启动上下文。
     *
     * @param request 创建运行时启动请求。
     * @param currentUser 当前教学平台用户。
     * @param originHomeUrl 原平台首页地址。
     * @return 平台首页启动上下文。
     */
    private PlatformLaunchContext buildLaunchContext(CreateOriginRuntimeLaunchRequest request,
                                                     CurrentUserContext.CurrentUser currentUser,
                                                     String originHomeUrl) {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId(generateId());
        launchContext.setTenantId(currentUser.getTenantId());
        launchContext.setUserId(currentUser.getUserId());
        launchContext.setConnectorSystemId(request.getConnectorSystemId());
        launchContext.setTaskId(request.getTaskId());
        launchContext.setTeachingPointId(request.getTeachingPointId());
        launchContext.setExecutionId(request.getExecutionId());
        launchContext.setCaptureSessionId(request.getCaptureSessionId());
        launchContext.setPracticeAttemptId(request.getPracticeAttemptId());
        launchContext.setExamAttemptId(request.getExamAttemptId());
        launchContext.setQuestionAttemptId(request.getQuestionAttemptId());
        launchContext.setSceneType(request.getSceneType());
        launchContext.setSdkMode(request.getSdkMode());
        launchContext.setActorType(request.getActorType());
        launchContext.setOriginHomeUrl(originHomeUrl);
        launchContext.setCreateBy(currentUser.getUserId());
        launchContext.setUpdateBy(currentUser.getUserId());
        return launchContext;
    }

    /**
     * 转换运行时启动返回对象。
     *
     * @param created 已创建启动上下文与明文 token。
     * @param originHomeUrl 原平台首页地址。
     * @return 原平台运行时启动返回对象。
     */
    private OriginRuntimeLaunchVO toVO(PlatformLaunchContextService.CreatedLaunchContext created, String originHomeUrl) {
        PlatformLaunchContext launchContext = created.getLaunchContext();
        OriginRuntimeLaunchVO vo = new OriginRuntimeLaunchVO();
        vo.setLaunchContextId(launchContext.getId());
        vo.setLaunchToken(created.getLaunchToken());
        vo.setOriginHomeUrl(originHomeUrl);
        vo.setLaunchUrl(buildLaunchUrl(originHomeUrl, created.getLaunchToken()));
        vo.setConnectorSystemId(launchContext.getConnectorSystemId());
        vo.setSceneType(launchContext.getSceneType());
        vo.setSdkMode(launchContext.getSdkMode());
        vo.setActorType(launchContext.getActorType());
        vo.setExpireTime(launchContext.getExpireTime());
        return vo;
    }

    /**
     * 规范化原平台首页地址。
     *
     * @param baseUrl 原平台配置中的基础地址。
     * @return 去除尾部斜杠后的首页地址。
     */
    private String normalizeHomeUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /**
     * 拼接原平台教学入口地址。
     *
     * @param originHomeUrl 原平台首页地址。
     * @param launchToken 本次明文 launchToken。
     * @return 带 token 的原平台教学入口。
     */
    private String buildLaunchUrl(String originHomeUrl, String launchToken) {
        return UriComponentsBuilder.fromHttpUrl(originHomeUrl)
                .path(TEACHING_LAUNCH_PATH)
                .queryParam("token", launchToken)
                .build()
                .toUriString();
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
