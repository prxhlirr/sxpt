package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.PlatformCapabilityRequest;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.service.PlatformCapabilityService;
import com.sxpt.module.connector.vo.PlatformCapabilityVO;
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
 * 原平台能力维护接口。
 *
 * 业务功能：
 * 1. 为管理端提供原平台数据创建、校验、锁定、结果检查和归档能力的维护入口。
 * 2. 让模块策略启用前的能力校验有明确、可审计的配置来源。
 *
 * 关键流程：
 * 1. 接收前端 DTO 并转换为 PlatformCapability 实体。
 * 2. 调用 Service 完成业务校验、持久化和状态切换。
 * 3. 返回 VO，避免前端直接依赖数据库 Entity。
 */
@RestController
@RequestMapping("/api/v1/connector/platform-capabilities")
@ConditionalOnProperty(name = "sxpt.connector.platform-capability-controller.enabled", havingValue = "true", matchIfMissing = true)
public class PlatformCapabilityController {

    private final PlatformCapabilityService platformCapabilityService;

    public PlatformCapabilityController(PlatformCapabilityService platformCapabilityService) {
        this.platformCapabilityService = platformCapabilityService;
    }

    /**
     * 创建原平台能力声明。
     *
     * @param request 原平台能力维护请求。
     * @return 已创建的能力声明。
     */
    @PostMapping("/create")
    public ApiResult<PlatformCapabilityVO> create(@Valid @RequestBody PlatformCapabilityRequest request) {
        PlatformCapability saved = platformCapabilityService.createPlatformCapability(toCreateEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 更新原平台能力声明。
     *
     * @param id 能力 ID。
     * @param request 原平台能力维护请求。
     * @return 已更新的能力声明。
     */
    @PostMapping("/{id}/update")
    public ApiResult<PlatformCapabilityVO> update(@PathVariable String id,
                                                  @Valid @RequestBody PlatformCapabilityRequest request) {
        PlatformCapability saved = platformCapabilityService.updatePlatformCapability(toUpdateEntity(id, request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 启用原平台能力声明。
     *
     * @param id 能力 ID。
     * @return 已启用的能力声明。
     */
    @PostMapping("/{id}/enable")
    public ApiResult<PlatformCapabilityVO> enable(@PathVariable String id) {
        return ApiResult.success(toVO(platformCapabilityService.enablePlatformCapability(id)));
    }

    /**
     * 停用原平台能力声明。
     *
     * @param id 能力 ID。
     * @return 已停用的能力声明。
     */
    @PostMapping("/{id}/disable")
    public ApiResult<PlatformCapabilityVO> disable(@PathVariable String id) {
        return ApiResult.success(toVO(platformCapabilityService.disablePlatformCapability(id)));
    }

    /**
     * 查询原平台能力详情。
     *
     * @param id 能力 ID。
     * @return 能力详情。
     */
    @GetMapping("/{id}")
    public ApiResult<PlatformCapabilityVO> detail(@PathVariable String id) {
        return ApiResult.success(toVO(platformCapabilityService.getPlatformCapabilityById(id)));
    }

    /**
     * 查询某个原平台下的能力声明列表。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @return 能力声明列表。
     */
    @GetMapping
    public ApiResult<List<PlatformCapabilityVO>> list(@RequestParam String tenantId,
                                                      @RequestParam String connectorSystemId) {
        List<PlatformCapability> capabilities = platformCapabilityService.listPlatformCapabilities(
                tenantId, connectorSystemId);
        List<PlatformCapabilityVO> result = new ArrayList<>();
        for (PlatformCapability capability : capabilities) {
            result.add(toVO(capability));
        }
        return ApiResult.success(result);
    }

    /**
     * 将创建请求转换为实体。
     *
     * @param request 原平台能力维护请求。
     * @return 原平台能力实体。
     */
    private PlatformCapability toCreateEntity(PlatformCapabilityRequest request) {
        PlatformCapability capability = toEditableEntity(request);
        capability.setId(UUID.randomUUID().toString().replace("-", ""));
        capability.setTenantId(request.getTenantId());
        capability.setConnectorSystemId(request.getConnectorSystemId());
        capability.setCapabilityCode(request.getCapabilityCode());
        capability.setCreateBy(request.getCreateBy());
        return capability;
    }

    /**
     * 将更新请求转换为实体。
     *
     * @param id 能力 ID。
     * @param request 原平台能力维护请求。
     * @return 原平台能力实体。
     */
    private PlatformCapability toUpdateEntity(String id, PlatformCapabilityRequest request) {
        PlatformCapability capability = toEditableEntity(request);
        capability.setId(id);
        return capability;
    }

    /**
     * 提取能力可编辑字段。
     *
     * @param request 原平台能力维护请求。
     * @return 原平台能力实体。
     */
    private PlatformCapability toEditableEntity(PlatformCapabilityRequest request) {
        PlatformCapability capability = new PlatformCapability();
        capability.setCapabilityName(request.getCapabilityName());
        capability.setCapabilityType(request.getCapabilityType());
        capability.setSupportFlag(request.getSupportFlag());
        capability.setEndpointUrl(request.getEndpointUrl());
        capability.setMethod(request.getMethod());
        capability.setRequestSchemaJson(request.getRequestSchemaJson());
        capability.setResponseSchemaJson(request.getResponseSchemaJson());
        capability.setTimeoutMs(request.getTimeoutMs());
        capability.setRetryPolicyJson(request.getRetryPolicyJson());
        capability.setUpdateBy(request.getUpdateBy());
        return capability;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param capability 原平台能力实体。
     * @return 原平台能力返回对象。
     */
    private PlatformCapabilityVO toVO(PlatformCapability capability) {
        PlatformCapabilityVO vo = new PlatformCapabilityVO();
        vo.setId(capability.getId());
        vo.setTenantId(capability.getTenantId());
        vo.setConnectorSystemId(capability.getConnectorSystemId());
        vo.setCapabilityCode(capability.getCapabilityCode());
        vo.setCapabilityName(capability.getCapabilityName());
        vo.setCapabilityType(capability.getCapabilityType());
        vo.setSupportFlag(capability.getSupportFlag());
        vo.setEndpointUrl(capability.getEndpointUrl());
        vo.setMethod(capability.getMethod());
        vo.setRequestSchemaJson(capability.getRequestSchemaJson());
        vo.setResponseSchemaJson(capability.getResponseSchemaJson());
        vo.setTimeoutMs(capability.getTimeoutMs());
        vo.setRetryPolicyJson(capability.getRetryPolicyJson());
        vo.setStatus(capability.getStatus());
        vo.setCreateTime(capability.getCreateTime());
        vo.setUpdateTime(capability.getUpdateTime());
        return vo;
    }
}
