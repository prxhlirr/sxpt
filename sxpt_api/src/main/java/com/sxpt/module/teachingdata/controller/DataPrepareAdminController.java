package com.sxpt.module.teachingdata.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.connector.service.BusinessModuleService;
import com.sxpt.module.connector.service.ConnectorSystemService;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.connector.service.PlatformCapabilityService;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.service.DataInstanceAllocationService;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataPrepareJobService;
import com.sxpt.module.teachingdata.service.DataRequirementItemService;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import com.sxpt.module.teachingdata.service.DataRequirementService;
import com.sxpt.module.teachingdata.service.TeachingDataPoolService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 数据准备后台联调接口。
 *
 * 业务功能：
 * 1. 为前端数据准备管理页提供批次创建、批次查询、明细查询、任务查询和触发准备入口。
 * 2. 暂时暴露教学平台侧最小闭环，帮助通过页面交互发现批次、attempt、单位角色和状态流转问题。
 *
 * 关键流程：
 * 1. 老师或管理员先创建 DataRequirement 批次。
 * 2. 页面提交参与者约束并触发 DataPrepareFacadeService。
 * 3. 页面刷新批次、明细和任务列表，观察幂等、成功、失败和原平台返回字段。
 */
@RestController
@RequestMapping("/api/v1/teaching-data")
@ConditionalOnProperty(name = "sxpt.teaching-data.admin-controller.enabled", havingValue = "true", matchIfMissing = true)
public class DataPrepareAdminController {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final TypeReference<LinkedHashMap<String, Object>> REQUEST_JSON_TYPE =
            new TypeReference<LinkedHashMap<String, Object>>() {
            };

    private final DataRequirementService dataRequirementService;

    private final DataRequirementItemService dataRequirementItemService;

    private final DataPrepareJobService dataPrepareJobService;

    private final DataPrepareFacadeService dataPrepareFacadeService;

    private final TeachingDataPoolService teachingDataPoolService;

    private final DataInstanceAllocationService dataInstanceAllocationService;

    private final ConnectorSystemService connectorSystemService;

    private final PlatformCapabilityService platformCapabilityService;

    private final BusinessModuleService businessModuleService;

    private final TeachingDataTemplateService teachingDataTemplateService;

    private final ModuleDataStrategyService moduleDataStrategyService;

    private final BusinessModuleProcessChainService processChainService;

    public DataPrepareAdminController(DataRequirementService dataRequirementService,
                                      DataRequirementItemService dataRequirementItemService,
                                      DataPrepareJobService dataPrepareJobService,
                                      DataPrepareFacadeService dataPrepareFacadeService,
                                      TeachingDataPoolService teachingDataPoolService,
                                      DataInstanceAllocationService dataInstanceAllocationService,
                                      ConnectorSystemService connectorSystemService,
                                      PlatformCapabilityService platformCapabilityService,
                                      BusinessModuleService businessModuleService,
                                      TeachingDataTemplateService teachingDataTemplateService,
                                      ModuleDataStrategyService moduleDataStrategyService,
                                      BusinessModuleProcessChainService processChainService) {
        this.dataRequirementService = dataRequirementService;
        this.dataRequirementItemService = dataRequirementItemService;
        this.dataPrepareJobService = dataPrepareJobService;
        this.dataPrepareFacadeService = dataPrepareFacadeService;
        this.teachingDataPoolService = teachingDataPoolService;
        this.dataInstanceAllocationService = dataInstanceAllocationService;
        this.connectorSystemService = connectorSystemService;
        this.platformCapabilityService = platformCapabilityService;
        this.businessModuleService = businessModuleService;
        this.teachingDataTemplateService = teachingDataTemplateService;
        this.moduleDataStrategyService = moduleDataStrategyService;
        this.processChainService = processChainService;
    }

    /**
     * 创建数据需求批次。
     *
     * @param requirement 数据需求批次。
     * @return 已创建的数据需求批次。
     */
    @PostMapping("/requirements/create")
    public ApiResult<DataRequirement> createRequirement(@RequestBody DataRequirement requirement) {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        if (!StringUtils.hasText(requirement.getId())) {
            requirement.setId(generateId());
        }
        if (!StringUtils.hasText(requirement.getRequirementCode())) {
            requirement.setRequirementCode("REQ-" + System.currentTimeMillis());
        }
        requirement.setTenantId(currentUser.getTenantId());
        requirement.setCreateBy(currentUser.getUserId());
        requirement.setUpdateBy(currentUser.getUserId());
        return ApiResult.success(dataRequirementService.createDataRequirement(requirement));
    }

    /**
     * 查询指定任务和场景的数据需求批次。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @return 数据需求批次列表。
     */
    @GetMapping("/requirements")
    public ApiResult<List<DataRequirement>> listRequirements(@RequestParam String tenantId,
                                                             @RequestParam String taskId,
                                                             @RequestParam String sceneType) {
        return ApiResult.success(dataRequirementService.listByTaskAndScene(
                CurrentUserContext.getRequiredUser().getTenantId(), taskId, sceneType));
    }

