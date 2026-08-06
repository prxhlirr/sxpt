package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.connector.service.BusinessModuleService;
import com.sxpt.module.connector.service.ConnectorSystemService;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.connector.support.BusinessSsoLaunchUrlBuilder;
import com.sxpt.module.teachingdata.dto.CreateAuthoringLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.TriggerType;
import com.sxpt.module.teachingdata.service.AuthoringLaunchService;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import com.sxpt.module.teachingdata.service.DataRequirementService;
import com.sxpt.module.teachingdata.vo.AuthoringLaunchVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Trusted server-side orchestration for teacher authoring launches. */
@Service
@Profile("!test")
public class AuthoringLaunchServiceImpl implements AuthoringLaunchService {

    private static final String SCENE_RECORD = "RECORD";
    private static final String SDK_MODE_CAPTURE = "CAPTURE";

    private final ConnectorSystemService connectorSystemService;
    private final BusinessModuleService businessModuleService;
    private final ModuleDataStrategyService moduleDataStrategyService;
    private final TeachingDataTemplateService teachingDataTemplateService;
    private final BusinessModuleProcessChainService processChainService;
    private final DataRequirementService dataRequirementService;
    private final DataPrepareFacadeService dataPrepareFacadeService;
    private final TeachingDataInstanceMapper teachingDataInstanceMapper;
    private final PlatformLaunchContextService platformLaunchContextService;
    private final BusinessSsoLaunchUrlBuilder urlBuilder;

    public AuthoringLaunchServiceImpl(ConnectorSystemService connectorSystemService,
                                      BusinessModuleService businessModuleService,
                                      ModuleDataStrategyService moduleDataStrategyService,
                                      TeachingDataTemplateService teachingDataTemplateService,
                                      BusinessModuleProcessChainService processChainService,
                                      DataRequirementService dataRequirementService,
                                      DataPrepareFacadeService dataPrepareFacadeService,
                                      TeachingDataInstanceMapper teachingDataInstanceMapper,
                                      PlatformLaunchContextService platformLaunchContextService,
                                      BusinessSsoLaunchUrlBuilder urlBuilder) {
        this.connectorSystemService = connectorSystemService;
        this.businessModuleService = businessModuleService;
        this.moduleDataStrategyService = moduleDataStrategyService;
        this.teachingDataTemplateService = teachingDataTemplateService;
        this.processChainService = processChainService;
        this.dataRequirementService = dataRequirementService;
        this.dataPrepareFacadeService = dataPrepareFacadeService;
        this.teachingDataInstanceMapper = teachingDataInstanceMapper;
        this.platformLaunchContextService = platformLaunchContextService;
        this.urlBuilder = urlBuilder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthoringLaunchVO createLaunch(CreateAuthoringLaunchRequest request) {
        validateRequest(request);
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        requireTeacherOrAdmin(user);
        requireText(user.getTenantId());

        ConnectorSystem system = requireActiveSystem(user, request.getConnectorSystemId());
        BusinessModule module = requireActiveModule(user, system, request.getBusinessModuleId());
        ModuleDataStrategy strategy = requireRecordStrategy(user, system, module);
        TeachingDataTemplate template = requireRecordTemplate(user, system, module, strategy);
        BusinessModuleProcessActor actor = requirePrimaryActor(user, module);

        String uniqueId = generateId();
        String requestBatchId = "authoring_" + uniqueId;
        DataRequirement requirement = dataRequirementService.createDataRequirement(
                buildRequirement(request, user, system, module, strategy, template, uniqueId));
        DataPrepareFacadeService.PrepareAndExecuteRequest prepareRequest = buildPrepareRequest(
                requirement, requestBatchId, user, template, strategy, actor);
        DataPrepareJob job = dataPrepareFacadeService.prepareAndExecute(prepareRequest);
        TeachingDataInstance instance = requirePreparedInstance(job, requestBatchId, user);
        PlatformLaunchContextService.CreatedLaunchContext created = platformLaunchContextService.createLaunchContext(
                buildLaunchContext(request, user, system, module, actor, instance));
        return buildResult(system, module, instance, created);
    }

    private void validateRequest(CreateAuthoringLaunchRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getLessonId());
        requireText(request.getConnectorSystemId());
        requireText(request.getBusinessModuleId());
    }

