package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.dto.RegisterOriginDataSessionRequest;
import com.sxpt.module.connector.entity.OriginDataSession;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.OriginDataSessionMapper;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.OriginBusinessSceneService;
import com.sxpt.module.connector.service.OriginDataSessionService;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.vo.OriginBusinessSceneVO;
import com.sxpt.module.connector.vo.OriginDataSessionVO;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 原平台数据会话服务实现。
 *
 * 业务功能：
 * 1. 接收原平台在业务页面造数或复制完成后的 DataSession 注册请求。
 * 2. 将原平台业务数据引用转换成教学平台可追溯的 TeachingDataInstance 和 OriginDataSession。
 *
 * 关键流程：
 * 1. 使用 launchToken 解析本次教学启动上下文，但不推进 launchToken 状态。
 * 2. 按租户和幂等键查找已有 DataSession，重复注册直接返回已有绑定。
 * 3. 新注册时创建教学数据实例，再创建 DataSession。
 * 4. 返回遮罩层后续上报所需的 dataSessionId 和 dataInstanceId。
 */
@Service
@Profile("!test")
public class OriginDataSessionServiceImpl implements OriginDataSessionService {

    private static final String SESSION_STATUS_REGISTERED = "REGISTERED";

    private static final String SCENE_TYPE_PRACTICE = "PRACTICE";

    private static final String SCENE_TYPE_EXAM = "EXAM";

    private static final String GENERATION_SOURCE_ORIGIN_SELF_CREATED = "ORIGIN_SELF_CREATED";

    private static final String GENERATION_SOURCE_ORIGIN_COPIED_FROM_RECORD = "ORIGIN_COPIED_FROM_RECORD";

    private static final String ORIGIN_REGISTERED_TEMPLATE_ID = "ORIGIN_REGISTERED";

    private final OriginDataSessionMapper originDataSessionMapper;

    private final TeachingDataInstanceMapper teachingDataInstanceMapper;

    private final PlatformLaunchContextService platformLaunchContextService;

    private final OriginBusinessSceneService originBusinessSceneService;

    public OriginDataSessionServiceImpl(OriginDataSessionMapper originDataSessionMapper,
                                        TeachingDataInstanceMapper teachingDataInstanceMapper,
                                        PlatformLaunchContextService platformLaunchContextService,
                                        OriginBusinessSceneService originBusinessSceneService) {
        this.originDataSessionMapper = originDataSessionMapper;
        this.teachingDataInstanceMapper = teachingDataInstanceMapper;
        this.platformLaunchContextService = platformLaunchContextService;
        this.originBusinessSceneService = originBusinessSceneService;
    }