    /**
     * 查询数据需求批次下的需求明细。
     *
     * @param requirementId 数据需求批次 ID。
     * @param tenantId 租户 ID。
     * @return 数据需求明细列表。
     */
    @GetMapping("/requirements/{requirementId}/items")
    public ApiResult<List<DataRequirementItem>> listRequirementItems(@PathVariable String requirementId,
                                                                     @RequestParam String tenantId) {
        return ApiResult.success(dataRequirementItemService.listByRequirement(
                CurrentUserContext.getRequiredUser().getTenantId(), requirementId));
    }

    /**
     * 查询指定任务和场景的数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @return 数据准备任务列表。
     */
    @GetMapping("/jobs")
    public ApiResult<List<DataPrepareJob>> listJobs(@RequestParam String tenantId,
                                                    @RequestParam String taskId,
                                                    @RequestParam String sceneType) {
        return ApiResult.success(dataPrepareJobService.listByTaskAndScene(
                CurrentUserContext.getRequiredUser().getTenantId(), taskId, sceneType));
    }

    /**
     * 数据准备前置链路自检。
     *
     * 业务功能：在真正创建批次和调用原平台前，检查当前任务选择的平台、能力、模块、模板、策略和办理链是否满足造数最小闭环。
     * 关键流程：
     * 1. 使用当前登录用户租户覆盖请求租户，避免跨租户探测配置。
     * 2. 逐项检查 DATA_CREATE 能力、启用模块、启用模板、启用策略、启用流程步骤和步骤参与方。
     * 3. 返回结构化检查项，让前端和后台日志都能定位“状态不允许操作”之前的具体缺口。
     *
     * @param taskId 教学任务 ID。
     * @param tenantId 前端传入租户 ID，仅用于兼容调用，实际以当前登录用户租户为准。
     * @param connectorSystemId 原平台系统 ID。
     * @param businessModuleId 业务模块 ID。
     * @param moduleCode 业务模块编码。
     * @param sceneType 教学场景。
     * @return 数据准备链路自检结果。
     */
    @GetMapping("/tasks/{taskId}/prepare/preflight")
    public ApiResult<DataPreparePreflightResult> preflightTaskPrepare(@PathVariable String taskId,
                                                                      @RequestParam String tenantId,
                                                                      @RequestParam String connectorSystemId,
                                                                      @RequestParam String businessModuleId,
                                                                      @RequestParam String moduleCode,
                                                                      @RequestParam String sceneType) {
        requireText(taskId);
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        return ApiResult.success(buildPreflightResult(
                currentUser.getTenantId(), taskId, connectorSystemId, businessModuleId, moduleCode, sceneType));
    }

    /**
     * 触发数据准备。
     *
     * @param request 数据准备触发请求。
     * @return 执行后的数据准备任务。
     */
    @PostMapping("/prepare/execute")
    public ApiResult<DataPrepareJob> prepareAndExecute(
            @RequestBody DataPrepareFacadeService.PrepareAndExecuteRequest request) {
        applyCurrentUserToPrepareRequest(request);
        return ApiResult.success(dataPrepareFacadeService.prepareAndExecute(request));
    }

    /**
     * 按教学任务触发一次完整的数据准备。
     *
     * 业务功能：
     * 1. 为页面提供“按任务触发”的后端入口，避免前端分别编排创建批次和启动准备两个动作。
     * 2. 以登录用户为租户和审计来源，保证触发入口不会信任前端传入的租户或操作人字段。
     *
     * 关键流程：
     * 1. 校验任务、平台、模块、场景和参与者这些最小可执行上下文。
     * 2. 创建 DataRequirement 批次，让策略绑定、模板绑定和策略快照仍由 DataRequirementService 统一处理。
     * 3. 组装 GenerateRequest 并调用 DataPrepareFacadeService，保证明细生成、原平台造数、数据池落库走同一条主链路。
     *
     * @param taskId 教学任务 ID。
     * @param request 触发数据准备所需的模块、班级、场景和参与者约束。
     * @return 新创建的数据需求批次、准备任务和 requestBatchId，供页面刷新证据链。
     */
    @PostMapping("/tasks/{taskId}/prepare")
    @Transactional(rollbackFor = Exception.class)
    public ApiResult<TaskPrepareTriggerResult> triggerTaskPrepare(@PathVariable String taskId,
                                                                  @RequestBody TriggerTaskPrepareRequest request) {
        validateTaskPrepareRequest(taskId, request);
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        ensureTaskPrepareReady(currentUser.getTenantId(), taskId, request);
        String operatorId = currentUser.getUserId();
        String requestBatchId = StringUtils.hasText(request.getRequestBatchId())
                ? request.getRequestBatchId()
                : "attempt-" + System.currentTimeMillis();
        String effectiveRequestJson = buildTaskPrepareRequestJson(currentUser.getTenantId(), request);

        DataRequirement requirement = new DataRequirement();
        requirement.setId(generateId());
        requirement.setTenantId(currentUser.getTenantId());
        requirement.setRequirementCode(StringUtils.hasText(request.getRequirementCode())
                ? request.getRequirementCode()
                : "REQ-" + System.currentTimeMillis());
        requirement.setConnectorSystemId(request.getConnectorSystemId());
        requirement.setBusinessModuleId(request.getBusinessModuleId());
        requirement.setModuleCode(request.getModuleCode());
        requirement.setStrategyId(request.getStrategyId());
        requirement.setTemplateId(request.getTemplateId());
        requirement.setTaskId(taskId);
        requirement.setClassId(request.getClassId());
        requirement.setSceneType(request.getSceneType());
        requirement.setRemark(request.getRemark());
        requirement.setCreateBy(operatorId);
        requirement.setUpdateBy(operatorId);
        DataRequirement createdRequirement = dataRequirementService.createDataRequirement(requirement);

        DataRequirementGenerationService.GenerateRequest generateRequest =
                new DataRequirementGenerationService.GenerateRequest();
        generateRequest.setRequirementId(createdRequirement.getId());
        generateRequest.setRequestBatchId(requestBatchId);
        generateRequest.setCreateBy(operatorId);
        generateRequest.setUpdateBy(operatorId);
        generateRequest.setParticipants(request.getParticipants());

        DataPrepareFacadeService.PrepareAndExecuteRequest prepareRequest =
                new DataPrepareFacadeService.PrepareAndExecuteRequest();
        prepareRequest.setTriggerType(StringUtils.hasText(request.getTriggerType())
                ? request.getTriggerType()
                : "ON_DEMAND");
        prepareRequest.setIdempotencyKey(StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey()
                : "task-prepare:" + taskId + ":" + request.getSceneType() + ":" + requestBatchId);
        prepareRequest.setTraceId(request.getTraceId());
        prepareRequest.setRequestJson(effectiveRequestJson);
        prepareRequest.setGenerateRequest(generateRequest);

        DataPrepareJob job = dataPrepareFacadeService.prepareAndExecute(prepareRequest);
        return ApiResult.success(new TaskPrepareTriggerResult(createdRequirement, job, requestBatchId));
    }

