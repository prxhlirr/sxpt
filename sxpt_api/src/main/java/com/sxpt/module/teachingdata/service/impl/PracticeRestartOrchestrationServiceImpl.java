package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.teachingdata.dto.CreateStudentTaskLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.AllocationStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.StrategySceneType;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.TriggerType;
import com.sxpt.module.teachingdata.mapper.DataInstanceAllocationMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.DataInstanceAllocationService;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import com.sxpt.module.teachingdata.service.PracticeRestartOrchestrationService;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.mapper.TeachUserMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/**
 * 学生重新练习编排服务实现。
 *
 * 业务功能：以旧分配记录为可信来源，继承题目、流程、单位、角色和评分上下文，
 * 复用现有数据准备门面重新生成原平台业务数据，并为当前学生领取新的分配记录。
 *
 * 关键流程：先校验 sourceAllocationId 属于当前登录学生且不是考试场景，再读取旧造数明细和模板快照，
 * 构造标准 PrepareAndExecuteRequest，完成造数和领取后把旧分配标记为 RELEASED，
 * 避免学生继续把旧数据当作当前练习入口。
 */
@Service
@Profile("!test")
public class PracticeRestartOrchestrationServiceImpl implements PracticeRestartOrchestrationService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String RELEASE_REASON_RESTARTED = "RESTARTED";

    private final DataInstanceAllocationMapper dataInstanceAllocationMapper;

    private final TeachUserMapper teachUserMapper;

    private final DataRequirementItemMapper dataRequirementItemMapper;

    private final TeachingDataPoolMapper teachingDataPoolMapper;

    private final DataPrepareFacadeService dataPrepareFacadeService;

    private final DataInstanceAllocationService dataInstanceAllocationService;

    private final TeachingDataTemplateService teachingDataTemplateService;

    public PracticeRestartOrchestrationServiceImpl(DataInstanceAllocationMapper dataInstanceAllocationMapper,
                                                   TeachUserMapper teachUserMapper,
                                                   DataRequirementItemMapper dataRequirementItemMapper,
                                                   TeachingDataPoolMapper teachingDataPoolMapper,
                                                   DataPrepareFacadeService dataPrepareFacadeService,
                                                   DataInstanceAllocationService dataInstanceAllocationService,
                                                   TeachingDataTemplateService teachingDataTemplateService) {
        this.dataInstanceAllocationMapper = dataInstanceAllocationMapper;
        this.teachUserMapper = teachUserMapper;
        this.dataRequirementItemMapper = dataRequirementItemMapper;
        this.teachingDataPoolMapper = teachingDataPoolMapper;
        this.dataPrepareFacadeService = dataPrepareFacadeService;
        this.dataInstanceAllocationService = dataInstanceAllocationService;
        this.teachingDataTemplateService = teachingDataTemplateService;
    }

    /**
     * 基于当前登录学生指定的历史分配记录重新生成练习数据。
     *
     * @param request 学生重练请求，必须包含 taskId、sceneType 和 sourceAllocationId。
     * @return 新生成并分配给当前学生的数据分配记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataInstanceAllocation restartCurrentStudentTaskData(CreateStudentTaskLaunchRequest request) {
        validateRestartRequest(request);
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        TeachUser teachUser = resolveTeachUser(currentUser);
        String tenantId = resolveTenantId(currentUser, teachUser);
        DataInstanceAllocation sourceAllocation = getSourceAllocation(request, currentUser, teachUser, tenantId);
        validateRestartPolicy(request, sourceAllocation);
        DataRequirementItem sourceItem = getSourceRequirementItem(sourceAllocation);
        TeachingDataTemplate template = resolveTemplate(tenantId, sourceItem);

        String restartAttemptId = buildRestartAttemptId();
        String requestBatchId = buildRestartRequestBatchId(request, restartAttemptId);
        DataPrepareJob job = dataPrepareFacadeService.prepareAndExecute(
                buildRestartPrepareRequest(
                        sourceAllocation, sourceItem, template, requestBatchId, restartAttemptId, currentUser));
        TeachingDataPool pool = getRestartPool(sourceItem, job, requestBatchId);
        DataInstanceAllocation restarted = dataInstanceAllocationService.acquireReadyInstance(
                buildRestartAcquireRequest(sourceAllocation, request, pool, restartAttemptId, currentUser));
        String restartSnapshotJson = buildRestartAllocationSnapshot(sourceAllocation, restarted, restartAttemptId);
        attachRestartSnapshot(restarted, restartSnapshotJson, currentUser);
        markSourceAllocationRestarted(sourceAllocation, restarted, currentUser);
        return restarted;
    }

    /**
     * 校验重练请求的最小可信字段。
     *
     * @param request 学生重练请求。
     */
    private void validateRestartRequest(CreateStudentTaskLaunchRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTaskId());
        requireText(request.getSceneType());
        requireText(request.getSourceAllocationId());
    }

    /**
     * 按当前登录态读取教学平台用户镜像。
     *
     * @param currentUser 当前认证用户。
     * @return 教学平台用户；本地联调账号可能没有镜像。
     */
    private TeachUser resolveTeachUser(CurrentUserContext.CurrentUser currentUser) {
        if (teachUserMapper == null) {
            return null;
        }
        TeachUser teachUser = teachUserMapper.selectById(currentUser.getUserId());
        if (teachUser == null && StringUtils.hasText(currentUser.getUsername())) {
            teachUser = teachUserMapper.selectOne(new QueryWrapper<TeachUser>()
                    .eq("username", currentUser.getUsername())
                    .eq("deleted", Boolean.FALSE));
        }
        if (teachUser != null && Boolean.TRUE.equals(teachUser.getDeleted())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return teachUser;
    }

    /**
     * 解析当前学生可信租户。
     *
     * @param currentUser 当前认证用户。
     * @param teachUser 教学平台用户镜像。
     * @return 租户 ID。
     */
    private String resolveTenantId(CurrentUserContext.CurrentUser currentUser, TeachUser teachUser) {
        if (teachUser != null && StringUtils.hasText(teachUser.getTenantId())) {
            return teachUser.getTenantId();
        }
        requireText(currentUser.getTenantId());
        return currentUser.getTenantId();
    }

    /**
     * 读取并校验本次重练的来源分配记录。
     *
     * @param request 学生重练请求。
     * @param currentUser 当前认证用户。
     * @param teachUser 教学平台用户镜像。
     * @param tenantId 租户 ID。
     * @return 来源分配记录。
     */
    private DataInstanceAllocation getSourceAllocation(CreateStudentTaskLaunchRequest request,
                                                       CurrentUserContext.CurrentUser currentUser,
                                                       TeachUser teachUser,
                                                       String tenantId) {
        DataInstanceAllocation allocation = dataInstanceAllocationMapper.selectOne(
                new QueryWrapper<DataInstanceAllocation>()
                        .eq("id", request.getSourceAllocationId())
                        .eq("tenant_id", tenantId)
                        .eq("task_id", request.getTaskId())
                        .eq("allocation_scene", request.getSceneType())
                        .eq("deleted", Boolean.FALSE)
                        .last("limit 1"));
        if (allocation == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!buildIdentityCandidates(currentUser, teachUser).contains(allocation.getOwnerUserId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return allocation;
    }

    /**
     * 构造当前学生可能出现在历史分配记录中的身份键。
     *
     * @param currentUser 当前认证用户。
     * @param teachUser 教学平台用户镜像。
     * @return 去重后的身份键列表。
     */
    private List<String> buildIdentityCandidates(CurrentUserContext.CurrentUser currentUser, TeachUser teachUser) {
        List<String> result = new ArrayList<>();
        addIdentityCandidate(result, currentUser.getUserId());
        addIdentityCandidate(result, currentUser.getUsername());
        if (teachUser != null) {
            addIdentityCandidate(result, teachUser.getId());
            addIdentityCandidate(result, teachUser.getStudentNo());
            addIdentityCandidate(result, teachUser.getUsername());
        }
        return result;
    }

    /**
     * 添加非空且未重复的身份键。
     *
     * @param candidates 身份键列表。
     * @param value 待添加值。
     */
    private void addIdentityCandidate(List<String> candidates, String value) {
        if (StringUtils.hasText(value) && !candidates.contains(value)) {
            candidates.add(value);
        }
    }

    /**
     * 校验当前场景是否允许学生自助重练。
     *
     * @param request 学生重练请求。
     * @param sourceAllocation 来源分配记录。
     */
    private void validateRestartPolicy(CreateStudentTaskLaunchRequest request,
                                       DataInstanceAllocation sourceAllocation) {
        if (StrategySceneType.EXAM.getValue().equals(request.getSceneType())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (!AllocationStatus.ALLOCATED.getValue().equals(sourceAllocation.getAllocationStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        requireText(sourceAllocation.getDataInstanceId());
        requireText(sourceAllocation.getRequestItemId());
    }

    /**
     * 读取来源分配记录对应的造数明细。
     *
     * @param allocation 来源分配记录。
     * @return 造数明细。
     */
    private DataRequirementItem getSourceRequirementItem(DataInstanceAllocation allocation) {
        DataRequirementItem item = dataRequirementItemMapper.selectOne(new QueryWrapper<DataRequirementItem>()
                .eq("tenant_id", allocation.getTenantId())
                .eq("request_item_id", allocation.getRequestItemId())
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
        if (item == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        requireText(item.getRequirementId());
        return item;
    }

    /**
     * 解析重练造数应使用的模板。
     *
     * @param tenantId 租户 ID。
     * @param sourceItem 来源造数明细。
     * @return 教学数据模板。
     */
    private TeachingDataTemplate resolveTemplate(String tenantId, DataRequirementItem sourceItem) {
        TeachingDataTemplate template = null;
        if (StringUtils.hasText(sourceItem.getTemplateId())) {
            template = teachingDataTemplateService.getTeachingDataTemplateById(sourceItem.getTemplateId());
        }
        if (template == null) {
            List<TeachingDataTemplate> templates = teachingDataTemplateService.listActiveTemplatesByModuleAndScene(
                    tenantId,
                    sourceItem.getConnectorSystemId(),
                    sourceItem.getModuleCode(),
                    sourceItem.getSceneType());
            if (templates != null && !templates.isEmpty()) {
                template = templates.get(0);
            }
        }
        if (template == null
                || !tenantId.equals(template.getTenantId())
                || !StringUtils.hasText(template.getTemplateCode())
                || !StringUtils.hasText(template.getInitState())) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        return template;
    }

    /**
     * 构造重练造数请求。
     *
     * @param allocation 来源分配记录。
     * @param sourceItem 来源造数明细。
     * @param template 模板快照。
     * @param requestBatchId 新请求批次。
     * @param restartAttemptId 新练习 attempt。
     * @param currentUser 当前认证用户。
     * @return 数据准备门面请求。
     */
    private DataPrepareFacadeService.PrepareAndExecuteRequest buildRestartPrepareRequest(
            DataInstanceAllocation allocation,
            DataRequirementItem sourceItem,
            TeachingDataTemplate template,
            String requestBatchId,
            String restartAttemptId,
            CurrentUserContext.CurrentUser currentUser) {
        DataRequirementGenerationService.GenerateRequest generateRequest =
                new DataRequirementGenerationService.GenerateRequest();
        generateRequest.setRequirementId(sourceItem.getRequirementId());
        generateRequest.setRequestBatchId(requestBatchId);
        generateRequest.setCreateBy(currentUser.getUserId());
        generateRequest.setUpdateBy(currentUser.getUserId());
        generateRequest.setParticipants(Collections.singletonList(
                buildRestartParticipant(allocation, sourceItem, restartAttemptId)));

        DataPrepareFacadeService.PrepareAndExecuteRequest prepareRequest =
                new DataPrepareFacadeService.PrepareAndExecuteRequest();
        prepareRequest.setTriggerType(TriggerType.ON_DEMAND.getValue());
        prepareRequest.setIdempotencyKey(buildRestartIdempotencyKey(allocation, restartAttemptId));
        prepareRequest.setRequestJson(buildRestartRequestJson(allocation, sourceItem, template, restartAttemptId));
        prepareRequest.setGenerateRequest(generateRequest);
        return prepareRequest;
    }

    /**
     * 构造重练造数参与者约束。
     *
     * @param allocation 来源分配记录。
     * @param sourceItem 来源造数明细。
     * @param restartAttemptId 新练习 attempt。
     * @return 重练参与者约束。
     */
    private DataRequirementGenerationService.ParticipantRequirement buildRestartParticipant(
            DataInstanceAllocation allocation,
            DataRequirementItem sourceItem,
            String restartAttemptId) {
        DataRequirementGenerationService.ParticipantRequirement participant =
                new DataRequirementGenerationService.ParticipantRequirement();
        participant.setStudentId(firstText(allocation.getOwnerUserId(), sourceItem.getStudentId()));
        participant.setQuestionId(sourceItem.getQuestionId());
        participant.setExamAttemptId(restartAttemptId);
        participant.setQuestionAttemptId(buildRestartQuestionAttemptId(sourceItem, restartAttemptId));
        participant.setCollaborationUnitId(sourceItem.getCollaborationUnitId());
        participant.setSegmentNo(firstLong(sourceItem.getSegmentNo(), allocation.getSegmentNo()));
        participant.setActorType(firstText(sourceItem.getActorType(), allocation.getActorType()));
        participant.setOwnerExternalOrgId(sourceItem.getOwnerExternalOrgId());
        participant.setOwnerExternalOrgName(sourceItem.getOwnerExternalOrgName());
        participant.setRequiredExternalOrgId(firstText(sourceItem.getRequiredExternalOrgId(),
                allocation.getRequiredExternalOrgId()));
        participant.setRequiredExternalOrgName(firstText(sourceItem.getRequiredExternalOrgName(),
                allocation.getRequiredExternalOrgName()));
        participant.setRequiredExternalRoleId(firstText(sourceItem.getRequiredExternalRoleId(),
                allocation.getRequiredExternalRoleId()));
        participant.setRequiredExternalRoleName(firstText(sourceItem.getRequiredExternalRoleName(),
                allocation.getRequiredExternalRoleName()));
        participant.setDataScopeJson(sourceItem.getDataScopeJson());
        participant.setRequiredActionsJson(sourceItem.getRequiredActionsJson());
        participant.setScorePointSnapshotJson(sourceItem.getScorePointSnapshotJson());
        return participant;
    }

    /**
     * 构造符合第三方造数契约的重练请求快照。
     *
     * @param allocation 来源分配记录。
     * @param sourceItem 来源造数明细。
     * @param template 模板快照。
     * @param restartAttemptId 新练习 attempt。
     * @return JSON 文本。
     */
    private String buildRestartRequestJson(DataInstanceAllocation allocation,
                                           DataRequirementItem sourceItem,
                                           TeachingDataTemplate template,
                                           String restartAttemptId) {
        LinkedHashMap<String, Object> templateSnapshot = new LinkedHashMap<>();
        templateSnapshot.put("id", template.getId());
        templateSnapshot.put("code", template.getTemplateCode());
        templateSnapshot.put("templateCode", template.getTemplateCode());
        templateSnapshot.put("name", template.getTemplateName());
        templateSnapshot.put("initState", template.getInitState());

        LinkedHashMap<String, Object> restartSnapshot = new LinkedHashMap<>();
        restartSnapshot.put("sourceAllocationId", allocation.getId());
        restartSnapshot.put("sourceDataInstanceId", allocation.getDataInstanceId());
        restartSnapshot.put("sourceAttemptId", allocation.getAttemptId());
        restartSnapshot.put("sourceRequestItemId", allocation.getRequestItemId());
        restartSnapshot.put("restartAttemptId", restartAttemptId);

        LinkedHashMap<String, Object> bizParams = new LinkedHashMap<>();
        bizParams.put("sourceExternalBusinessId", allocation.getExternalBusinessId());
        bizParams.put("sourceExternalBusinessNo", allocation.getExternalBusinessName());
        bizParams.put("questionId", sourceItem.getQuestionId());
        bizParams.put("processStepCode", allocation.getProcessStepCode());
        bizParams.put("actorType", firstText(sourceItem.getActorType(), allocation.getActorType()));

        LinkedHashMap<String, Object> requestJson = new LinkedHashMap<>();
        requestJson.put("template", templateSnapshot);
        requestJson.put("templateCode", template.getTemplateCode());
        requestJson.put("initState", template.getInitState());
        requestJson.put("restart", restartSnapshot);
        requestJson.put("bizParams", bizParams);
        return toJson(requestJson);
    }

    /**
     * 查询本次重练新生成的数据池。
     *
     * @param sourceItem 来源造数明细。
     * @param job 数据准备任务。
     * @param requestBatchId 新请求批次。
     * @return 可领取的数据池。
     */
    private TeachingDataPool getRestartPool(DataRequirementItem sourceItem, DataPrepareJob job, String requestBatchId) {
        TeachingDataPool pool = teachingDataPoolMapper.selectOne(new QueryWrapper<TeachingDataPool>()
                .eq("tenant_id", job.getTenantId())
                .eq("requirement_id", sourceItem.getRequirementId())
                .eq("idempotency_key", buildPoolIdempotencyKey(sourceItem, requestBatchId))
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
        if (pool == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return pool;
    }

    /**
     * 构造新数据领取请求。
     *
     * @param allocation 来源分配记录。
     * @param request 学生重练请求。
     * @param pool 新数据池。
     * @param restartAttemptId 新练习 attempt。
     * @param currentUser 当前认证用户。
     * @return 数据领取请求。
     */
    private DataInstanceAllocationService.AcquireReadyInstanceRequest buildRestartAcquireRequest(
            DataInstanceAllocation allocation,
            CreateStudentTaskLaunchRequest request,
            TeachingDataPool pool,
            String restartAttemptId,
            CurrentUserContext.CurrentUser currentUser) {
        DataInstanceAllocationService.AcquireReadyInstanceRequest acquireRequest =
                new DataInstanceAllocationService.AcquireReadyInstanceRequest();
        acquireRequest.setTenantId(allocation.getTenantId());
        acquireRequest.setPoolId(pool.getId());
        acquireRequest.setOwnerUserId(allocation.getOwnerUserId());
        acquireRequest.setTaskId(request.getTaskId());
        acquireRequest.setAllocationScene(request.getSceneType());
        acquireRequest.setAttemptId(restartAttemptId);
        acquireRequest.setQuestionAttemptId(buildRestartQuestionAttemptIdFromPool(pool, restartAttemptId));
        acquireRequest.setCreateBy(currentUser.getUserId());
        acquireRequest.setUpdateBy(currentUser.getUserId());
        return acquireRequest;
    }

    /**
     * 写入新分配记录的重练来源快照。
     *
     * @param restarted 新分配记录。
     * @param restartSnapshotJson 重练来源快照。
     * @param currentUser 当前认证用户。
     */
    private void attachRestartSnapshot(DataInstanceAllocation restarted,
                                       String restartSnapshotJson,
                                       CurrentUserContext.CurrentUser currentUser) {
        restarted.setRequirementSnapshotJson(restartSnapshotJson);
        restarted.setUpdateBy(currentUser.getUserId());
        restarted.setUpdateTime(LocalDateTime.now());
        dataInstanceAllocationMapper.update(null, new UpdateWrapper<DataInstanceAllocation>()
                .eq("id", restarted.getId())
                .eq("tenant_id", restarted.getTenantId())
                .eq("deleted", Boolean.FALSE)
                .set("requirement_snapshot_json", restartSnapshotJson)
                .set("update_by", currentUser.getUserId())
                .set("update_time", restarted.getUpdateTime()));
    }

    /**
     * 将旧分配历史化，避免后续学生启动继续使用旧数据。
     *
     * @param sourceAllocation 来源分配记录。
     * @param restarted 新分配记录。
     * @param currentUser 当前认证用户。
     */
    private void markSourceAllocationRestarted(DataInstanceAllocation sourceAllocation,
                                               DataInstanceAllocation restarted,
                                               CurrentUserContext.CurrentUser currentUser) {
        LocalDateTime now = LocalDateTime.now();
        String reason = RELEASE_REASON_RESTARTED + ":" + restarted.getId();
        int updated = dataInstanceAllocationMapper.update(null, new UpdateWrapper<DataInstanceAllocation>()
                .eq("id", sourceAllocation.getId())
                .eq("tenant_id", sourceAllocation.getTenantId())
                .eq("allocation_status", AllocationStatus.ALLOCATED.getValue())
                .eq("deleted", Boolean.FALSE)
                .set("allocation_status", AllocationStatus.RELEASED.getValue())
                .set("release_time", now)
                .set("release_reason", reason)
                .set("update_by", currentUser.getUserId())
                .set("update_time", now));
        if (updated != 1) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        sourceAllocation.setAllocationStatus(AllocationStatus.RELEASED.getValue());
        sourceAllocation.setReleaseTime(now);
        sourceAllocation.setReleaseReason(reason);
        sourceAllocation.setUpdateBy(currentUser.getUserId());
        sourceAllocation.setUpdateTime(now);
    }

    /**
     * 构造新分配记录上的来源审计快照。
     *
     * @param sourceAllocation 来源分配记录。
     * @param restarted 新分配记录。
     * @param restartAttemptId 新练习 attempt。
     * @return JSON 文本。
     */
    private String buildRestartAllocationSnapshot(DataInstanceAllocation sourceAllocation,
                                                  DataInstanceAllocation restarted,
                                                  String restartAttemptId) {
        LinkedHashMap<String, Object> restart = new LinkedHashMap<>();
        restart.put("restart", Boolean.TRUE);
        restart.put("sourceAllocationId", sourceAllocation.getId());
        restart.put("sourceDataInstanceId", sourceAllocation.getDataInstanceId());
        restart.put("sourceAttemptId", sourceAllocation.getAttemptId());
        restart.put("sourceRequestBatchId", sourceAllocation.getRequestBatchId());
        restart.put("sourceRequestItemId", sourceAllocation.getRequestItemId());
        restart.put("sourceExternalBusinessId", sourceAllocation.getExternalBusinessId());
        restart.put("restartAllocationId", restarted.getId());
        restart.put("restartDataInstanceId", restarted.getDataInstanceId());
        restart.put("restartAttemptId", restartAttemptId);
        return toJson(restart);
    }

    /**
     * 构造重练幂等键。
     *
     * @param allocation 来源分配记录。
     * @param restartAttemptId 新练习 attempt。
     * @return 幂等键。
     */
    private String buildRestartIdempotencyKey(DataInstanceAllocation allocation, String restartAttemptId) {
        return "restart:" + allocation.getTenantId()
                + ":" + allocation.getTaskId()
                + ":" + allocation.getOwnerUserId()
                + ":" + allocation.getId()
                + ":" + restartAttemptId;
    }

    /**
     * 构造数据池幂等键。
     *
     * @param sourceItem 来源造数明细。
     * @param requestBatchId 新请求批次。
     * @return 数据池幂等键。
     */
    private String buildPoolIdempotencyKey(DataRequirementItem sourceItem, String requestBatchId) {
        String key = StringUtils.hasText(sourceItem.getQuestionId()) ? sourceItem.getQuestionId() : "_all";
        return "pool:" + sourceItem.getRequirementId() + ":" + requestBatchId + ":" + key;
    }

    /**
     * 构造新请求批次 ID。
     *
     * @param request 学生重练请求。
     * @param restartAttemptId 新练习 attempt。
     * @return 请求批次 ID。
     */
    private String buildRestartRequestBatchId(CreateStudentTaskLaunchRequest request, String restartAttemptId) {
        return "restart_" + request.getTaskId() + "_" + restartAttemptId;
    }

    /**
     * 构造新练习 attempt。
     *
     * @return 新练习 attempt ID。
     */
    private String buildRestartAttemptId() {
        return "attempt_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 构造新题目作答 attempt。
     *
     * @param sourceItem 来源造数明细。
     * @param restartAttemptId 新练习 attempt。
     * @return 新题目作答 attempt。
     */
    private String buildRestartQuestionAttemptId(DataRequirementItem sourceItem, String restartAttemptId) {
        if (!StringUtils.hasText(sourceItem.getQuestionId())) {
            return restartAttemptId;
        }
        return sourceItem.getQuestionId() + "_" + restartAttemptId;
    }

    /**
     * 按数据池题目构造新题目作答 attempt。
     *
     * @param pool 新数据池。
     * @param restartAttemptId 新练习 attempt。
     * @return 新题目作答 attempt。
     */
    private String buildRestartQuestionAttemptIdFromPool(TeachingDataPool pool, String restartAttemptId) {
        if (!StringUtils.hasText(pool.getQuestionId())) {
            return restartAttemptId;
        }
        return pool.getQuestionId() + "_" + restartAttemptId;
    }

    /**
     * 序列化 JSON。
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
     * 取第一个有内容的文本。
     *
     * @param first 首选文本。
     * @param second 兜底文本。
     * @return 非空文本或 null。
     */
    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    /**
     * 取第一个非空数值。
     *
     * @param first 首选数值。
     * @param second 兜底数值。
     * @return 非空数值或 null。
     */
    private Long firstLong(Long first, Long second) {
        return first != null ? first : second;
    }

    /**
     * 校验文本非空。
     *
     * @param value 待校验文本。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }
}