    private void requireTeacherOrAdmin(CurrentUserContext.CurrentUser user) {
        if (!user.hasAnyRole("TEACHER", "ADMIN")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
    }

    private ConnectorSystem requireActiveSystem(CurrentUserContext.CurrentUser user, String systemId) {
        ConnectorSystem system = connectorSystemService.getConnectorSystemById(systemId);
        if (system == null || Boolean.TRUE.equals(system.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!user.getTenantId().equals(system.getTenantId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        if (!RecordStatus.ACTIVE.getValue().equals(system.getStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        requireText(system.getBaseUrl());
        return system;
    }

    private BusinessModule requireActiveModule(CurrentUserContext.CurrentUser user,
                                               ConnectorSystem system,
                                               String moduleId) {
        BusinessModule module = businessModuleService.getBusinessModuleById(moduleId);
        if (module == null || Boolean.TRUE.equals(module.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!user.getTenantId().equals(module.getTenantId())
                || !system.getId().equals(module.getConnectorSystemId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        if (!RecordStatus.ACTIVE.getValue().equals(module.getStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        requireText(module.getModuleCode());
        requireText(module.getEntryUrl());
        return module;
    }

    private ModuleDataStrategy requireRecordStrategy(CurrentUserContext.CurrentUser user,
                                                     ConnectorSystem system,
                                                     BusinessModule module) {
        ModuleDataStrategy strategy = moduleDataStrategyService.getActiveStrategyByModuleCodeAndScene(
                user.getTenantId(), system.getId(), module.getModuleCode(), SCENE_RECORD);
        if (strategy == null) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        if (!user.getTenantId().equals(strategy.getTenantId())
                || !system.getId().equals(strategy.getConnectorSystemId())
                || !module.getId().equals(strategy.getBusinessModuleId())
                || !SCENE_RECORD.equals(strategy.getSceneType())
                || !RecordStatus.ACTIVE.getValue().equals(strategy.getStatus())
                || Boolean.TRUE.equals(strategy.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        requireText(strategy.getTemplateId());
        return strategy;
    }

    private TeachingDataTemplate requireRecordTemplate(CurrentUserContext.CurrentUser user,
                                                       ConnectorSystem system,
                                                       BusinessModule module,
                                                       ModuleDataStrategy strategy) {
        TeachingDataTemplate template = teachingDataTemplateService.getTeachingDataTemplateById(strategy.getTemplateId());
        if (template == null
                || Boolean.TRUE.equals(template.getDeleted())
                || !RecordStatus.ACTIVE.getValue().equals(template.getStatus())
                || !user.getTenantId().equals(template.getTenantId())
                || !system.getId().equals(template.getConnectorSystemId())
                || !module.getModuleCode().equals(template.getModuleCode())
                || !SCENE_RECORD.equals(template.getSceneType())) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        requireText(template.getTemplateCode());
        return template;
    }

    private BusinessModuleProcessActor requirePrimaryActor(CurrentUserContext.CurrentUser user,
                                                           BusinessModule module) {
        List<BusinessModuleProcessStep> steps = processChainService.listActiveProcessSteps(
                user.getTenantId(), module.getId());
        if (steps != null) {
            for (BusinessModuleProcessStep step : steps) {
                List<BusinessModuleProcessActor> actors = processChainService.listActiveProcessActors(
                        user.getTenantId(), step.getId());
                if (actors == null) {
                    continue;
                }
                for (BusinessModuleProcessActor actor : actors) {
                    if (Boolean.TRUE.equals(actor.getIsRequired())) {
                        requireText(actor.getActorType());
                        requireText(actor.getRequiredOrgCode());
                        requireText(actor.getRequiredRoleCode());
                        return actor;
                    }
                }
            }
        }
        throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
    }

    private DataRequirement buildRequirement(CreateAuthoringLaunchRequest request,
                                             CurrentUserContext.CurrentUser user,
                                             ConnectorSystem system,
                                             BusinessModule module,
                                             ModuleDataStrategy strategy,
                                             TeachingDataTemplate template,
                                             String uniqueId) {
        DataRequirement requirement = new DataRequirement();
        requirement.setId(uniqueId);
        requirement.setTenantId(user.getTenantId());
        requirement.setRequirementCode("AUTHORING_" + uniqueId);
        requirement.setConnectorSystemId(system.getId());
        requirement.setBusinessModuleId(module.getId());
        requirement.setModuleCode(module.getModuleCode());
        requirement.setStrategyId(strategy.getId());
        requirement.setTemplateId(template.getId());
        requirement.setTaskId(request.getLessonId());
        requirement.setSceneType(SCENE_RECORD);
        requirement.setRequirementStatus(RequirementStatus.CREATED.getValue());
        requirement.setCreateBy(user.getUserId());
        requirement.setUpdateBy(user.getUserId());
        return requirement;
    }

    private DataPrepareFacadeService.PrepareAndExecuteRequest buildPrepareRequest(
            DataRequirement requirement,
            String requestBatchId,
            CurrentUserContext.CurrentUser user,
            TeachingDataTemplate template,
            ModuleDataStrategy strategy,
            BusinessModuleProcessActor actor) {
        DataRequirementGenerationService.ParticipantRequirement participant =
                new DataRequirementGenerationService.ParticipantRequirement();
        participant.setStudentId(user.getUserId());
        participant.setSegmentNo(actor.getActorNo() == null ? null : actor.getActorNo().longValue());
        participant.setActorType(actor.getActorType());
        participant.setOwnerExternalOrgId(actor.getRequiredOrgCode());
        participant.setOwnerExternalOrgName(actor.getRequiredOrgName());
        participant.setRequiredExternalOrgId(actor.getRequiredOrgCode());
        participant.setRequiredExternalOrgName(actor.getRequiredOrgName());
        participant.setRequiredExternalRoleId(actor.getRequiredRoleCode());
        participant.setRequiredExternalRoleName(actor.getRequiredRoleName());

        DataRequirementGenerationService.GenerateRequest generateRequest =
                new DataRequirementGenerationService.GenerateRequest();
        generateRequest.setRequirementId(requirement.getId());
        generateRequest.setRequestBatchId(requestBatchId);
        generateRequest.setCreateBy(user.getUserId());
        generateRequest.setUpdateBy(user.getUserId());
        generateRequest.setParticipants(Collections.singletonList(participant));

        DataPrepareFacadeService.PrepareAndExecuteRequest prepareRequest =
                new DataPrepareFacadeService.PrepareAndExecuteRequest();
        prepareRequest.setTriggerType(TriggerType.MANUAL.getValue());
        prepareRequest.setIdempotencyKey("authoring:" + requirement.getId() + ":" + requestBatchId);
        prepareRequest.setTraceId(requestBatchId);
        prepareRequest.setRequestJson(buildPrepareRequestJson(template, strategy));
        prepareRequest.setGenerateRequest(generateRequest);
        return prepareRequest;
    }

    private TeachingDataInstance requirePreparedInstance(DataPrepareJob job,
                                                         String generatedRequestBatchId,
                                                         CurrentUserContext.CurrentUser user) {
        if (job == null || !PrepareJobStatus.SUCCESS.getValue().equals(job.getJobStatus())) {
            String reason = job == null ? null : job.getErrorMessage();
            String message = StringUtils.hasText(reason)
                    ? "业务数据实例创建失败：" + reason
                    : "业务数据实例创建失败";
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED.getCode(), message);
        }
        requireText(job.getId());
        String requestBatchId = StringUtils.hasText(job.getRequestBatchId())
                ? job.getRequestBatchId()
                : generatedRequestBatchId;
        TeachingDataInstance instance = teachingDataInstanceMapper.selectOne(
                new QueryWrapper<TeachingDataInstance>()
                        .eq("tenant_id", user.getTenantId())
                        .eq("prepare_job_id", job.getId())
                        .eq("request_batch_id", requestBatchId)
                        .eq("owner_user_id", user.getUserId())
                        .eq("scene_type", SCENE_RECORD)
                        .eq("validation_status", "PASSED")
                        .eq("instance_status", "READY")
                        .eq("deleted", Boolean.FALSE)
                        .last("limit 1"));
        if (instance == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return instance;
    }

    private PlatformLaunchContext buildLaunchContext(CreateAuthoringLaunchRequest request,
                                                     CurrentUserContext.CurrentUser user,
                                                     ConnectorSystem system,
                                                     BusinessModule module,
                                                     BusinessModuleProcessActor actor,
                                                     TeachingDataInstance instance) {
        PlatformLaunchContext context = new PlatformLaunchContext();
        context.setId(generateId());
        context.setTenantId(user.getTenantId());
        context.setUserId(user.getUserId());
        context.setConnectorSystemId(system.getId());
        context.setTaskId(request.getLessonId());
        context.setDataInstanceId(instance.getId());
        context.setSceneType(SCENE_RECORD);
        context.setSdkMode(SDK_MODE_CAPTURE);
        context.setTargetUrl(module.getEntryUrl());
        context.setSegmentNo(actor.getActorNo() == null ? null : actor.getActorNo().longValue());
        context.setActorType(actor.getActorType());
        context.setRequiredExternalOrgId(actor.getRequiredOrgCode());
        context.setRequiredExternalOrgName(actor.getRequiredOrgName());
        context.setRequiredExternalRoleId(actor.getRequiredRoleCode());
        context.setRequiredExternalRoleName(actor.getRequiredRoleName());
        context.setExternalBusinessId(instance.getExternalBusinessId());
        context.setExternalBusinessNo(instance.getExternalBusinessNo());
        context.setCreateBy(user.getUserId());
        context.setUpdateBy(user.getUserId());
        return context;
    }

    private AuthoringLaunchVO buildResult(ConnectorSystem system,
                                         BusinessModule module,
                                         TeachingDataInstance instance,
                                         PlatformLaunchContextService.CreatedLaunchContext created) {
        PlatformLaunchContext context = created.getLaunchContext();
        AuthoringLaunchVO result = new AuthoringLaunchVO();
        result.setTenantId(context.getTenantId());
        result.setLaunchContextId(context.getId());
        result.setLaunchToken(created.getLaunchToken());
        result.setDataInstanceId(instance.getId());
        result.setRedirectUrl(module.getEntryUrl());
        result.setLaunchUrl(urlBuilder.build(system.getBaseUrl(), context.getTenantId(),
                created.getLaunchToken(), module.getEntryUrl()));
        result.setExpireTime(context.getExpireTime());
        return result;
    }

    private String buildPrepareRequestJson(TeachingDataTemplate template, ModuleDataStrategy strategy) {
        return "{"
                + "\"templateId\":" + jsonValue(template.getId()) + ","
                + "\"templateCode\":" + jsonValue(template.getTemplateCode()) + ","
                + "\"initState\":" + jsonValue(firstText(template.getInitState(), strategy.getInitExternalStatus()))
                + "}";
    }

    private String jsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
