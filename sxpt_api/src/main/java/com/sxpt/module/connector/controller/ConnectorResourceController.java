package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.CreateConnectorResourceRequest;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.service.ConnectorResourceService;
import com.sxpt.module.connector.vo.ConnectorResourceVO;
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
 * 原平台正式资源接口。
 *
 * 业务功能：
 * 1. 提供关键页面资源沉淀为正式资源的入口。
 * 2. 提供按页面查询正式资源的入口，为 SDK runtime 和教学步骤配置提供资源清单。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截没有稳定编码的资源。
 * 2. 将 DTO 转换为 ConnectorResource 实体并生成应用层主键。
 * 3. 调用 Service 写入正式资源，再转换为 VO 返回前端。
 */
@RestController
@RequestMapping("/api/v1/connector/resources")
@ConditionalOnProperty(name = "sxpt.connector.resource-controller.enabled", havingValue = "true", matchIfMissing = true)
public class ConnectorResourceController {

    private final ConnectorResourceService connectorResourceService;

    public ConnectorResourceController(ConnectorResourceService connectorResourceService) {
        this.connectorResourceService = connectorResourceService;
    }

    /**
     * 创建原平台正式资源。
     *
     * @param request 创建正式资源请求。
     * @return 已保存的正式资源。
     */
    @PostMapping("/create")
    public ApiResult<ConnectorResourceVO> create(@Valid @RequestBody CreateConnectorResourceRequest request) {
        ConnectorResource saved = connectorResourceService.createConnectorResource(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 按页面查询原平台正式资源。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param pageUrl 页面地址。
     * @return 页面下的正式资源列表。
     */
    @GetMapping
    public ApiResult<List<ConnectorResourceVO>> listByPage(@RequestParam String tenantId,
                                                           @RequestParam String connectorSystemId,
                                                           @RequestParam String pageUrl) {
        return ApiResult.success(toVOList(connectorResourceService.listByPage(tenantId, connectorSystemId, pageUrl)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建正式资源请求。
     * @return 原平台正式资源实体。
     */
    private ConnectorResource toEntity(CreateConnectorResourceRequest request) {
        ConnectorResource resource = new ConnectorResource();
        resource.setId(generateId());
        resource.setTenantId(request.getTenantId());
        resource.setConnectorSystemId(request.getConnectorSystemId());
        resource.setResourceCode(request.getResourceCode());
        resource.setResourceName(request.getResourceName());
        resource.setResourceType(request.getResourceType());
        resource.setPageUrl(request.getPageUrl());
        resource.setLocator(request.getLocator());
        resource.setStableKey(request.getStableKey());
        resource.setMetadataJson(request.getMetadataJson());
        resource.setSourceCaptureId(request.getSourceCaptureId());
        resource.setCreateBy(request.getCreateBy());
        return resource;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param resources 原平台正式资源实体列表。
     * @return 原平台正式资源展示对象列表。
     */
    private List<ConnectorResourceVO> toVOList(List<ConnectorResource> resources) {
        List<ConnectorResourceVO> result = new ArrayList<>();
        for (ConnectorResource resource : resources) {
            result.add(toVO(resource));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param resource 原平台正式资源实体。
     * @return 原平台正式资源展示对象。
     */
    private ConnectorResourceVO toVO(ConnectorResource resource) {
        ConnectorResourceVO vo = new ConnectorResourceVO();
        vo.setId(resource.getId());
        vo.setTenantId(resource.getTenantId());
        vo.setConnectorSystemId(resource.getConnectorSystemId());
        vo.setResourceCode(resource.getResourceCode());
        vo.setResourceName(resource.getResourceName());
        vo.setResourceType(resource.getResourceType());
        vo.setPageUrl(resource.getPageUrl());
        vo.setLocator(resource.getLocator());
        vo.setStableKey(resource.getStableKey());
        vo.setMetadataJson(resource.getMetadataJson());
        vo.setSourceCaptureId(resource.getSourceCaptureId());
        vo.setStatus(resource.getStatus());
        vo.setCreateTime(resource.getCreateTime());
        vo.setUpdateTime(resource.getUpdateTime());
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