    /**
     * 人工重试失败的数据准备任务。
     *
     * @param jobId 数据准备任务 ID。
     * @param request 失败任务重试请求。
     * @return 重试执行后的数据准备任务。
     */
    @PostMapping("/prepare/jobs/{jobId}/retry")
    public ApiResult<DataPrepareJob> retryFailedJob(
            @PathVariable String jobId,
            @RequestBody DataPrepareFacadeService.RetryFailedJobRequest request) {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        request.setJobId(jobId);
        request.setTenantId(currentUser.getTenantId());
        request.setUpdateBy(currentUser.getUserId());
        return ApiResult.success(dataPrepareFacadeService.retryFailedJob(request));
    }

    /**
     * 构建数据准备链路自检结果。
     *
     * 业务功能：把造数前必须成立的业务事实压缩成可审计检查项，提前发现运行期会触发 STATE_NOT_ALLOWED 的配置缺口。
     * 关键流程：按平台能力、模块、模板、策略、流程步骤、流程参与方顺序检查；前置节点失败时仍继续检查可独立判断的节点。
     */
    private DataPreparePreflightResult buildPreflightResult(String tenantId,
                                                            String taskId,
                                                            String connectorSystemId,
                                                            String businessModuleId,
                                                            String moduleCode,
                                                            String sceneType) {
        DataPreparePreflightResult result = new DataPreparePreflightResult(taskId);
        ConnectorSystem connectorSystem = findConnectorSystem(tenantId, connectorSystemId);
        addConnectorCheck(result, connectorSystem, connectorSystemId);

        List<PlatformCapability> capabilities =
                platformCapabilityService.listPlatformCapabilities(tenantId, connectorSystemId);
        addDataCreateCapabilityCheck(result, connectorSystem, capabilities);

        BusinessModule businessModule = findActiveBusinessModule(tenantId, connectorSystemId, businessModuleId);
        addBusinessModuleCheck(result, businessModule, businessModuleId, moduleCode);

        List<TeachingDataTemplate> templates = teachingDataTemplateService
                .listActiveTemplatesByModuleAndScene(tenantId, connectorSystemId, moduleCode, sceneType);
        addTemplateCheck(result, templates, moduleCode, sceneType);

        List<ModuleDataStrategy> strategies = moduleDataStrategyService
                .listActiveStrategiesByBusinessModule(tenantId, connectorSystemId, businessModuleId);
        addStrategyCheck(result, strategies, moduleCode, sceneType);

        List<BusinessModuleProcessStep> steps =
                processChainService.listActiveProcessSteps(tenantId, businessModuleId);
        addProcessStepCheck(result, steps);
        addProcessActorCheck(result, tenantId, steps);

        result.finish();
        return result;
    }

    private ConnectorSystem findConnectorSystem(String tenantId, String connectorSystemId) {
        List<ConnectorSystem> systems = connectorSystemService.listConnectorSystemsByTenantId(tenantId);
        for (ConnectorSystem system : systems) {
            if (connectorSystemId.equals(system.getId())) {
                return system;
            }
        }
        return null;
    }

    private BusinessModule findActiveBusinessModule(String tenantId, String connectorSystemId, String businessModuleId) {
        List<BusinessModule> modules = businessModuleService.listActiveBusinessModules(tenantId, connectorSystemId);
        for (BusinessModule module : modules) {
            if (businessModuleId.equals(module.getId())) {
                return module;
            }
        }
        return null;
    }

