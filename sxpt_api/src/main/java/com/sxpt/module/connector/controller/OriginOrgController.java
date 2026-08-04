package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.OriginOrgRequest;
import com.sxpt.module.connector.entity.OriginOrg;
import com.sxpt.module.connector.service.OriginOrgService;
import com.sxpt.module.connector.vo.OriginOrgVO;
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
 * 原平台组织字典维护接口。
 *
 * 业务功能：
 * 1. 为后台管理端提供原平台组织的创建、更新、启用、停用、详情和列表查询入口。
 * 2. 为模块流程参与方配置页提供组织下拉数据源，减少手工填写带来的编码不一致。
 *
 * 关键流程：
 * 1. Controller 接收 DTO 并转换为 OriginOrg 实体。
 * 2. Service 完成业务校验和持久化。
 * 3. Controller 返回 VO，避免前端直接依赖数据库实体。
 */
@RestController
@RequestMapping("/api/v1/connector/origin-orgs")
@ConditionalOnProperty(name = "sxpt.connector.origin-org-controller.enabled", havingValue = "true", matchIfMissing = true)
public class OriginOrgController {

    private final OriginOrgService originOrgService;

    public OriginOrgController(OriginOrgService originOrgService) {
        this.originOrgService = originOrgService;
    }

    /**
     * 创建原平台组织字典。
     *
     * @param request 原平台组织维护请求。
     * @return 已创建的原平台组织字典。
     */
    @PostMapping("/create")
    public ApiResult<OriginOrgVO> create(@Valid @RequestBody OriginOrgRequest request) {
        OriginOrg saved = originOrgService.createOriginOrg(toCreateEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 更新原平台组织字典。
     *
     * @param id 组织字典 ID。
     * @param request 原平台组织维护请求。
     * @return 已更新的原平台组织字典。
     */
    @PostMapping("/{id}/update")
    public ApiResult<OriginOrgVO> update(@PathVariable String id,
                                         @Valid @RequestBody OriginOrgRequest request) {
        OriginOrg saved = originOrgService.updateOriginOrg(toUpdateEntity(id, request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 启用原平台组织字典。
     *
     * @param id 组织字典 ID。
     * @return 已启用的原平台组织字典。
     */
    @PostMapping("/{id}/enable")
    public ApiResult<OriginOrgVO> enable(@PathVariable String id) {
        return ApiResult.success(toVO(originOrgService.enableOriginOrg(id)));
    }

    /**
     * 停用原平台组织字典。
     *
     * @param id 组织字典 ID。
     * @return 已停用的原平台组织字典。
     */
    @PostMapping("/{id}/disable")
    public ApiResult<OriginOrgVO> disable(@PathVariable String id) {
        return ApiResult.success(toVO(originOrgService.disableOriginOrg(id)));
    }

    /**
     * 查询原平台组织字典详情。
     *
     * @param id 组织字典 ID。
     * @return 原平台组织字典详情。
     */
    @GetMapping("/{id}")
    public ApiResult<OriginOrgVO> detail(@PathVariable String id) {
        return ApiResult.success(toVO(originOrgService.getOriginOrgById(id)));
    }

    /**
     * 查询某个原平台下的组织字典列表。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param activeOnly 是否只返回启用组织。
     * @return 原平台组织字典列表。
     */
    @GetMapping
    public ApiResult<List<OriginOrgVO>> list(@RequestParam String tenantId,
                                             @RequestParam String connectorSystemId,
                                             @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<OriginOrg> orgs = originOrgService.listOriginOrgs(tenantId, connectorSystemId, activeOnly);
        List<OriginOrgVO> result = new ArrayList<>();
        for (OriginOrg org : orgs) {
            result.add(toVO(org));
        }
        return ApiResult.success(result);
    }

    /**
     * 将创建请求转换为实体。
     *
     * @param request 原平台组织维护请求。
     * @return 原平台组织字典实体。
     */
    private OriginOrg toCreateEntity(OriginOrgRequest request) {
        OriginOrg originOrg = toEditableEntity(request);
        originOrg.setId(UUID.randomUUID().toString().replace("-", ""));
        originOrg.setTenantId(request.getTenantId());
        originOrg.setConnectorSystemId(request.getConnectorSystemId());
        originOrg.setOrgCode(request.getOrgCode());
        originOrg.setCreateBy(request.getCreateBy());
        return originOrg;
    }

    /**
     * 将更新请求转换为实体。
     *
     * @param id 组织字典 ID。
     * @param request 原平台组织维护请求。
     * @return 原平台组织字典实体。
     */
    private OriginOrg toUpdateEntity(String id, OriginOrgRequest request) {
        OriginOrg originOrg = toEditableEntity(request);
        originOrg.setId(id);
        return originOrg;
    }

    /**
     * 提取组织字典可编辑字段。
     *
     * @param request 原平台组织维护请求。
     * @return 原平台组织字典实体。
     */
    private OriginOrg toEditableEntity(OriginOrgRequest request) {
        OriginOrg originOrg = new OriginOrg();
        originOrg.setOrgName(request.getOrgName());
        originOrg.setExternalOrgId(request.getExternalOrgId());
        originOrg.setParentExternalOrgId(request.getParentExternalOrgId());
        originOrg.setOrgType(request.getOrgType());
        originOrg.setRemark(request.getRemark());
        originOrg.setUpdateBy(request.getUpdateBy());
        return originOrg;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param originOrg 原平台组织字典实体。
     * @return 原平台组织字典返回对象。
     */
    private OriginOrgVO toVO(OriginOrg originOrg) {
        OriginOrgVO vo = new OriginOrgVO();
        vo.setId(originOrg.getId());
        vo.setTenantId(originOrg.getTenantId());
        vo.setConnectorSystemId(originOrg.getConnectorSystemId());
        vo.setOrgCode(originOrg.getOrgCode());
        vo.setOrgName(originOrg.getOrgName());
        vo.setExternalOrgId(originOrg.getExternalOrgId());
        vo.setParentExternalOrgId(originOrg.getParentExternalOrgId());
        vo.setOrgType(originOrg.getOrgType());
        vo.setRemark(originOrg.getRemark());
        vo.setStatus(originOrg.getStatus());
        vo.setCreateTime(originOrg.getCreateTime());
        vo.setUpdateTime(originOrg.getUpdateTime());
        return vo;
    }
}
