package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.teachingdata.dto.CreateStudentDataLaunchRequest;
import com.sxpt.module.teachingdata.dto.CreateStudentTaskLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.AllocationStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.TriggerType;
import com.sxpt.module.teachingdata.mapper.DataInstanceAllocationMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.DataInstanceAllocationService;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import com.sxpt.module.teachingdata.service.StudentDataLaunchService;
import com.sxpt.module.teachingdata.vo.StudentDataLaunchVO;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.mapper.TeachUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 学生数据启动服务实现。
 *
 * 业务功能：
 * 1. 从 data_instance_allocation 读取学生已分配的数据、原平台单位和角色。
 * 2. 生成 platform_launch_context，供原平台通过 launchToken 获取可信办理上下文。
 *
 * 关键流程：
 * 1. 前端提交 allocationId、tenantId 和 studentId。
 * 2. 服务端确认该分配记录属于当前学生且仍处于 ALLOCATED。
 * 3. 服务端把分配记录里的办理链快照写入启动上下文，返回带 token 的跳转地址。
 */
@Service
@Profile("!test")
public class StudentDataLaunchServiceImpl implements StudentDataLaunchService {

    private static final String SDK_MODE_STUDENT = "STUDENT";

    private final DataInstanceAllocationMapper dataInstanceAllocationMapper;

    private final TeachUserMapper teachUserMapper;

    private final DataRequirementItemMapper dataRequirementItemMapper;

    private final TeachingDataPoolMapper teachingDataPoolMapper;

    private final DataPrepareFacadeService dataPrepareFacadeService;

    private final DataInstanceAllocationService dataInstanceAllocationService;

    private final PlatformLaunchContextService platformLaunchContextService;

    @Autowired
    public StudentDataLaunchServiceImpl(DataInstanceAllocationMapper dataInstanceAllocationMapper,
                                        TeachUserMapper teachUserMapper,
                                        DataRequirementItemMapper dataRequirementItemMapper,
                                        TeachingDataPoolMapper teachingDataPoolMapper,
                                        DataPrepareFacadeService dataPrepareFacadeService,
                                        DataInstanceAllocationService dataInstanceAllocationService,
                                        PlatformLaunchContextService platformLaunchContextService) {
        this.dataInstanceAllocationMapper = dataInstanceAllocationMapper;
        this.teachUserMapper = teachUserMapper;
        this.dataRequirementItemMapper = dataRequirementItemMapper;
        this.teachingDataPoolMapper = teachingDataPoolMapper;
        this.dataPrepareFacadeService = dataPrepareFacadeService;
        this.dataInstanceAllocationService = dataInstanceAllocationService;
        this.platformLaunchContextService = platformLaunchContextService;
    }

    public StudentDataLaunchServiceImpl(DataInstanceAllocationMapper dataInstanceAllocationMapper,
                                        PlatformLaunchContextService platformLaunchContextService) {
        this(dataInstanceAllocationMapper, null, null, null, null, null, platformLaunchContextService);
    }

    /**
     * 创建学生进入原平台办理的一次性启动上下文。
     *
     * @param request 学生启动请求。
     * @return 启动结果，包含一次性 token、跳转地址和分配记录快照。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentDataLaunchVO createLaunch(CreateStudentDataLaunchRequest request) {
        validateRequest(request);
        DataInstanceAllocation allocation = getStudentAllocation(request);
        validateAllocation(allocation, request);
        PlatformLaunchContext launchContext = buildLaunchContext(request, allocation);
        PlatformLaunchContextService.CreatedLaunchContext created =
                platformLaunchContextService.createLaunchContext(launchContext);
        return buildResult(created, allocation);
    }

    /**
     * 查询当前登录学生在指定教学任务下已经分配的原平台数据。
     *
     * @param request 学生任务查询请求，前端不允许提交学生身份。
     * @return 当前登录学生可见的数据分配记录。
     */
    @Override
    public List<DataInstanceAllocation> listCurrentStudentTaskAllocations(CreateStudentTaskLaunchRequest request) {
        validateTaskLaunchRequest(request);
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        TeachUser teachUser = resolveTeachUser(currentUser);
        String tenantId = resolveTenantId(teachUser);
        return queryCurrentStudentTaskAllocations(request, currentUser, teachUser, tenantId);
    }

