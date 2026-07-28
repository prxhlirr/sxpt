package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.ModuleDataStrategyRequest;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.connector.vo.ModuleDataStrategyVO;
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
 * 原平台业务模块数据准备策略管理接口。
 *
 * 业务功能：
 * 1. 为后台管理提供策略创建、编辑、启停、详情和列表查询入口。
 * 2. 为后续数据需求生成提供按模块编码和场景读取启用策略的 HTTP 契约。
 *
 * 关键流程：
 * 1. Controller 接收后台请求并完成 DTO 到 Entity 的转换。
 * 2. Service 统一校验策略最小字段、启用完整性和版本递增规则。
 * 3. Controller 将 Entity 转换为 VO，避免前端直接依赖数据库实体。
 */
@RestController
@RequestMapping("/api/v1/connector/module-data-strategies")
@ConditionalOnProperty(name = "sxpt.connector.module-data-strategy-controller.enabled", havingValue = "true", matchIfMissing = true)
public class ModuleDataStrategyController {

    private final ModuleDataStrategyService moduleDataStrategyService;

    public ModuleDataStrategyController(ModuleDataStrategyService moduleDataStrategyService) {
        this.moduleDataStrategyService = moduleDataStrategyService;
    }

    /**
     * 创建数据准备策略。
     *
     * @param request 策略创建请求。
     * @return 已创建的策略。
     */
    @PostMapping("/create")
    public ApiResult<ModuleDataStrategyVO> create(@Valid @RequestBody ModuleDataStrategyRequest request) {
        ModuleDataStrategy saved = moduleDataStrategyService.createModuleDataStrategy(toCreateEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 更新数据准备策略。
     *
     * @param id 策略 ID。
     * @param request 策略更新请求。
     * @return 已更新的策略。
     */
    @PostMapping("/{id}/update")
    public ApiResult<ModuleDataStrategyVO> update(@PathVariable String id,
                                                  @Valid @RequestBody ModuleDataStrategyRequest request) {
        ModuleDataStrategy saved = moduleDataStrategyService.updateModuleDataStrategy(toUpdateEntity(id, request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 启用数据准备策略。
     *
     * @param id 策略 ID。
     * @return 已启用的策略。
     */
    @PostMapping("/{id}/enable")
    public ApiResult<ModuleDataStrategyVO> enable(@PathVariable String id) {
        ModuleDataStrategy strategy = moduleDataStrategyService.enableModuleDataStrategy(id);
        return ApiResult.success(toVO(strategy));
    }

    /**
     * 禁用数据准备策略。
     *
     * @param id 策略 ID。
     * @return 已禁用的策略。
     */
    @PostMapping("/{id}/disable")
    public ApiResult<ModuleDataStrategyVO> disable(@PathVariable String id) {
        ModuleDataStrategy strategy = moduleDataStrategyService.disableModuleDataStrategy(id);
        return ApiResult.success(toVO(strategy));
    }

    /**
     * 查询策略详情。
     *
     * @param id 策略 ID。
     * @return 策略详情。
     */
    @GetMapping("/{id}")
    public ApiResult<ModuleDataStrategyVO> detail(@PathVariable String id) {
        ModuleDataStrategy strategy = moduleDataStrategyService.getModuleDataStrategyById(id);
        return ApiResult.success(toVO(strategy));
    }

    /**
     * 查询某业务模块下的全部未删除策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 策略列表。
     */
    @GetMapping
    public ApiResult<List<ModuleDataStrategyVO>> list(@RequestParam String tenantId,
                                                      @RequestParam String connectorSystemId,
                                                      @RequestParam String businessModuleId) {
        return ApiResult.success(toVOList(moduleDataStrategyService
                .listStrategiesByBusinessModule(tenantId, connectorSystemId, businessModuleId)));
    }

    /**
     * 查询某业务模块下的启用策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 启用策略列表。
     */
    @GetMapping("/active")
    public ApiResult<List<ModuleDataStrategyVO>> listActive(@RequestParam String tenantId,
                                                            @RequestParam String connectorSystemId,
                                                            @RequestParam String businessModuleId) {
        return ApiResult.success(toVOList(moduleDataStrategyService
                .listActiveStrategiesByBusinessModule(tenantId, connectorSystemId, businessModuleId)));
    }

    /**
     * 查询某模块编码和场景对应的启用策略。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param moduleCode 业务模块编码。
     * @param sceneType 教学场景。
     * @return 启用策略。
     */
    @GetMapping("/active/by-scene")
    public ApiResult<ModuleDataStrategyVO> activeByScene(@RequestParam String tenantId,
                                                         @RequestParam String connectorSystemId,
                                                         @RequestParam String moduleCode,
                                                         @RequestParam String sceneType) {
        return ApiResult.success(toVO(moduleDataStrategyService
                .getActiveStrategyByModuleCodeAndScene(tenantId, connectorSystemId, moduleCode, sceneType)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 策略创建请求。
     * @return 策略实体。
     */
    private ModuleDataStrategy toCreateEntity(ModuleDataStrategyRequest request) {
        ModuleDataStrategy strategy = toEditableEntity(request);
        strategy.setId(generateId());
        strategy.setTenantId(request.getTenantId());
        strategy.setConnectorSystemId(request.getConnectorSystemId());
        strategy.setBusinessModuleId(request.getBusinessModuleId());
        strategy.setModuleCode(request.getModuleCode());
        strategy.setSceneType(request.getSceneType());
        strategy.setCreateBy(request.getCreateBy());
        return strategy;
    }

    /**
     * 将更新请求转换为数据库实体。
     *
     * @param id 策略 ID。
     * @param request 策略更新请求。
     * @return 策略实体。
     */
    private ModuleDataStrategy toUpdateEntity(String id, ModuleDataStrategyRequest request) {
        ModuleDataStrategy strategy = toEditableEntity(request);
        strategy.setId(id);
        return strategy;
    }

    /**
     * 提取创建和更新都允许维护的策略规则字段。
     *
     * @param request 策略维护请求。
     * @return 包含可编辑字段的策略实体。
     */
    private ModuleDataStrategy toEditableEntity(ModuleDataStrategyRequest request) {
        ModuleDataStrategy strategy = new ModuleDataStrategy();
        strategy.setModuleName(request.getModuleName());
        strategy.setNeedPreData(request.getNeedPreData());
        strategy.setDataSourceStrategy(request.getDataSourceStrategy());
        strategy.setInitExternalStatus(request.getInitExternalStatus());
        strategy.setTargetExternalStatus(request.getTargetExternalStatus());
        strategy.setDefaultOrgRolePolicyJson(request.getDefaultOrgRolePolicyJson());
        strategy.setSharePolicy(request.getSharePolicy());
        strategy.setRegeneratePolicy(request.getRegeneratePolicy());
        strategy.setLockPolicy(request.getLockPolicy());
        strategy.setExpirePolicyJson(request.getExpirePolicyJson());
        strategy.setResultCheckPolicyJson(request.getResultCheckPolicyJson());
        strategy.setStrategyCode(request.getStrategyCode());
        strategy.setTemplateId(request.getTemplateId());
        strategy.setPrepareTiming(request.getPrepareTiming());
        strategy.setPoolSizePolicyJson(request.getPoolSizePolicyJson());
        strategy.setValidationPolicyJson(request.getValidationPolicyJson());
        strategy.setArchivePolicyJson(request.getArchivePolicyJson());
        strategy.setUpdateBy(request.getUpdateBy());
        return strategy;
    }

    /**
     * 将实体列表转换为前端返回对象列表。
     *
     * @param strategies 策略实体列表。
     * @return 策略返回对象列表。
     */
    private List<ModuleDataStrategyVO> toVOList(List<ModuleDataStrategy> strategies) {
        List<ModuleDataStrategyVO> result = new ArrayList<>();
        for (ModuleDataStrategy strategy : strategies) {
            result.add(toVO(strategy));
        }
        return result;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param strategy 策略实体。
     * @return 策略返回对象。
     */
    private ModuleDataStrategyVO toVO(ModuleDataStrategy strategy) {
        ModuleDataStrategyVO vo = new ModuleDataStrategyVO();
        vo.setId(strategy.getId());
        vo.setTenantId(strategy.getTenantId());
        vo.setConnectorSystemId(strategy.getConnectorSystemId());
        vo.setBusinessModuleId(strategy.getBusinessModuleId());
        vo.setModuleCode(strategy.getModuleCode());
        vo.setModuleName(strategy.getModuleName());
        vo.setSceneType(strategy.getSceneType());
        vo.setNeedPreData(strategy.getNeedPreData());
        vo.setDataSourceStrategy(strategy.getDataSourceStrategy());
        vo.setInitExternalStatus(strategy.getInitExternalStatus());
        vo.setTargetExternalStatus(strategy.getTargetExternalStatus());
        vo.setDefaultOrgRolePolicyJson(strategy.getDefaultOrgRolePolicyJson());
        vo.setSharePolicy(strategy.getSharePolicy());
        vo.setRegeneratePolicy(strategy.getRegeneratePolicy());
        vo.setLockPolicy(strategy.getLockPolicy());
        vo.setExpirePolicyJson(strategy.getExpirePolicyJson());
        vo.setResultCheckPolicyJson(strategy.getResultCheckPolicyJson());
        vo.setStrategyCode(strategy.getStrategyCode());
        vo.setTemplateId(strategy.getTemplateId());
        vo.setPrepareTiming(strategy.getPrepareTiming());
        vo.setPoolSizePolicyJson(strategy.getPoolSizePolicyJson());
        vo.setValidationPolicyJson(strategy.getValidationPolicyJson());
        vo.setArchivePolicyJson(strategy.getArchivePolicyJson());
        vo.setStrategyVersion(strategy.getStrategyVersion());
        vo.setStatus(strategy.getStatus());
        vo.setCreateTime(strategy.getCreateTime());
        vo.setUpdateTime(strategy.getUpdateTime());
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