    private void addConnectorCheck(DataPreparePreflightResult result,
                                   ConnectorSystem connectorSystem,
                                   String connectorSystemId) {
        if (connectorSystem == null) {
            result.addFail("CONNECTOR_SYSTEM", "原平台系统不存在或不属于当前租户", evidence("connectorSystemId", connectorSystemId));
            return;
        }
        result.addPass("CONNECTOR_SYSTEM", "原平台系统已注册", evidence(
                "connectorSystemId", connectorSystem.getId(),
                "systemCode", connectorSystem.getSystemCode(),
                "status", connectorSystem.getStatus()));
    }

    private void addDataCreateCapabilityCheck(DataPreparePreflightResult result,
                                              ConnectorSystem connectorSystem,
                                              List<PlatformCapability> capabilities) {
        PlatformCapability capability = findCapability(capabilities, "DATA_CREATE");
        if (capability == null) {
            result.addFail("DATA_CREATE_CAPABILITY", "原平台未注册或未启用 DATA_CREATE 能力", evidence("capabilityCode", "DATA_CREATE"));
            return;
        }
        if (!Boolean.TRUE.equals(capability.getSupportFlag())) {
            result.addFail("DATA_CREATE_CAPABILITY", "DATA_CREATE 能力声明为不支持", evidence(
                    "capabilityId", capability.getId(),
                    "supportFlag", String.valueOf(capability.getSupportFlag())));
            return;
        }
        if (!StringUtils.hasText(capability.getEndpointUrl())
                && connectorSystem != null
                && !"LOCAL_DEV".equals(connectorSystem.getSystemType())) {
            result.addWarn("DATA_CREATE_CAPABILITY", "DATA_CREATE 能力未配置 endpointUrl，真实原平台对接可能无法发起造数请求", evidence(
                    "capabilityId", capability.getId(),
                    "method", capability.getMethod()));
            return;
        }
        result.addPass("DATA_CREATE_CAPABILITY", "DATA_CREATE 能力可用于造数", evidence(
                "capabilityId", capability.getId(),
                "endpointUrl", capability.getEndpointUrl(),
                "method", capability.getMethod()));
    }

    private PlatformCapability findCapability(List<PlatformCapability> capabilities, String capabilityCode) {
        for (PlatformCapability capability : capabilities) {
            if (capabilityCode.equals(capability.getCapabilityCode())
                    && Boolean.TRUE.equals(capability.getSupportFlag())
                    && "ACTIVE".equals(capability.getStatus())) {
                return capability;
            }
        }
        return null;
    }

    private void addBusinessModuleCheck(DataPreparePreflightResult result,
                                        BusinessModule businessModule,
                                        String businessModuleId,
                                        String moduleCode) {
        if (businessModule == null) {
            result.addFail("BUSINESS_MODULE", "业务模块不存在、未启用或不属于当前原平台", evidence(
                    "businessModuleId", businessModuleId,
                    "moduleCode", moduleCode));
            return;
        }
        if (!moduleCode.equals(businessModule.getModuleCode())) {
            result.addFail("BUSINESS_MODULE", "业务模块 ID 与 moduleCode 不匹配", evidence(
                    "businessModuleId", businessModule.getId(),
                    "expectedModuleCode", businessModule.getModuleCode(),
                    "requestModuleCode", moduleCode));
            return;
        }
        result.addPass("BUSINESS_MODULE", "业务模块已启用且编码匹配", evidence(
                "businessModuleId", businessModule.getId(),
                "moduleCode", businessModule.getModuleCode(),
                "moduleName", businessModule.getModuleName()));
    }

    private void addTemplateCheck(DataPreparePreflightResult result,
                                  List<TeachingDataTemplate> templates,
                                  String moduleCode,
                                  String sceneType) {
        if (templates == null || templates.isEmpty()) {
            result.addFail("DATA_TEMPLATE", "当前模块和场景没有启用的数据模板", evidence(
                    "moduleCode", moduleCode,
                    "sceneType", sceneType));
            return;
        }
        TeachingDataTemplate template = templates.get(0);
        result.addPass("DATA_TEMPLATE", "当前模块和场景存在启用的数据模板", evidence(
                "templateId", template.getId(),
                "templateCode", template.getTemplateCode(),
                "templateName", template.getTemplateName(),
                "templateCount", String.valueOf(templates.size())));
    }

    private void addStrategyCheck(DataPreparePreflightResult result,
                                  List<ModuleDataStrategy> strategies,
                                  String moduleCode,
                                  String sceneType) {
        ModuleDataStrategy matched = null;
        if (strategies != null) {
            for (ModuleDataStrategy strategy : strategies) {
                if (moduleCode.equals(strategy.getModuleCode()) && sceneType.equals(strategy.getSceneType())) {
                    matched = strategy;
                    break;
                }
            }
        }
        if (matched == null) {
            result.addFail("MODULE_DATA_STRATEGY", "当前模块和场景没有启用的数据准备策略", evidence(
                    "moduleCode", moduleCode,
                    "sceneType", sceneType));
            return;
        }
        result.addPass("MODULE_DATA_STRATEGY", "当前模块和场景存在启用的数据准备策略", evidence(
                "strategyId", matched.getId(),
                "strategyCode", matched.getStrategyCode(),
                "templateId", matched.getTemplateId()));
    }

