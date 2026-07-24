package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.CreateTeachingDataInstanceRequest;
import com.sxpt.module.connector.dto.ResetTeachingDataInstanceRequest;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.service.TeachingDataInstanceService;
import com.sxpt.module.connector.vo.TeachingDataInstanceVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 教学业务数据实例接口。
 *
 * 业务功能：
 * 1. 提供教学业务数据实例创建入口，保存原平台业务数据引用。
 * 2. 提供按外部业务 ID、使用人和任务查询实例的入口，支撑后续 launchToken 与任务运行上下文。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation。
 * 2. 将 DTO 转换为 TeachingDataInstance 实体，并生成应用层主键。
 * 3. 调用 Service 完成实例写入或查询。
 * 4. 将实体转换为 VO，避免前端依赖数据库 Entity。
 */
@RestController
@RequestMapping("/api/v1/connector/data-instances")
@ConditionalOnProperty(name = "sxpt.connector.data-instance-controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachingDataInstanceController {

    private final TeachingDataInstanceService teachingDataInstanceService;

    public TeachingDataInstanceController(TeachingDataInstanceService teachingDataInstanceService) {
        this.teachingDataInstanceService = teachingDataInstanceService;
    }

    /**
     * 创建教学业务数据实例。
     *
     * @param request 创建教学业务数据实例请求。
     * @return 已创建的教学业务数据实例。
     */
    @PostMapping("/create")
    public ApiResult<TeachingDataInstanceVO> create(@Valid @RequestBody CreateTeachingDataInstanceRequest request) {
        TeachingDataInstance saved = teachingDataInstanceService.createTeachingDataInstance(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 按原平台业务数据 ID 查询教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param externalBusinessId 原平台业务数据 ID。
     * @return 教学业务数据实例。
     */
    @GetMapping("/external")
    public ApiResult<TeachingDataInstanceVO> getByExternalBusiness(@RequestParam String tenantId,
                                                                   @RequestParam String connectorSystemId,
                                                                   @RequestParam String externalBusinessId) {
        TeachingDataInstance instance = teachingDataInstanceService
                .getByExternalBusiness(tenantId, connectorSystemId, externalBusinessId);
        return ApiResult.success(instance == null ? null : toVO(instance));
    }

    /**
     * 查询指定使用人和场景下的教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param ownerUserId 使用人 ID。
     * @param sceneType 场景类型。
     * @return 教学业务数据实例列表。
     */
    @GetMapping("/owner")
    public ApiResult<List<TeachingDataInstanceVO>> listByOwnerAndScene(@RequestParam String tenantId,
                                                                       @RequestParam String ownerUserId,
                                                                       @RequestParam String sceneType) {
        return ApiResult.success(toVOList(teachingDataInstanceService
                .listByOwnerAndScene(tenantId, ownerUserId, sceneType)));
    }

    /**
     * 查询指定任务和场景下的教学业务数据实例。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 场景类型。
     * @return 教学业务数据实例列表。
     */
    @GetMapping("/task")
    public ApiResult<List<TeachingDataInstanceVO>> listByTaskAndScene(@RequestParam String tenantId,
                                                                      @RequestParam String taskId,
                                                                      @RequestParam String sceneType) {
        return ApiResult.success(toVOList(teachingDataInstanceService
                .listByTaskAndScene(tenantId, taskId, sceneType)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建教学业务数据实例请求。
     * @return 教学业务数据实例实体。
     */
    /**
     * 锁定教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @return 已锁定的教学业务数据实例。
     */
    @PostMapping("/{id}/lock")
    public ApiResult<TeachingDataInstanceVO> lock(@PathVariable String id) {
        return ApiResult.success(toVO(teachingDataInstanceService.lockTeachingDataInstance(id)));
    }

    /**
     * 废弃教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @return 已废弃的教学业务数据实例。
     */
    @PostMapping("/{id}/discard")
    public ApiResult<TeachingDataInstanceVO> discard(@PathVariable String id) {
        return ApiResult.success(toVO(teachingDataInstanceService.discardTeachingDataInstance(id)));
    }

    /**
     * 重置教学业务数据实例。
     *
     * @param id 教学业务数据实例 ID。
     * @param request 重置教学业务数据实例请求。
     * @return 已重置的教学业务数据实例。
     */
    @PostMapping("/{id}/reset")
    public ApiResult<TeachingDataInstanceVO> reset(@PathVariable String id,
                                                   @Valid @RequestBody ResetTeachingDataInstanceRequest request) {
        return ApiResult.success(toVO(teachingDataInstanceService.resetTeachingDataInstance(
                id,
                request.getExternalBusinessId(),
                request.getExternalBusinessNo(),
                request.getExternalStatus(),
                request.getMetadataJson())));
    }

    private TeachingDataInstance toEntity(CreateTeachingDataInstanceRequest request) {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId(generateId());
        instance.setTenantId(request.getTenantId());
        instance.setTemplateId(request.getTemplateId());
        instance.setConnectorSystemId(request.getConnectorSystemId());
        instance.setOwnerUserId(request.getOwnerUserId());
        instance.setClassId(request.getClassId());
        instance.setTaskId(request.getTaskId());
        instance.setTeachingPointId(request.getTeachingPointId());
        instance.setExecutionId(request.getExecutionId());
        instance.setAttemptId(request.getAttemptId());
        instance.setSceneType(request.getSceneType());
        instance.setExternalBusinessId(request.getExternalBusinessId());
        instance.setExternalBusinessNo(request.getExternalBusinessNo());
        instance.setExternalStatus(request.getExternalStatus());
        instance.setExpireTime(request.getExpireTime());
        instance.setMetadataJson(request.getMetadataJson());
        return instance;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param instances 教学业务数据实例实体列表。
     * @return 教学业务数据实例展示对象列表。
     */
    private List<TeachingDataInstanceVO> toVOList(List<TeachingDataInstance> instances) {
        List<TeachingDataInstanceVO> result = new ArrayList<>();
        for (TeachingDataInstance instance : instances) {
            result.add(toVO(instance));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param instance 教学业务数据实例实体。
     * @return 教学业务数据实例展示对象。
     */
    private TeachingDataInstanceVO toVO(TeachingDataInstance instance) {
        TeachingDataInstanceVO vo = new TeachingDataInstanceVO();
        vo.setId(instance.getId());
        vo.setTenantId(instance.getTenantId());
        vo.setTemplateId(instance.getTemplateId());
        vo.setConnectorSystemId(instance.getConnectorSystemId());
        vo.setOwnerUserId(instance.getOwnerUserId());
        vo.setClassId(instance.getClassId());
        vo.setTaskId(instance.getTaskId());
        vo.setTeachingPointId(instance.getTeachingPointId());
        vo.setExecutionId(instance.getExecutionId());
        vo.setAttemptId(instance.getAttemptId());
        vo.setSceneType(instance.getSceneType());
        vo.setExternalBusinessId(instance.getExternalBusinessId());
        vo.setExternalBusinessNo(instance.getExternalBusinessNo());
        vo.setExternalStatus(instance.getExternalStatus());
        vo.setInstanceStatus(instance.getInstanceStatus());
        vo.setResetCount(instance.getResetCount());
        vo.setLockTime(instance.getLockTime());
        vo.setExpireTime(instance.getExpireTime());
        vo.setMetadataJson(instance.getMetadataJson());
        vo.setStatus(instance.getStatus());
        vo.setCreateTime(instance.getCreateTime());
        vo.setUpdateTime(instance.getUpdateTime());
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