    /**
     * 为当前登录学生重新创建一条初始业务数据并完成新的分配绑定。
     *
     * @param request 学生任务重练请求，前端不允许提交学生身份、单位或角色。
     * @return 新生成并绑定给当前学生的数据分配记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataInstanceAllocation recreateAllocationForCurrentStudentTask(CreateStudentTaskLaunchRequest request) {
        requireRestartDependencies();
        validateTaskLaunchRequest(request);
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        TeachUser teachUser = resolveTeachUser(currentUser);
        String tenantId = resolveTenantId(teachUser);
        DataInstanceAllocation sourceAllocation =
                getCurrentStudentTaskAllocation(request, currentUser, teachUser, tenantId);
        DataRequirementItem sourceItem = getSourceRequirementItem(sourceAllocation);
        String restartAttemptId = buildRestartAttemptId();
        String requestBatchId = buildRestartRequestBatchId(request, restartAttemptId);
        DataPrepareJob job = dataPrepareFacadeService.prepareAndExecute(
                buildRestartPrepareRequest(sourceAllocation, sourceItem, requestBatchId, restartAttemptId, currentUser));
        TeachingDataPool pool = getRestartPool(sourceItem, job, requestBatchId);
        return dataInstanceAllocationService.acquireReadyInstance(
                buildRestartAcquireRequest(sourceAllocation, request, pool, restartAttemptId, currentUser));
    }

    /**
     * 校验重练闭环依赖是否已经注入。
     */
    private void requireRestartDependencies() {
        if (dataRequirementItemMapper == null
                || teachingDataPoolMapper == null
                || dataPrepareFacadeService == null
                || dataInstanceAllocationService == null) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 读取旧分配记录对应的数据准备明细。
     *
     * @param allocation 当前学生已有的分配记录。
     * @return 原始数据准备明细。
     */
    private DataRequirementItem getSourceRequirementItem(DataInstanceAllocation allocation) {
        requireText(allocation.getRequestItemId());
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
     * 构造重练触发数据准备请求。
     *
     * @param allocation 旧分配记录。
     * @param sourceItem 旧数据准备明细。
     * @param requestBatchId 新请求批次。
     * @param restartAttemptId 新练习次数标识。
     * @param currentUser 当前登录用户。
     * @return 数据准备门面请求。
     */
    private DataPrepareFacadeService.PrepareAndExecuteRequest buildRestartPrepareRequest(
            DataInstanceAllocation allocation,
            DataRequirementItem sourceItem,
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
        prepareRequest.setIdempotencyKey("restart:" + sourceItem.getRequirementId() + ":" + requestBatchId);
        prepareRequest.setRequestJson(buildRestartRequestJson(allocation, restartAttemptId));
        prepareRequest.setGenerateRequest(generateRequest);
        return prepareRequest;
    }

    /**
     * 构造重练数据准备参与方约束。
     *
     * @param allocation 旧分配记录。
     * @param sourceItem 旧数据准备明细。
     * @param restartAttemptId 新练习次数标识。
     * @return 新数据准备参与方约束。
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
        participant.setSegmentNo(sourceItem.getSegmentNo());
        participant.setActorType(sourceItem.getActorType());
        participant.setOwnerExternalOrgId(sourceItem.getOwnerExternalOrgId());
        participant.setOwnerExternalOrgName(sourceItem.getOwnerExternalOrgName());
        participant.setRequiredExternalOrgId(sourceItem.getRequiredExternalOrgId());
        participant.setRequiredExternalOrgName(sourceItem.getRequiredExternalOrgName());
        participant.setRequiredExternalRoleId(sourceItem.getRequiredExternalRoleId());
        participant.setRequiredExternalRoleName(sourceItem.getRequiredExternalRoleName());
        participant.setDataScopeJson(sourceItem.getDataScopeJson());
        participant.setRequiredActionsJson(sourceItem.getRequiredActionsJson());
        participant.setScorePointSnapshotJson(sourceItem.getScorePointSnapshotJson());
        return participant;
    }

    /**
     * 查询本次重练新生成的数据池。
     *
     * @param sourceItem 旧数据准备明细。
     * @param job 本次数据准备任务。
     * @param requestBatchId 新请求批次。
     * @return 可用于领取的数据池。
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
     * 构造重练数据领取请求。
     *
     * @param allocation 旧分配记录。
     * @param request 学生任务重练请求。
     * @param pool 本次重练数据池。
     * @param restartAttemptId 新练习次数标识。
     * @param currentUser 当前登录用户。
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
     * 构造数据池幂等键。
     *
     * @param sourceItem 旧数据准备明细。
     * @param requestBatchId 新请求批次。
     * @return 与编排服务一致的数据池幂等键。
     */
    private String buildPoolIdempotencyKey(DataRequirementItem sourceItem, String requestBatchId) {
        String key = StringUtils.hasText(sourceItem.getQuestionId()) ? sourceItem.getQuestionId() : "_all";
        return "pool:" + sourceItem.getRequirementId() + ":" + requestBatchId + ":" + key;
    }

    /**
     * 构造重练请求批次 ID。
     *
     * @param request 学生任务重练请求。
     * @param restartAttemptId 新练习次数标识。
     * @return 新请求批次 ID。
     */
    private String buildRestartRequestBatchId(CreateStudentTaskLaunchRequest request, String restartAttemptId) {
        return "restart_" + request.getTaskId() + "_" + restartAttemptId;
    }

    /**
     * 构造重练次数标识。
     *
     * @return 新练习次数标识。
     */
    private String buildRestartAttemptId() {
        return "attempt_" + generateId();
    }

    /**
     * 构造新的题目作答次数标识。
     *
     * @param sourceItem 旧数据准备明细。
     * @param restartAttemptId 新练习次数标识。
     * @return 新题目作答次数标识。
     */
    private String buildRestartQuestionAttemptId(DataRequirementItem sourceItem, String restartAttemptId) {
        if (!StringUtils.hasText(sourceItem.getQuestionId())) {
            return restartAttemptId;
        }
        return sourceItem.getQuestionId() + "_" + restartAttemptId;
    }

    /**
     * 构造分配记录中的题目作答次数标识。
     *
     * @param pool 本次重练数据池。
     * @param restartAttemptId 新练习次数标识。
     * @return 新题目作答次数标识。
     */
    private String buildRestartQuestionAttemptIdFromPool(TeachingDataPool pool, String restartAttemptId) {
        if (!StringUtils.hasText(pool.getQuestionId())) {
            return restartAttemptId;
        }
        return pool.getQuestionId() + "_" + restartAttemptId;
    }

    /**
     * 构造重练审计请求摘要。
     *
     * @param allocation 旧分配记录。
     * @param restartAttemptId 新练习次数标识。
     * @return JSON 字符串。
     */
    private String buildRestartRequestJson(DataInstanceAllocation allocation, String restartAttemptId) {
        return "{"
                + "\"sourceAllocationId\":" + jsonValue(allocation.getId()) + ","
                + "\"sourceDataInstanceId\":" + jsonValue(allocation.getDataInstanceId()) + ","
                + "\"restartAttemptId\":" + jsonValue(restartAttemptId)
                + "}";
    }

    /**
     * 按当前登录学生和教学任务创建原平台启动上下文。
     *
     * @param request 学生任务启动请求。
     * @return 启动结果，包含一次性 token、跳转地址和分配记录快照。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentDataLaunchVO createLaunchForCurrentStudentTask(CreateStudentTaskLaunchRequest request) {
        validateTaskLaunchRequest(request);
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        TeachUser teachUser = resolveTeachUser(currentUser);
        String tenantId = resolveTenantId(teachUser);
        DataInstanceAllocation allocation = getCurrentStudentTaskAllocation(request, currentUser, teachUser, tenantId);
        CreateStudentDataLaunchRequest launchRequest = new CreateStudentDataLaunchRequest();
        launchRequest.setTenantId(tenantId);
        launchRequest.setAllocationId(allocation.getId());
        launchRequest.setStudentId(allocation.getOwnerUserId());
        launchRequest.setExecutionId(firstText(request.getExecutionId(),
                firstText(allocation.getExecutionId(), allocation.getAttemptId())));
        validateAllocation(allocation, launchRequest);
        PlatformLaunchContext launchContext = buildLaunchContext(launchRequest, allocation);
        PlatformLaunchContextService.CreatedLaunchContext created =
                platformLaunchContextService.createLaunchContext(launchContext);
        return buildResult(created, allocation);
    }

    /**
     * 校验学生任务启动请求的最小字段。
     *
     * @param request 学生任务启动请求。
     */
    private void validateTaskLaunchRequest(CreateStudentTaskLaunchRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTaskId());
        requireText(request.getSceneType());
    }

    /**
     * 按认证用户读取教学平台用户镜像。
     *
     * @param currentUser 当前认证用户。
     * @return 教学平台用户；本地联调账号可能为空。
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
     * 解析可信租户 ID。
     *
     * @param teachUser 教学平台用户。
     * @return 租户 ID。
     */
    private String resolveTenantId(TeachUser teachUser) {
        if (teachUser != null && StringUtils.hasText(teachUser.getTenantId())) {
            return teachUser.getTenantId();
        }
        return "demo-tenant";
    }

    /**
     * 查询当前学生在指定任务和场景下的已分配数据。
     *
     * @param request 学生任务启动请求。
     * @param currentUser 当前认证用户。
     * @param teachUser 教学平台用户。
     * @param tenantId 租户 ID。
     * @return 数据实例分配记录。
     */
    private DataInstanceAllocation getCurrentStudentTaskAllocation(CreateStudentTaskLaunchRequest request,
                                                                  CurrentUserContext.CurrentUser currentUser,
                                                                  TeachUser teachUser,
                                                                  String tenantId) {
        List<DataInstanceAllocation> allocations =
                queryCurrentStudentTaskAllocations(request, currentUser, teachUser, tenantId);
        if (allocations.isEmpty()) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return allocations.get(0);
    }

    /**
     * 按当前登录学生身份查询任务分配记录。
     *
     * @param request 学生任务查询请求。
     * @param currentUser 当前认证用户。
     * @param teachUser 教学平台用户。
     * @param tenantId 租户 ID。
     * @return 当前学生可见的数据分配记录。
     */
    private List<DataInstanceAllocation> queryCurrentStudentTaskAllocations(CreateStudentTaskLaunchRequest request,
                                                                           CurrentUserContext.CurrentUser currentUser,
                                                                           TeachUser teachUser,
                                                                           String tenantId) {
        List<String> identityCandidates = buildIdentityCandidates(currentUser, teachUser);
        List<DataInstanceAllocation> allocations = dataInstanceAllocationMapper.selectList(
                new QueryWrapper<DataInstanceAllocation>()
                        .eq("tenant_id", tenantId)
                        .eq("task_id", request.getTaskId())
                        .eq("allocation_scene", request.getSceneType())
                        .eq("allocation_status", AllocationStatus.ALLOCATED.getValue())
                        .eq("deleted", Boolean.FALSE)
                        .and(wrapper -> wrapper.in("owner_user_id", identityCandidates)
                                .or()
                                .in("student_id", identityCandidates))
                        .orderByAsc("question_attempt_id")
                        .orderByAsc("segment_no")
                        .orderByDesc("allocate_time"));
        return allocations == null ? new ArrayList<>() : allocations;
    }

    /**
     * 构造当前学生可能出现于历史分配记录中的身份键。
     *
     * @param currentUser 当前认证用户。
     * @param teachUser 教学平台用户。
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
     * 校验学生启动请求的最小字段。
     *
     * @param request 学生启动请求。
     */
    private void validateRequest(CreateStudentDataLaunchRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getAllocationId());
        requireText(request.getStudentId());
    }