    private void addProcessStepCheck(DataPreparePreflightResult result, List<BusinessModuleProcessStep> steps) {
        if (steps == null || steps.isEmpty()) {
            result.addFail("PROCESS_STEPS", "业务模块没有启用的办理步骤，生成流程快照时会被拒绝", evidence("stepCount", "0"));
            return;
        }
        BusinessModuleProcessStep firstStep = steps.get(0);
        result.addPass("PROCESS_STEPS", "业务模块存在启用的办理步骤", evidence(
                "stepCount", String.valueOf(steps.size()),
                "firstStepCode", firstStep.getStepCode(),
                "firstStepName", firstStep.getStepName()));
    }

    private void addProcessActorCheck(DataPreparePreflightResult result,
                                      String tenantId,
                                      List<BusinessModuleProcessStep> steps) {
        if (steps == null || steps.isEmpty()) {
            result.addFail("PROCESS_ACTORS", "无法检查步骤参与方，因为业务模块没有启用步骤", evidence("blockedBy", "PROCESS_STEPS"));
            return;
        }
        List<String> missingStepCodes = new ArrayList<>();
        int actorCount = 0;
        for (BusinessModuleProcessStep step : steps) {
            List<BusinessModuleProcessActor> actors =
                    processChainService.listActiveProcessActors(tenantId, step.getId());
            if (actors == null || actors.isEmpty()) {
                missingStepCodes.add(step.getStepCode());
            } else {
                actorCount += actors.size();
            }
        }
        if (!missingStepCodes.isEmpty()) {
            result.addFail("PROCESS_ACTORS", "存在启用步骤未配置启用参与方，生成流程快照时会被拒绝", evidence(
                    "missingStepCodes", String.join(",", missingStepCodes),
                    "actorCount", String.valueOf(actorCount)));
            return;
        }
        result.addPass("PROCESS_ACTORS", "所有启用步骤均配置了启用参与方", evidence(
                "stepCount", String.valueOf(steps.size()),
                "actorCount", String.valueOf(actorCount)));
    }

    private Map<String, String> evidence(String key, String value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put(key, value);
        return result;
    }

    private Map<String, String> evidence(String key1, String value1, String key2, String value2) {
        Map<String, String> result = evidence(key1, value1);
        result.put(key2, value2);
        return result;
    }

    private Map<String, String> evidence(String key1,
                                         String value1,
                                         String key2,
                                         String value2,
                                         String key3,
                                         String value3) {
        Map<String, String> result = evidence(key1, value1, key2, value2);
        result.put(key3, value3);
        return result;
    }

    private Map<String, String> evidence(String key1,
                                         String value1,
                                         String key2,
                                         String value2,
                                         String key3,
                                         String value3,
                                         String key4,
                                         String value4) {
        Map<String, String> result = evidence(key1, value1, key2, value2, key3, value3);
        result.put(key4, value4);
        return result;
    }

    /**
     * 查询指定批次下的数据池。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 数据池列表。
     */
    @GetMapping("/pools")
    public ApiResult<List<TeachingDataPool>> listPools(@RequestParam String tenantId,
                                                       @RequestParam String requirementId) {
        return ApiResult.success(teachingDataPoolService.listByRequirement(
                CurrentUserContext.getRequiredUser().getTenantId(), requirementId));
    }

    /**
     * 查询指定批次下的数据实例分配记录。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 当前批次下所有有效领取记录。
     */
    @GetMapping("/allocations")
    public ApiResult<List<DataInstanceAllocation>> listAllocations(@RequestParam String tenantId,
                                                                   @RequestParam String requirementId) {
        return ApiResult.success(dataInstanceAllocationService.listByRequirement(
                CurrentUserContext.getRequiredUser().getTenantId(), requirementId));
    }

    /**
     * 查询指定学生在任务和场景下已经领取的数据分配记录。
     *
     * @param tenantId 租户 ID。
     * @param taskId 教学任务 ID。
     * @param sceneType 教学场景。
     * @param ownerUserId 学生用户 ID。
     * @return 当前学生在该任务和场景下的有效分配记录。
     */
    @GetMapping("/allocations/mine")
    public ApiResult<List<DataInstanceAllocation>> listMyAllocations(@RequestParam String tenantId,
                                                                     @RequestParam String taskId,
                                                                     @RequestParam String sceneType,
                                                                     @RequestParam String ownerUserId) {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        return ApiResult.success(dataInstanceAllocationService.listByOwnerAndScene(
                currentUser.getTenantId(), taskId, sceneType, currentUser.getUserId()));
    }

    /**
     * 从数据池领取一条可用实例。
     *
     * @param request 数据领取请求。
     * @return 分配记录。
     */
    @PostMapping("/allocations/acquire")
    public ApiResult<DataInstanceAllocation> acquire(
            @RequestBody DataInstanceAllocationService.AcquireReadyInstanceRequest request) {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        String ownerUserId = resolveAcquireOwnerUserId(request, currentUser);
        request.setTenantId(currentUser.getTenantId());
        request.setOwnerUserId(ownerUserId);
        request.setCreateBy(currentUser.getUserId());
        request.setUpdateBy(currentUser.getUserId());
        return ApiResult.success(dataInstanceAllocationService.acquireReadyInstance(request));
    }