    /**
     * 注册原平台数据会话。
     *
     * @param request 原平台数据会话注册请求。
     * @return 原平台数据会话返回对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OriginDataSessionVO register(RegisterOriginDataSessionRequest request) {
        validateRequest(request);
        PlatformLaunchContext launchContext =
                platformLaunchContextService.resolveLaunchToken(request.getTenantId(), request.getLaunchToken());
        validateRegisterAgainstLaunchContext(request, launchContext);
        OriginDataSession existing = findByIdempotencyKey(request.getTenantId(), request.getIdempotencyKey());
        if (existing != null) {
            return toVO(existing, launchContext.getSdkMode());
        }
        TeachingDataInstance instance = createTeachingDataInstance(request, launchContext);
        OriginDataSession session = createOriginDataSession(request, launchContext, instance.getId());
        return toVO(session, launchContext.getSdkMode());
    }

    /**
     * 校验原平台注册请求的最小字段。
     *
     * @param request 原平台数据会话注册请求。
     */
    private void validateRequest(RegisterOriginDataSessionRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getLaunchToken());
        requireText(request.getBusinessSceneCode());
        requireText(request.getExternalBusinessId());
        requireText(request.getIdempotencyKey());
    }

    /**
     * 校验注册请求和启动上下文之间的场景边界。
     * <p>
     * 练习数据必须基于老师备案样本复制，因此 PRACTICE 场景必须携带来源 DataSession 和来源原平台业务数据。
     * 考试数据按题目归属落库，因此 EXAM 场景必须携带 questionAttemptId 并命中本次考试清单。
     *
     * @param request 原平台数据会话注册请求。
     * @param launchContext 启动上下文。
     */
    private void validateRegisterAgainstLaunchContext(RegisterOriginDataSessionRequest request,
                                                      PlatformLaunchContext launchContext) {
        if (SCENE_TYPE_PRACTICE.equals(launchContext.getSceneType())) {
            requireText(request.getSourceDataSessionId());
            requireText(request.getSourceExternalBusinessId());
            ensurePracticeSceneAllowed(request, launchContext);
        }
        if (SCENE_TYPE_EXAM.equals(launchContext.getSceneType())) {
            requireText(request.getQuestionAttemptId());
            ensureExamSceneAllowed(request, launchContext);
        }
    }

    /**
     * 校验练习造数是否命中老师发布任务时沉淀的业务场景。
     * <p>
     * 这里不校验原平台 URL，因为教学平台不再维护原平台模块路径；只校验稳定业务语义和来源样本，
     * 确保学生复制出来的数据确实来自本次练习任务允许的备案产物。
     *
     * @param request 原平台数据会话注册请求。
     * @param launchContext 启动上下文。
     */
    private void ensurePracticeSceneAllowed(RegisterOriginDataSessionRequest request,
                                            PlatformLaunchContext launchContext) {
        List<OriginBusinessSceneVO> scenes = originBusinessSceneService.listScenesForLaunch(launchContext);
        boolean matched = scenes.stream().anyMatch(scene ->
                request.getBusinessSceneCode().equals(scene.getBusinessSceneCode())
                        && request.getSourceDataSessionId().equals(scene.getSourceDataSessionId())
                        && request.getSourceExternalBusinessId().equals(scene.getSourceExternalBusinessId()));
        if (!matched) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    /**
     * 校验考试造数是否命中本次考试题目清单。
     * <p>
     * 考试不一定基于老师备案样本，因此这里不要求 sourceDataSessionId；但必须用 questionAttemptId
     * 把原平台生成的业务数据绑定到具体题目，否则后续评分无法知道“这条原平台数据属于哪一道题”。
     *
     * @param request 原平台数据会话注册请求。
     * @param launchContext 启动上下文。
     */
    private void ensureExamSceneAllowed(RegisterOriginDataSessionRequest request,
                                        PlatformLaunchContext launchContext) {
        List<OriginBusinessSceneVO> scenes = originBusinessSceneService.listScenesForLaunch(launchContext);
        boolean matched = scenes.stream().anyMatch(scene ->
                request.getQuestionAttemptId().equals(scene.getQuestionAttemptId())
                        && request.getBusinessSceneCode().equals(scene.getBusinessSceneCode()));
        if (!matched) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    /**
     * 按幂等键查找已有 DataSession。
     *
     * @param tenantId 租户 ID。
     * @param idempotencyKey 幂等键。
     * @return 已存在的数据会话，不存在时返回 null。
     */
    private OriginDataSession findByIdempotencyKey(String tenantId, String idempotencyKey) {
        return originDataSessionMapper.selectOne(new QueryWrapper<OriginDataSession>()
                .eq("tenant_id", tenantId)
                .eq("idempotency_key", idempotencyKey)
                .eq("deleted", Boolean.FALSE));
    }

    /**
     * 创建教学数据实例。
     *
     * @param request 原平台数据会话注册请求。
     * @param launchContext 启动上下文。
     * @return 已保存教学数据实例。
     */
    private TeachingDataInstance createTeachingDataInstance(RegisterOriginDataSessionRequest request,
                                                           PlatformLaunchContext launchContext) {
        LocalDateTime now = LocalDateTime.now();
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId(generateId());
        instance.setTenantId(launchContext.getTenantId());
        instance.setTemplateId(ORIGIN_REGISTERED_TEMPLATE_ID);
        instance.setConnectorSystemId(launchContext.getConnectorSystemId());
        instance.setOwnerUserId(launchContext.getUserId());
        instance.setTaskId(launchContext.getTaskId());
        instance.setTeachingPointId(launchContext.getTeachingPointId());
        instance.setExecutionId(launchContext.getExecutionId());
        instance.setAttemptId(resolveAttemptId(request, launchContext));
        instance.setSceneType(launchContext.getSceneType());
        instance.setExternalBusinessId(request.getExternalBusinessId());
        instance.setExternalBusinessNo(request.getExternalBusinessNo());
        instance.setExternalStatus(request.getExternalStatus());
        instance.setActorType(launchContext.getActorType());
        instance.setTargetUrl(request.getEntryUrl());
        instance.setGenerationSource(resolveGenerationSource(request));
        instance.setBusinessSceneCode(request.getBusinessSceneCode());
        instance.setBusinessSceneName(request.getBusinessSceneName());
        instance.setSourceDataSessionId(request.getSourceDataSessionId());
        instance.setSourceExternalBusinessId(request.getSourceExternalBusinessId());
        instance.setDataSpecSnapshotJson(request.getDataSpecSnapshotJson());
        instance.setEntryUrl(request.getEntryUrl());
        instance.setRequirementSnapshotJson(request.getDataSpecSnapshotJson());
        instance.setValidationStatus(ValidationStatus.PASSED.getValue());
        instance.setValidationTime(now);
        instance.setValidationResultJson("{\"registeredByOrigin\":true}");
        instance.setInstanceStatus(DataInstanceStatus.READY.getValue());
        instance.setMetadataJson(request.getOriginPayloadSnapshotJson());
        instance.setResetCount(0L);
        instance.setCreateBy(launchContext.getUserId());
        instance.setCreateTime(now);
        instance.setUpdateBy(launchContext.getUserId());
        instance.setUpdateTime(now);
        instance.setStatus(RecordStatus.ACTIVE.getValue());
        instance.setDeleted(Boolean.FALSE);
        teachingDataInstanceMapper.insert(instance);
        return instance;
    }

    /**
     * 创建原平台数据会话。
     *
     * @param request 原平台数据会话注册请求。
     * @param launchContext 启动上下文。
     * @param dataInstanceId 教学数据实例 ID。
     * @return 已保存原平台数据会话。
     */
    private OriginDataSession createOriginDataSession(RegisterOriginDataSessionRequest request,
                                                      PlatformLaunchContext launchContext,
                                                      String dataInstanceId) {
        LocalDateTime now = LocalDateTime.now();
        OriginDataSession session = new OriginDataSession();
        session.setId(generateId());
        session.setTenantId(launchContext.getTenantId());
        session.setLaunchContextId(launchContext.getId());
        session.setConnectorSystemId(launchContext.getConnectorSystemId());
        session.setUserId(launchContext.getUserId());
        session.setActorType(launchContext.getActorType());
        session.setSceneType(launchContext.getSceneType());
        session.setTaskId(launchContext.getTaskId());
        session.setExecutionId(launchContext.getExecutionId());
        session.setCaptureSessionId(launchContext.getCaptureSessionId());
        session.setPracticeAttemptId(launchContext.getPracticeAttemptId());
        session.setExamAttemptId(launchContext.getExamAttemptId());
        session.setQuestionAttemptId(resolveQuestionAttemptId(request, launchContext));
        session.setBusinessSceneCode(request.getBusinessSceneCode());
        session.setBusinessSceneName(request.getBusinessSceneName());
        session.setSourceDataSessionId(request.getSourceDataSessionId());
        session.setSourceExternalBusinessId(request.getSourceExternalBusinessId());
        session.setDataInstanceId(dataInstanceId);
        session.setExternalBusinessId(request.getExternalBusinessId());
        session.setExternalBusinessNo(request.getExternalBusinessNo());
        session.setExternalStatus(request.getExternalStatus());
        session.setEntryUrl(request.getEntryUrl());
        session.setDataSpecSnapshotJson(request.getDataSpecSnapshotJson());
        session.setOriginPayloadSnapshotJson(request.getOriginPayloadSnapshotJson());
        session.setIdempotencyKey(request.getIdempotencyKey());
        session.setSessionStatus(SESSION_STATUS_REGISTERED);
        session.setCreateBy(launchContext.getUserId());
        session.setCreateTime(now);
        session.setUpdateBy(launchContext.getUserId());
        session.setUpdateTime(now);
        session.setStatus(RecordStatus.ACTIVE.getValue());
        session.setDeleted(Boolean.FALSE);
        originDataSessionMapper.insert(session);
        return session;
    }

    /**
     * 解析教学数据实例来源。
     *
     * @param request 原平台数据会话注册请求。
     * @return 教学数据实例来源。
     */
    private String resolveGenerationSource(RegisterOriginDataSessionRequest request) {
        if (StringUtils.hasText(request.getSourceDataSessionId())
                || StringUtils.hasText(request.getSourceExternalBusinessId())) {
            return GENERATION_SOURCE_ORIGIN_COPIED_FROM_RECORD;
        }
        return GENERATION_SOURCE_ORIGIN_SELF_CREATED;
    }

    /**
     * 解析当前场景最合适的 attempt ID。
     *
     * @param request 原平台数据会话注册请求。
     * @param launchContext 启动上下文。
     * @return attempt ID。
     */
    private String resolveAttemptId(RegisterOriginDataSessionRequest request, PlatformLaunchContext launchContext) {
        if (StringUtils.hasText(launchContext.getPracticeAttemptId())) {
            return launchContext.getPracticeAttemptId();
        }
        String questionAttemptId = resolveQuestionAttemptId(request, launchContext);
        if (StringUtils.hasText(questionAttemptId)) {
            return questionAttemptId;
        }
        return launchContext.getExamAttemptId();
    }

    /**
     * 解析题目尝试 ID。
     *
     * @param request 原平台数据会话注册请求。
     * @param launchContext 启动上下文。
     * @return 题目尝试 ID。
     */
    private String resolveQuestionAttemptId(RegisterOriginDataSessionRequest request,
                                            PlatformLaunchContext launchContext) {
        if (StringUtils.hasText(request.getQuestionAttemptId())) {
            return request.getQuestionAttemptId();
        }
        return launchContext.getQuestionAttemptId();
    }

    /**
     * 转换 DataSession 返回对象。
     *
     * @param session 原平台数据会话。
     * @param sdkMode SDK 模式。
     * @return 原平台数据会话返回对象。
     */
    private OriginDataSessionVO toVO(OriginDataSession session, String sdkMode) {
        OriginDataSessionVO vo = new OriginDataSessionVO();
        vo.setDataSessionId(session.getId());
        vo.setDataInstanceId(session.getDataInstanceId());
        vo.setLaunchContextId(session.getLaunchContextId());
        vo.setConnectorSystemId(session.getConnectorSystemId());
        vo.setSceneType(session.getSceneType());
        vo.setSdkMode(sdkMode);
        vo.setBusinessSceneCode(session.getBusinessSceneCode());
        vo.setQuestionAttemptId(session.getQuestionAttemptId());
        vo.setExternalBusinessId(session.getExternalBusinessId());
        vo.setSessionStatus(session.getSessionStatus());
        return vo;
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
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
