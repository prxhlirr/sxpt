package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.ExternalConnectorContext;
import com.sxpt.module.connector.dto.BusinessModuleRequest;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.service.BusinessModuleService;
import com.sxpt.module.connector.vo.BusinessModuleVO;
import com.sxpt.module.connector.vo.ConnectorBusinessModuleVO;
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
import java.util.Arrays;

/**
 * 原平台业务模块管理接口。
 *
 * 业务功能：
 * 1. 为后台管理提供业务模块创建、编辑、启停、详情和列表查询入口。
 * 2. 为数据准备策略维护提供稳定的业务模块主数据来源。
 *
 * 关键流程：
 * 1. Controller 接收后台请求并完成 DTO 到 Entity 的转换。
 * 2. Service 统一校验租户、原平台、模块编码、入口和支持场景等核心约束。
 * 3. Controller 将 Entity 转换为 VO，避免前端直接依赖数据库实体。
 */
@RestController
@RequestMapping("/api/v1/connector/business-modules")
@ConditionalOnProperty(name = "sxpt.connector.business-module-controller.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessModuleController {

    private final BusinessModuleService businessModuleService;

    public BusinessModuleController(BusinessModuleService businessModuleService) {
        this.businessModuleService = businessModuleService;
    }

    /**
     * 创建原平台业务模块。
     *
     * @param request 业务模块创建请求。
     * @return 已创建的业务模块。
     */
    @PostMapping("/create")
    public ApiResult<BusinessModuleVO> create(@Valid @RequestBody BusinessModuleRequest request) {
        BusinessModule saved = businessModuleService.createBusinessModule(toCreateEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 更新原平台业务模块。
     *
     * @param id 业务模块 ID。
     * @param request 业务模块更新请求。
     * @return 已更新的业务模块。
     */
    @PostMapping("/{id}/update")
    public ApiResult<BusinessModuleVO> update(@PathVariable String id,
                                              @Valid @RequestBody BusinessModuleRequest request) {
        BusinessModule saved = businessModuleService.updateBusinessModule(toUpdateEntity(id, request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 启用原平台业务模块。
     *
     * @param id 业务模块 ID。
     * @return 已启用的业务模块。
     */
    @PostMapping("/{id}/enable")
    public ApiResult<BusinessModuleVO> enable(@PathVariable String id) {
        BusinessModule businessModule = businessModuleService.enableBusinessModule(id);
        return ApiResult.success(toVO(businessModule));
    }

    /**
     * 禁用原平台业务模块。
     *
     * @param id 业务模块 ID。
     * @return 已禁用的业务模块。
     */
    @PostMapping("/{id}/disable")
    public ApiResult<BusinessModuleVO> disable(@PathVariable String id) {
        BusinessModule businessModule = businessModuleService.disableBusinessModule(id);
        return ApiResult.success(toVO(businessModule));
    }

    /**
     * 查询原平台业务模块详情。
     *
     * @param id 业务模块 ID。
     * @return 业务模块详情。
     */
    @GetMapping("/{id}")
    public ApiResult<BusinessModuleVO> detail(@PathVariable String id) {
        BusinessModule businessModule = businessModuleService.getBusinessModuleById(id);
        return ApiResult.success(toVO(businessModule));
    }

    /**
     * 查询某租户在某原平台下的全部未删除业务模块。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 业务模块列表。
     */
    @GetMapping(headers = "!X-Connector-Key")
    public ApiResult<List<BusinessModuleVO>> list(@RequestParam String tenantId,
                                                  @RequestParam String connectorSystemId) {
        return ApiResult.success(toVOList(businessModuleService.listBusinessModules(tenantId, connectorSystemId)));
    }

    /** OA 使用 Connector Key 查询当前接入可绑定的启用模块。 */
    @GetMapping(headers = "X-Connector-Key")
    public ApiResult<List<ConnectorBusinessModuleVO>> listForConnector() {
        ExternalConnectorContext.require();
        String tenantId = ExternalConnectorContext.require().getSourceSystem().getTenantId();
        String learningSystemId = ExternalConnectorContext.require().getLearningSystem().getId();
        List<ConnectorBusinessModuleVO> result = new ArrayList<>();
        for (BusinessModule module : businessModuleService.listActiveBusinessModules(tenantId, learningSystemId)) {
            ConnectorBusinessModuleVO option = new ConnectorBusinessModuleVO();
            option.setBusinessModuleCode(module.getModuleCode());
            option.setBusinessModuleName(module.getModuleName());
            option.setClassicCaseEnabled(Boolean.TRUE);
            option.setSupportedGenerationModes(Arrays.asList("REPLAY_CASE", "FORMAT_DEMO"));
            result.add(option);
        }
        return ApiResult.success(result);
    }

    /**
     * 查询某租户在某原平台下可用于策略选择的启用业务模块。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 启用业务模块列表。
     */
    @GetMapping("/active")
    public ApiResult<List<BusinessModuleVO>> listActive(@RequestParam String tenantId,
                                                        @RequestParam String connectorSystemId) {
        return ApiResult.success(toVOList(businessModuleService.listActiveBusinessModules(tenantId, connectorSystemId)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 业务模块创建请求。
     * @return 业务模块实体。
     */
    private BusinessModule toCreateEntity(BusinessModuleRequest request) {
        BusinessModule businessModule = toEditableEntity(request);
        businessModule.setId(generateId());
        businessModule.setTenantId(request.getTenantId());
        businessModule.setConnectorSystemId(request.getConnectorSystemId());
        businessModule.setModuleCode(request.getModuleCode());
        return businessModule;
    }

    /**
     * 将更新请求转换为数据库实体。
     *
     * @param id 业务模块 ID。
     * @param request 业务模块更新请求。
     * @return 业务模块实体。
     */
    private BusinessModule toUpdateEntity(String id, BusinessModuleRequest request) {
        BusinessModule businessModule = toEditableEntity(request);
        businessModule.setId(id);
        return businessModule;
    }

    /**
     * 提取创建和更新都允许维护的字段。
     *
     * @param request 业务模块维护请求。
     * @return 包含可编辑字段的业务模块实体。
     */
    private BusinessModule toEditableEntity(BusinessModuleRequest request) {
        BusinessModule businessModule = new BusinessModule();
        businessModule.setModuleName(request.getModuleName());
        businessModule.setExternalModuleId(request.getExternalModuleId());
        businessModule.setEntryUrl(request.getEntryUrl());
        businessModule.setModuleType(request.getModuleType());
        businessModule.setSupportScenes(request.getSupportScenes());
        businessModule.setNeedPreData(request.getNeedPreData());
        businessModule.setDefaultInitialStatus(request.getDefaultInitialStatus());
        businessModule.setDefaultTargetStatus(request.getDefaultTargetStatus());
        businessModule.setCapabilityCodesJson(request.getCapabilityCodesJson());
        businessModule.setDefaultTemplateId(request.getDefaultTemplateId());
        businessModule.setRemark(request.getRemark());
        businessModule.setUpdateBy(request.getUpdateBy());
        return businessModule;
    }

    /**
     * 将实体列表转换为前端返回对象列表。
     *
     * @param businessModules 业务模块实体列表。
     * @return 业务模块返回对象列表。
     */
    private List<BusinessModuleVO> toVOList(List<BusinessModule> businessModules) {
        List<BusinessModuleVO> result = new ArrayList<>();
        for (BusinessModule businessModule : businessModules) {
            result.add(toVO(businessModule));
        }
        return result;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param businessModule 业务模块实体。
     * @return 业务模块返回对象。
     */
    private BusinessModuleVO toVO(BusinessModule businessModule) {
        BusinessModuleVO vo = new BusinessModuleVO();
        vo.setId(businessModule.getId());
        vo.setTenantId(businessModule.getTenantId());
        vo.setConnectorSystemId(businessModule.getConnectorSystemId());
        vo.setModuleCode(businessModule.getModuleCode());
        vo.setModuleName(businessModule.getModuleName());
        vo.setExternalModuleId(businessModule.getExternalModuleId());
        vo.setEntryUrl(businessModule.getEntryUrl());
        vo.setModuleType(businessModule.getModuleType());
        vo.setSupportScenes(businessModule.getSupportScenes());
        vo.setNeedPreData(businessModule.getNeedPreData());
        vo.setDefaultInitialStatus(businessModule.getDefaultInitialStatus());
        vo.setDefaultTargetStatus(businessModule.getDefaultTargetStatus());
        vo.setCapabilityCodesJson(businessModule.getCapabilityCodesJson());
        vo.setDefaultTemplateId(businessModule.getDefaultTemplateId());
        vo.setRemark(businessModule.getRemark());
        vo.setStatus(businessModule.getStatus());
        vo.setCreateTime(businessModule.getCreateTime());
        vo.setUpdateTime(businessModule.getUpdateTime());
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
