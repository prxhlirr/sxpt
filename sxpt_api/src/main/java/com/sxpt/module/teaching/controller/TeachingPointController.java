package com.sxpt.module.teaching.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.teaching.dto.CreateTeachingPointRequest;
import com.sxpt.module.teaching.entity.TeachingPoint;
import com.sxpt.module.teaching.service.TeachingPointService;
import com.sxpt.module.teaching.vo.TeachingPointVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 教学点接口。
 *
 * 业务功能：
 * 1. 提供教师发布教学点的入口，保存业务说明、流程图、备案路径和执行策略。
 * 2. 提供按原平台查询教学点的入口，为后续教学步骤发布和任务发布提供选择清单。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截缺少教学点编码、名称或执行策略的无效发布。
 * 2. 将 DTO 转换为 TeachingPoint 实体并生成应用层主键。
 * 3. 调用 Service 写入已发布教学点，再转换为 VO 返回前端。
 */
@RestController
@RequestMapping("/api/v1/teaching/points")
@ConditionalOnProperty(name = "sxpt.teaching.point-controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachingPointController {

    private final TeachingPointService teachingPointService;

    public TeachingPointController(TeachingPointService teachingPointService) {
        this.teachingPointService = teachingPointService;
    }

    /**
     * 发布教学点。
     *
     * @param request 创建教学点请求。
     * @return 已发布的教学点。
     */
    @PostMapping("/create")
    public ApiResult<TeachingPointVO> create(@Valid @RequestBody CreateTeachingPointRequest request) {
        TeachingPoint saved = teachingPointService.createTeachingPoint(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 按原平台查询教学点。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @return 教学点列表。
     */
    @GetMapping
    public ApiResult<List<TeachingPointVO>> listByConnector(@RequestParam String tenantId,
                                                            @RequestParam String connectorSystemId) {
        return ApiResult.success(toVOList(teachingPointService.listByConnector(tenantId, connectorSystemId)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建教学点请求。
     * @return 教学点实体。
     */
    private TeachingPoint toEntity(CreateTeachingPointRequest request) {
        TeachingPoint teachingPoint = new TeachingPoint();
        teachingPoint.setId(generateId());
        teachingPoint.setTenantId(request.getTenantId());
        teachingPoint.setConnectorSystemId(request.getConnectorSystemId());
        teachingPoint.setPointCode(request.getPointCode());
        teachingPoint.setPointName(request.getPointName());
        teachingPoint.setPointType(request.getPointType());
        teachingPoint.setSourceCaptureSessionId(request.getSourceCaptureSessionId());
        teachingPoint.setBusinessOverviewJson(request.getBusinessOverviewJson());
        teachingPoint.setFlowFileId(request.getFlowFileId());
        teachingPoint.setFlowFileUrl(request.getFlowFileUrl());
        teachingPoint.setRecordPathJson(request.getRecordPathJson());
        teachingPoint.setRequiredExternalRoleId(request.getRequiredExternalRoleId());
        teachingPoint.setRequiredExternalRoleName(request.getRequiredExternalRoleName());
        teachingPoint.setExecutionStrategy(request.getExecutionStrategy());
        teachingPoint.setDefaultGrantStartTime(request.getDefaultGrantStartTime());
        teachingPoint.setDefaultGrantEndTime(request.getDefaultGrantEndTime());
        teachingPoint.setDataScopeJson(request.getDataScopeJson());
        teachingPoint.setOverlayPolicyJson(request.getOverlayPolicyJson());
        teachingPoint.setDescription(request.getDescription());
        teachingPoint.setCreateBy(request.getCreateBy());
        teachingPoint.setUpdateBy(request.getCreateBy());
        return teachingPoint;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param teachingPoints 教学点实体列表。
     * @return 教学点展示对象列表。
     */
    private List<TeachingPointVO> toVOList(List<TeachingPoint> teachingPoints) {
        List<TeachingPointVO> result = new ArrayList<>();
        for (TeachingPoint teachingPoint : teachingPoints) {
            result.add(toVO(teachingPoint));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param teachingPoint 教学点实体。
     * @return 教学点展示对象。
     */
    private TeachingPointVO toVO(TeachingPoint teachingPoint) {
        TeachingPointVO vo = new TeachingPointVO();
        vo.setId(teachingPoint.getId());
        vo.setTenantId(teachingPoint.getTenantId());
        vo.setConnectorSystemId(teachingPoint.getConnectorSystemId());
        vo.setPointCode(teachingPoint.getPointCode());
        vo.setPointName(teachingPoint.getPointName());
        vo.setPointType(teachingPoint.getPointType());
        vo.setVersionNo(teachingPoint.getVersionNo());
        vo.setSourceCaptureSessionId(teachingPoint.getSourceCaptureSessionId());
        vo.setBusinessOverviewJson(teachingPoint.getBusinessOverviewJson());
        vo.setFlowFileId(teachingPoint.getFlowFileId());
        vo.setFlowFileUrl(teachingPoint.getFlowFileUrl());
        vo.setRecordPathJson(teachingPoint.getRecordPathJson());
        vo.setRequiredExternalRoleId(teachingPoint.getRequiredExternalRoleId());
        vo.setRequiredExternalRoleName(teachingPoint.getRequiredExternalRoleName());
        vo.setExecutionStrategy(teachingPoint.getExecutionStrategy());
        vo.setDefaultGrantStartTime(teachingPoint.getDefaultGrantStartTime());
        vo.setDefaultGrantEndTime(teachingPoint.getDefaultGrantEndTime());
        vo.setDataScopeJson(teachingPoint.getDataScopeJson());
        vo.setOverlayPolicyJson(teachingPoint.getOverlayPolicyJson());
        vo.setDescription(teachingPoint.getDescription());
        vo.setPointStatus(teachingPoint.getPointStatus());
        vo.setStatus(teachingPoint.getStatus());
        vo.setCreateTime(teachingPoint.getCreateTime());
        vo.setUpdateTime(teachingPoint.getUpdateTime());
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