    /**
     * 解析数据领取归属人。
     *
     * 业务功能：区分教师发布时的学生预分配和学生端自助领取，避免把教师发布的数据错误分配给教师本人。
     * 关键流程：有教学管理角色时保留请求中的学生 ID；普通学生只能领取到自己名下。
     *
     * @param request 当前数据领取请求。
     * @param currentUser 当前认证用户。
     * @return 服务端确认后的领取归属用户 ID。
     */
    private String resolveAcquireOwnerUserId(DataInstanceAllocationService.AcquireReadyInstanceRequest request,
                                             CurrentUserContext.CurrentUser currentUser) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        String requestedOwnerUserId = request.getOwnerUserId();
        if (!StringUtils.hasText(requestedOwnerUserId)) {
            return currentUser.getUserId();
        }
        if (requestedOwnerUserId.equals(currentUser.getUserId())) {
            return requestedOwnerUserId;
        }
        if (canAllocateForOtherUser(currentUser)) {
            return requestedOwnerUserId;
        }
        throw new BusinessException(ApiResultCode.FORBIDDEN);
    }

    /**
     * 判断当前用户是否具备为其他学生预分配数据的教学管理身份。
     *
     * 业务功能：把教师、专家和管理员视为数据准备操作人，学生账号不能伪造 ownerUserId 冒领他人数据。
     * 关键流程：同时兼容角色编码和用户类型，降低历史种子数据或角色编码差异造成的误判。
     *
     * @param currentUser 当前认证用户。
     * @return 是否允许为其他用户领取数据。
     */
    private boolean canAllocateForOtherUser(CurrentUserContext.CurrentUser currentUser) {
        if (currentUser == null) {
            return false;
        }
        if (matchesAnyRole(currentUser.getUserType(), "ADMIN", "SUPER_ADMIN", "MANAGER", "EXPERT", "TEACHER")) {
            return true;
        }
        for (String roleCode : currentUser.getRoleCodes()) {
            if (matchesAnyRole(roleCode, "ADMIN", "SUPER_ADMIN", "MANAGER", "EXPERT", "TEACHER")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断角色文本是否匹配目标角色集合。
     *
     * 业务功能：集中处理大小写差异，避免每个授权分支重复字符串归一化逻辑。
     * 关键流程：空值直接拒绝；非空文本转为大写后做精确匹配。
     *
     * @param value 待判断角色文本。
     * @param expectedRoles 允许的角色编码集合。
     * @return 是否命中允许角色。
     */
    private boolean matchesAnyRole(String value, String... expectedRoles) {
        if (!StringUtils.hasText(value) || expectedRoles == null) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        for (String expectedRole : expectedRoles) {
            if (normalized.equals(expectedRole)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成应用层主键。
     *
     * @return 无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 用可信登录身份覆盖数据准备触发请求中的审计字段，避免前端伪造操作人。
     *
     * @param request 数据准备触发请求。
     */
    private void applyCurrentUserToPrepareRequest(DataPrepareFacadeService.PrepareAndExecuteRequest request) {
        if (request == null || request.getGenerateRequest() == null) {
            return;
        }
        String currentUserId = CurrentUserContext.getRequiredUser().getUserId();
        request.getGenerateRequest().setCreateBy(currentUserId);
        request.getGenerateRequest().setUpdateBy(currentUserId);
    }

    /**
     * 校验按任务触发准备的最小上下文。
     *
     * 业务功能：在进入批次创建之前阻断不可执行请求，避免生成缺少平台、模块或参与者的孤立数据。
     * 关键流程：只校验入口层能判断的必填项，策略和模板有效性继续交给领域服务按真实数据校验。
     *
     * @param taskId 教学任务 ID。
     * @param request 页面提交的数据准备触发请求。
     */
    private void validateTaskPrepareRequest(String taskId, TriggerTaskPrepareRequest request) {
        requireText(taskId);
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getConnectorSystemId());
        requireText(request.getBusinessModuleId());
        requireText(request.getModuleCode());
        requireText(request.getSceneType());
        if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验按任务触发准备的后台配置闭环。
     *
     * 业务功能：在创建批次和启动造数前复用 preflight 事实检查，避免绕过页面自检后生成不可执行的孤立批次。
     * 关键流程：使用当前登录租户重新计算自检结果；只要存在失败节点就返回配置不完整，由调用方先补齐配置。
     *
     * @param tenantId 当前登录用户租户 ID。
     * @param taskId 教学任务 ID。
     * @param request 页面提交的数据准备触发请求。
     */
    private void ensureTaskPrepareReady(String tenantId, String taskId, TriggerTaskPrepareRequest request) {
        DataPreparePreflightResult preflightResult = buildPreflightResult(
                tenantId,
                taskId,
                request.getConnectorSystemId(),
                request.getBusinessModuleId(),
                request.getModuleCode(),
                request.getSceneType());
        if (!preflightResult.isReady()) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
    }

    /**
     * 构造按任务触发造数时的服务端请求快照。
     *
     * 业务功能：把第三方造数文档要求的模板编码、初始状态和追踪号固定到本次 requestJson，
     * 避免前端漏传或伪造运行契约字段导致 HTTP adapter 在运行期拿不到必需参数。
     *
     * 关键流程：先解析前端已有业务参数，再按当前租户、平台、模块和场景选择启用模板，
     * 最后用服务端模板覆盖 requestJson 中的模板快照，并保留原有 bizParams 等业务扩展字段。
     *
     * @param tenantId 当前登录用户所属租户 ID。
     * @param request 页面提交的任务造数触发请求。
     * @return 可直接冻结到批次和任务上的 JSON 快照。
     */
    private String buildTaskPrepareRequestJson(String tenantId, TriggerTaskPrepareRequest request) {
        LinkedHashMap<String, Object> requestJson = parseRequestJson(request.getRequestJson());
        TeachingDataTemplate template = resolveTaskPrepareTemplate(tenantId, request);
        requireTemplateRuntimeText(template.getTemplateCode());
        requireTemplateRuntimeText(template.getInitState());

        LinkedHashMap<String, Object> templateSnapshot = new LinkedHashMap<>();
        templateSnapshot.put("id", template.getId());
        templateSnapshot.put("code", template.getTemplateCode());
        templateSnapshot.put("templateCode", template.getTemplateCode());
        templateSnapshot.put("name", template.getTemplateName());
        templateSnapshot.put("initState", template.getInitState());

        requestJson.put("template", templateSnapshot);
        requestJson.put("templateCode", template.getTemplateCode());
        requestJson.put("initState", template.getInitState());
        if (StringUtils.hasText(request.getTraceId())) {
            requestJson.put("traceId", request.getTraceId());
        }
        return toJson(requestJson);
    }

    /**
     * 解析页面透传的业务 JSON。
     *
     * 业务功能：允许页面继续传递 bizParams 等业务扩展，但要求顶层必须是 JSON 对象，
     * 这样后续服务端可以稳定合并模板快照字段。
     *
     * @param requestJson 页面透传 JSON 文本。
     * @return 可修改的有序 Map。
     */
    private LinkedHashMap<String, Object> parseRequestJson(String requestJson) {
        if (!StringUtils.hasText(requestJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return JSON_MAPPER.readValue(requestJson, REQUEST_JSON_TYPE);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 解析本次按任务触发造数应使用的启用模板。
     *
     * 业务功能：把模板选择收敛在后端可信配置中，支持页面指定 templateId，
     * 未指定时使用当前模块和场景下的第一个启用模板。
     *
     * @param tenantId 当前登录用户所属租户 ID。
     * @param request 页面提交的任务造数触发请求。
     * @return 与当前平台、模块和场景匹配的启用模板。
     */
    private TeachingDataTemplate resolveTaskPrepareTemplate(String tenantId, TriggerTaskPrepareRequest request) {
        List<TeachingDataTemplate> templates = teachingDataTemplateService.listActiveTemplatesByModuleAndScene(
                tenantId, request.getConnectorSystemId(), request.getModuleCode(), request.getSceneType());
        if (templates == null || templates.isEmpty()) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        if (!StringUtils.hasText(request.getTemplateId())) {
            return templates.get(0);
        }
        for (TeachingDataTemplate template : templates) {
            if (request.getTemplateId().equals(template.getId())) {
                return template;
            }
        }
        throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
    }

    /**
     * 校验启用模板的运行时字段非空。
     *
     * 业务功能：模板已被后台启用但缺少造数必需字段时，明确归类为配置不完整，
     * 让页面和排障日志指向后台配置修复，而不是误判为前端参数错误。
     *
     * @param value 模板运行时字段值。
     */
    private void requireTemplateRuntimeText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
    }

    /**
     * 序列化服务端请求快照。
     *
     * 业务功能：将已合并的模板和业务参数写回 requestJson，
     * 供后续批次冻结、任务审计和 HTTP adapter 构造第三方造数请求共同使用。
     *
     * @param value 待序列化对象。
     * @return JSON 文本。
     */
    private String toJson(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 校验文本字段非空。
     *
     * 业务功能：复用控制器入口参数校验，避免空任务或空模块进入后续领域服务。
     * 关键流程：使用 Spring 的文本判定保留与现有服务层一致的空白字符串处理方式。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 按任务触发数据准备的请求体。
     *
     * 业务功能：承载页面触发数据准备所需的教学任务上下文和参与者约束。
     * 关键流程：控制器接收后会覆盖租户和操作人，只使用这里的平台、模块、场景和参与者业务参数。
     */
    /**
     * 业务功能：承载数据准备前置链路自检结果。
     * 关键流程：Controller 逐项追加检查项后调用 finish 生成汇总状态，前端据此决定是否允许继续造数。
     */
    public static class DataPreparePreflightResult {

        private final String taskId;

        private final List<PreflightCheckItem> checks = new ArrayList<>();

        private boolean ready;

        private int passCount;

        private int warnCount;

        private int failCount;

        private String summary;

        public DataPreparePreflightResult(String taskId) {
            this.taskId = taskId;
        }

        public void addPass(String nodeCode, String message, Map<String, String> evidence) {
            checks.add(new PreflightCheckItem(nodeCode, "PASS", message, evidence));
        }

        public void addWarn(String nodeCode, String message, Map<String, String> evidence) {
            checks.add(new PreflightCheckItem(nodeCode, "WARN", message, evidence));
        }

        public void addFail(String nodeCode, String message, Map<String, String> evidence) {
            checks.add(new PreflightCheckItem(nodeCode, "FAIL", message, evidence));
        }

        public void finish() {
            passCount = 0;
            warnCount = 0;
            failCount = 0;
            for (PreflightCheckItem item : checks) {
                if ("PASS".equals(item.getStatus())) {
                    passCount++;
                } else if ("WARN".equals(item.getStatus())) {
                    warnCount++;
                } else if ("FAIL".equals(item.getStatus())) {
                    failCount++;
                }
            }
            ready = failCount == 0;
            summary = ready
                    ? "数据准备前置链路已具备最小闭环"
                    : "数据准备前置链路存在阻塞项，请先补齐失败节点";
        }

        public String getTaskId() {
            return taskId;
        }

        public List<PreflightCheckItem> getChecks() {
            return checks;
        }

        public boolean isReady() {
            return ready;
        }

        public int getPassCount() {
            return passCount;
        }

        public int getWarnCount() {
            return warnCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public String getSummary() {
            return summary;
        }
    }

    /**
     * 业务功能：描述单个自检节点的结果。
     * 关键流程：每个节点保留 nodeCode、状态、业务消息和证据字段，便于页面展示和日志排查使用同一套事实。
     */
    public static class PreflightCheckItem {

        private final String nodeCode;

        private final String status;

        private final String message;

        private final Map<String, String> evidence;

        public PreflightCheckItem(String nodeCode, String status, String message, Map<String, String> evidence) {
            this.nodeCode = nodeCode;
            this.status = status;
            this.message = message;
            this.evidence = evidence;
        }

        public String getNodeCode() {
            return nodeCode;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, String> getEvidence() {
            return evidence;
        }
    }

    public static class TriggerTaskPrepareRequest {

        private String requirementCode;

        private String connectorSystemId;

        private String businessModuleId;

        private String moduleCode;

        private String strategyId;

        private String templateId;

        private String classId;

        private String sceneType;

        private String requestBatchId;

        private String triggerType;

        private String idempotencyKey;

        private String traceId;

        private String requestJson;

        private String remark;

        private List<DataRequirementGenerationService.ParticipantRequirement> participants;

        public String getRequirementCode() {
            return requirementCode;
        }

        public void setRequirementCode(String requirementCode) {
            this.requirementCode = requirementCode;
        }

        public String getConnectorSystemId() {
            return connectorSystemId;
        }

        public void setConnectorSystemId(String connectorSystemId) {
            this.connectorSystemId = connectorSystemId;
        }

        public String getBusinessModuleId() {
            return businessModuleId;
        }

        public void setBusinessModuleId(String businessModuleId) {
            this.businessModuleId = businessModuleId;
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public void setModuleCode(String moduleCode) {
            this.moduleCode = moduleCode;
        }

        public String getStrategyId() {
            return strategyId;
        }

        public void setStrategyId(String strategyId) {
            this.strategyId = strategyId;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public String getClassId() {
            return classId;
        }

        public void setClassId(String classId) {
            this.classId = classId;
        }

        public String getSceneType() {
            return sceneType;
        }

        public void setSceneType(String sceneType) {
            this.sceneType = sceneType;
        }

        public String getRequestBatchId() {
            return requestBatchId;
        }

        public void setRequestBatchId(String requestBatchId) {
            this.requestBatchId = requestBatchId;
        }

        public String getTriggerType() {
            return triggerType;
        }

        public void setTriggerType(String triggerType) {
            this.triggerType = triggerType;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getRequestJson() {
            return requestJson;
        }

        public void setRequestJson(String requestJson) {
            this.requestJson = requestJson;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public List<DataRequirementGenerationService.ParticipantRequirement> getParticipants() {
            return participants;
        }

        public void setParticipants(List<DataRequirementGenerationService.ParticipantRequirement> participants) {
            this.participants = participants;
        }
    }

    /**
     * 按任务触发数据准备的返回结果。
     *
     * 业务功能：把批次和执行任务一起返回给页面，页面可以立即选中批次并刷新证据链。
     * 关键流程：返回 requestBatchId 方便页面将当前 attempt 与后续任务、池和分配记录对齐。
     */
    public static class TaskPrepareTriggerResult {

        private final DataRequirement requirement;

        private final DataPrepareJob job;

        private final String requestBatchId;

        public TaskPrepareTriggerResult(DataRequirement requirement, DataPrepareJob job, String requestBatchId) {
            this.requirement = requirement;
            this.job = job;
            this.requestBatchId = requestBatchId;
        }

        public DataRequirement getRequirement() {
            return requirement;
        }

        public DataPrepareJob getJob() {
            return job;
        }

        public String getRequestBatchId() {
            return requestBatchId;
        }
    }
}