    /**
     * 按租户和分配记录读取未删除数据，防止跨租户读取。
     *
     * @param request 学生启动请求。
     * @return 数据实例分配记录。
     */
    private DataInstanceAllocation getStudentAllocation(CreateStudentDataLaunchRequest request) {
        DataInstanceAllocation allocation = dataInstanceAllocationMapper.selectOne(
                new QueryWrapper<DataInstanceAllocation>()
                        .eq("id", request.getAllocationId())
                        .eq("tenant_id", request.getTenantId())
                        .eq("deleted", Boolean.FALSE));
        if (allocation == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return allocation;
    }

    /**
     * 校验分配记录是否仍可用于学生进入原平台。
     *
     * @param allocation 分配记录。
     * @param request 学生启动请求。
     */
    private void validateAllocation(DataInstanceAllocation allocation, CreateStudentDataLaunchRequest request) {
        if (!request.getStudentId().equals(allocation.getOwnerUserId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        if (!AllocationStatus.ALLOCATED.getValue().equals(allocation.getAllocationStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        requireText(allocation.getConnectorSystemId());
        requireText(allocation.getDataInstanceId());
        requireText(allocation.getTaskId());
        requireText(allocation.getAllocationScene());
        requireText(allocation.getTargetUrl());
        requireText(firstText(allocation.getOriginOrgId(), allocation.getRequiredExternalOrgId()));
        requireText(firstText(allocation.getOriginRoleId(), allocation.getRequiredExternalRoleId()));
    }

    /**
     * 基于分配记录构造原平台启动上下文。
     *
     * @param request 学生启动请求。
     * @param allocation 分配记录。
     * @return 原平台启动上下文。
     */
    private PlatformLaunchContext buildLaunchContext(CreateStudentDataLaunchRequest request,
                                                     DataInstanceAllocation allocation) {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId(generateId());
        launchContext.setTenantId(request.getTenantId());
        launchContext.setUserId(request.getStudentId());
        launchContext.setConnectorSystemId(allocation.getConnectorSystemId());
        launchContext.setTaskId(allocation.getTaskId());
        launchContext.setExecutionId(firstText(request.getExecutionId(),
                firstText(allocation.getExecutionId(), allocation.getAttemptId())));
        launchContext.setDataInstanceId(allocation.getDataInstanceId());
        launchContext.setSceneType(allocation.getAllocationScene());
        launchContext.setSdkMode(SDK_MODE_STUDENT);
        launchContext.setTargetUrl(allocation.getTargetUrl());
        launchContext.setSegmentNo(resolveSegmentNo(allocation));
        launchContext.setActorType(allocation.getActorType());
        launchContext.setRequiredExternalOrgId(firstText(allocation.getOriginOrgId(),
                allocation.getRequiredExternalOrgId()));
        launchContext.setRequiredExternalOrgName(firstText(allocation.getOriginOrgName(),
                allocation.getRequiredExternalOrgName()));
        launchContext.setRequiredExternalRoleId(firstText(allocation.getOriginRoleId(),
                allocation.getRequiredExternalRoleId()));
        launchContext.setRequiredExternalRoleName(firstText(allocation.getOriginRoleName(),
                allocation.getRequiredExternalRoleName()));
        launchContext.setExternalBusinessId(allocation.getExternalBusinessId());
        launchContext.setExternalBusinessNo(allocation.getExternalBusinessName());
        launchContext.setDataScopeJson(buildDataScopeJson(allocation));
        launchContext.setCreateBy(request.getStudentId());
        launchContext.setUpdateBy(request.getStudentId());
        return launchContext;
    }

    /**
     * 解析当前办理参与方序号。
     *
     * @param allocation 分配记录。
     * @return 启动上下文中的片段序号。
     */
    private Long resolveSegmentNo(DataInstanceAllocation allocation) {
        if (allocation.getProcessActorNo() != null) {
            return allocation.getProcessActorNo().longValue();
        }
        return allocation.getSegmentNo();
    }

    /**
     * 构造下发给原平台 SDK 的数据范围快照。
     *
     * @param allocation 分配记录。
     * @return JSON 字符串。
     */
    private String buildDataScopeJson(DataInstanceAllocation allocation) {
        return "{"
                + "\"allocationId\":" + jsonValue(allocation.getId()) + ","
                + "\"requestBatchId\":" + jsonValue(allocation.getRequestBatchId()) + ","
                + "\"requestItemId\":" + jsonValue(allocation.getRequestItemId()) + ","
                + "\"businessModuleId\":" + jsonValue(allocation.getBusinessModuleId()) + ","
                + "\"processStepCode\":" + jsonValue(allocation.getProcessStepCode()) + ","
                + "\"processStepName\":" + jsonValue(allocation.getProcessStepName()) + ","
                + "\"processActorNo\":" + jsonValue(allocation.getProcessActorNo() == null
                ? null : String.valueOf(allocation.getProcessActorNo())) + ","
                + "\"actorRelation\":" + jsonValue(allocation.getActorRelation()) + ","
                + "\"actorSnapshotJson\":" + jsonValue(allocation.getActorSnapshotJson()) + ","
                + "\"requirementSnapshotJson\":" + jsonValue(allocation.getRequirementSnapshotJson())
                + "}";
    }

    /**
     * 组装学生端启动返回结果。
     *
     * @param created 已创建的启动上下文和明文 token。
     * @param allocation 分配记录。
     * @return 学生端启动结果。
     */
    private StudentDataLaunchVO buildResult(PlatformLaunchContextService.CreatedLaunchContext created,
                                            DataInstanceAllocation allocation) {
        PlatformLaunchContext launchContext = created.getLaunchContext();
        StudentDataLaunchVO result = new StudentDataLaunchVO();
        result.setLaunchContextId(launchContext.getId());
        result.setLaunchToken(created.getLaunchToken());
        result.setTargetUrl(launchContext.getTargetUrl());
        result.setLaunchUrl(appendLaunchQuery(launchContext.getTargetUrl(),
                launchContext.getTenantId(), launchContext.getId(), created.getLaunchToken()));
        result.setExpireTime(launchContext.getExpireTime());
        result.setAllocation(allocation);
        return result;
    }

    /**
     * 给原平台地址追加启动参数。
     *
     * @param targetUrl 原平台目标地址。
     * @param tenantId 租户 ID。
     * @param launchContextId 启动上下文 ID。
     * @param launchToken 一次性明文 token。
     * @return 可直接跳转的原平台地址。
     */
    private String appendLaunchQuery(String targetUrl, String tenantId, String launchContextId, String launchToken) {
        String separator = targetUrl.contains("?") ? "&" : "?";
        return targetUrl + separator
                + "tenantId=" + encode(tenantId)
                + "&launchContextId=" + encode(launchContextId)
                + "&launchToken=" + encode(launchToken);
    }

    /**
     * URL 参数编码。
     *
     * @param value 原始参数值。
     * @return 编码后的参数值。
     */
    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
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
     * JSON 字符串字段转义。
     *
     * @param value 原始文本。
     * @return JSON 字符串值或 null。
     */
    private String jsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
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
     * @return 无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
