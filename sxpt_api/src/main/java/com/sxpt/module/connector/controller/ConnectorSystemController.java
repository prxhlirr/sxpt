package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.CreateConnectorSystemRequest;
import com.sxpt.module.connector.dto.UpdateConnectorSystemRequest;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.service.ConnectorSystemService;
import com.sxpt.module.connector.vo.ConnectorSystemVO;
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
 * 原业务平台配置接口。
 *
 * 业务功能：
 * 1. 提供原业务平台接入配置的创建入口。
 * 2. 为后续 launchToken、SDK 运行上下文、教学数据实例和采集能力提供平台配置基础。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation。
 * 2. 将 DTO 转换为实体，补充应用层主键。
 * 3. 调用 Service 完成业务校验和持久化。
 * 4. 将实体转换为 VO，避免直接返回数据库 Entity。
 */
@RestController
@RequestMapping("/api/v1/connector/system")
@ConditionalOnProperty(name = "sxpt.connector.system-controller.enabled", havingValue = "true", matchIfMissing = true)
public class ConnectorSystemController {

    private final ConnectorSystemService connectorSystemService;

    public ConnectorSystemController(ConnectorSystemService connectorSystemService) {
        this.connectorSystemService = connectorSystemService;
    }

    /**
     * 创建原业务平台配置。
     *
     * @param request 创建原平台配置请求。
     * @return 已创建的原平台配置。
     */
    @PostMapping("/create")
    public ApiResult<ConnectorSystemVO> create(@Valid @RequestBody CreateConnectorSystemRequest request) {
        ConnectorSystem saved = connectorSystemService.createConnectorSystem(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 更新原业务平台配置。
     *
     * @param request 更新原平台配置请求。
     * @return 已更新的原平台配置。
     */
    @PostMapping("/update")
    public ApiResult<ConnectorSystemVO> update(@Valid @RequestBody UpdateConnectorSystemRequest request) {
        ConnectorSystem saved = connectorSystemService.updateConnectorSystem(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 启用原业务平台配置。
     *
     * @param id 原平台配置 ID。
     * @return 已启用的原平台配置。
     */
    @PostMapping("/{id}/enable")
    public ApiResult<ConnectorSystemVO> enable(@PathVariable String id) {
        ConnectorSystem connectorSystem = connectorSystemService.enableConnectorSystem(id);
        return ApiResult.success(toVO(connectorSystem));
    }

    /**
     * 禁用原业务平台配置。
     *
     * @param id 原平台配置 ID。
     * @return 已禁用的原平台配置。
     */
    @PostMapping("/{id}/disable")
    public ApiResult<ConnectorSystemVO> disable(@PathVariable String id) {
        ConnectorSystem connectorSystem = connectorSystemService.disableConnectorSystem(id);
        return ApiResult.success(toVO(connectorSystem));
    }

    /**
     * 查询原业务平台配置详情。
     *
     * @param id 原平台配置 ID。
     * @return 原平台配置详情。
     */
    @GetMapping("/{id}")
    public ApiResult<ConnectorSystemVO> detail(@PathVariable String id) {
        ConnectorSystem connectorSystem = connectorSystemService.getConnectorSystemById(id);
        return ApiResult.success(toVO(connectorSystem));
    }

    /**
     * 查询指定租户下的原业务平台配置列表。
     *
     * @param tenantId 租户 ID。
     * @return 原平台配置列表。
     */
    @GetMapping("/list")
    public ApiResult<List<ConnectorSystemVO>> list(@RequestParam String tenantId) {
        List<ConnectorSystem> connectorSystems = connectorSystemService.listConnectorSystemsByTenantId(tenantId);
        List<ConnectorSystemVO> result = new ArrayList<>();
        for (ConnectorSystem connectorSystem : connectorSystems) {
            result.add(toVO(connectorSystem));
        }
        return ApiResult.success(result);
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建原平台配置请求。
     * @return 原平台配置实体。
     */
    private ConnectorSystem toEntity(CreateConnectorSystemRequest request) {
        ConnectorSystem connectorSystem = new ConnectorSystem();
        connectorSystem.setId(generateId());
        connectorSystem.setTenantId(request.getTenantId());
        connectorSystem.setSystemCode(request.getSystemCode());
        connectorSystem.setSystemName(request.getSystemName());
        connectorSystem.setSystemType(request.getSystemType());
        connectorSystem.setBaseUrl(request.getBaseUrl());
        connectorSystem.setAuthType(request.getAuthType());
        connectorSystem.setConfigJson(request.getConfigJson());
        return connectorSystem;
    }

    /**
     * 将更新请求转换为数据库实体。
     *
     * @param request 更新原平台配置请求。
     * @return 原平台配置实体。
     */
    private ConnectorSystem toEntity(UpdateConnectorSystemRequest request) {
        ConnectorSystem connectorSystem = new ConnectorSystem();
        connectorSystem.setId(request.getId());
        connectorSystem.setSystemName(request.getSystemName());
        connectorSystem.setSystemType(request.getSystemType());
        connectorSystem.setBaseUrl(request.getBaseUrl());
        connectorSystem.setAuthType(request.getAuthType());
        connectorSystem.setConfigJson(request.getConfigJson());
        return connectorSystem;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param connectorSystem 原平台配置实体。
     * @return 原平台配置返回对象。
     */
    private ConnectorSystemVO toVO(ConnectorSystem connectorSystem) {
        ConnectorSystemVO vo = new ConnectorSystemVO();
        vo.setId(connectorSystem.getId());
        vo.setTenantId(connectorSystem.getTenantId());
        vo.setSystemCode(connectorSystem.getSystemCode());
        vo.setSystemName(connectorSystem.getSystemName());
        vo.setSystemType(connectorSystem.getSystemType());
        vo.setBaseUrl(connectorSystem.getBaseUrl());
        vo.setAuthType(connectorSystem.getAuthType());
        vo.setStatus(connectorSystem.getStatus());
        vo.setCreateTime(connectorSystem.getCreateTime());
        vo.setUpdateTime(connectorSystem.getUpdateTime());
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
