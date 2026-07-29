package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.teachingdata.dto.CreateStudentDataLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.AllocationStatus;
import com.sxpt.module.teachingdata.mapper.DataInstanceAllocationMapper;
import com.sxpt.module.teachingdata.service.StudentDataLaunchService;
import com.sxpt.module.teachingdata.vo.StudentDataLaunchVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private final PlatformLaunchContextService platformLaunchContextService;

    public StudentDataLaunchServiceImpl(DataInstanceAllocationMapper dataInstanceAllocationMapper,
                                        PlatformLaunchContextService platformLaunchContextService) {
        this.dataInstanceAllocationMapper = dataInstanceAllocationMapper;
        this.platformLaunchContextService = platformLaunchContextService;
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
