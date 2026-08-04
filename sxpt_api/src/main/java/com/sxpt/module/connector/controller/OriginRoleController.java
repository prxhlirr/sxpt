package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.OriginRoleRequest;
import com.sxpt.module.connector.entity.OriginRole;
import com.sxpt.module.connector.service.OriginRoleService;
import com.sxpt.module.connector.vo.OriginRoleVO;
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
 * 原平台角色字典维护接口。
 *
 * 业务功能：
 * 1. 为后台管理端提供原平台角色的创建、更新、启用、停用、详情和列表查询入口。
 * 2. 为模块流程参与方配置页提供角色下拉数据源，减少手工填写带来的编码不一致。
 *
 * 关键流程：
 * 1. Controller 接收 DTO 并转换为 OriginRole 实体。
 * 2. Service 完成业务校验和持久化。
 * 3. Controller 返回 VO，避免前端直接依赖数据库实体。
 */
@RestController
@RequestMapping("/api/v1/connector/origin-roles")
@ConditionalOnProperty(name = "sxpt.connector.origin-role-controller.enabled", havingValue = "true", matchIfMissing = true)
public class OriginRoleController {

    private final OriginRoleService originRoleService;

    public OriginRoleController(OriginRoleService originRoleService) {
        this.originRoleService = originRoleService;
    }

    /**
     * 创建原平台角色字典。
     *
     * @param request 原平台角色维护请求。
     * @return 已创建的原平台角色字典。
     */
    @PostMapping("/create")
    public ApiResult<OriginRoleVO> create(@Valid @RequestBody OriginRoleRequest request) {
        OriginRole saved = originRoleService.createOriginRole(toCreateEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 更新原平台角色字典。
     *
     * @param id 角色字典 ID。
     * @param request 原平台角色维护请求。
     * @return 已更新的原平台角色字典。
     */
    @PostMapping("/{id}/update")
    public ApiResult<OriginRoleVO> update(@PathVariable String id,
                                          @Valid @RequestBody OriginRoleRequest request) {
        OriginRole saved = originRoleService.updateOriginRole(toUpdateEntity(id, request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 启用原平台角色字典。
     *
     * @param id 角色字典 ID。
     * @return 已启用的原平台角色字典。
     */
    @PostMapping("/{id}/enable")
    public ApiResult<OriginRoleVO> enable(@PathVariable String id) {
        return ApiResult.success(toVO(originRoleService.enableOriginRole(id)));
    }

    /**
     * 停用原平台角色字典。
     *
     * @param id 角色字典 ID。
     * @return 已停用的原平台角色字典。
     */
    @PostMapping("/{id}/disable")
    public ApiResult<OriginRoleVO> disable(@PathVariable String id) {
        return ApiResult.success(toVO(originRoleService.disableOriginRole(id)));
    }

    /**
     * 查询原平台角色字典详情。
     *
     * @param id 角色字典 ID。
     * @return 原平台角色字典详情。
     */
    @GetMapping("/{id}")
    public ApiResult<OriginRoleVO> detail(@PathVariable String id) {
        return ApiResult.success(toVO(originRoleService.getOriginRoleById(id)));
    }

    /**
     * 查询某个原平台下的角色字典列表。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param activeOnly 是否只返回启用角色。
     * @return 原平台角色字典列表。
     */
    @GetMapping
    public ApiResult<List<OriginRoleVO>> list(@RequestParam String tenantId,
                                              @RequestParam String connectorSystemId,
                                              @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<OriginRole> roles = originRoleService.listOriginRoles(tenantId, connectorSystemId, activeOnly);
        List<OriginRoleVO> result = new ArrayList<>();
        for (OriginRole role : roles) {
            result.add(toVO(role));
        }
        return ApiResult.success(result);
    }

    /**
     * 将创建请求转换为实体。
     *
     * @param request 原平台角色维护请求。
     * @return 原平台角色字典实体。
     */
    private OriginRole toCreateEntity(OriginRoleRequest request) {
        OriginRole originRole = toEditableEntity(request);
        originRole.setId(UUID.randomUUID().toString().replace("-", ""));
        originRole.setTenantId(request.getTenantId());
        originRole.setConnectorSystemId(request.getConnectorSystemId());
        originRole.setRoleCode(request.getRoleCode());
        originRole.setCreateBy(request.getCreateBy());
        return originRole;
    }

    /**
     * 将更新请求转换为实体。
     *
     * @param id 角色字典 ID。
     * @param request 原平台角色维护请求。
     * @return 原平台角色字典实体。
     */
    private OriginRole toUpdateEntity(String id, OriginRoleRequest request) {
        OriginRole originRole = toEditableEntity(request);
        originRole.setId(id);
        return originRole;
    }

    /**
     * 提取角色字典可编辑字段。
     *
     * @param request 原平台角色维护请求。
     * @return 原平台角色字典实体。
     */
    private OriginRole toEditableEntity(OriginRoleRequest request) {
        OriginRole originRole = new OriginRole();
        originRole.setRoleName(request.getRoleName());
        originRole.setExternalRoleId(request.getExternalRoleId());
        originRole.setRoleType(request.getRoleType());
        originRole.setRemark(request.getRemark());
        originRole.setUpdateBy(request.getUpdateBy());
        return originRole;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param originRole 原平台角色字典实体。
     * @return 原平台角色字典返回对象。
     */
    private OriginRoleVO toVO(OriginRole originRole) {
        OriginRoleVO vo = new OriginRoleVO();
        vo.setId(originRole.getId());
        vo.setTenantId(originRole.getTenantId());
        vo.setConnectorSystemId(originRole.getConnectorSystemId());
        vo.setRoleCode(originRole.getRoleCode());
        vo.setRoleName(originRole.getRoleName());
        vo.setExternalRoleId(originRole.getExternalRoleId());
        vo.setRoleType(originRole.getRoleType());
        vo.setRemark(originRole.getRemark());
        vo.setStatus(originRole.getStatus());
        vo.setCreateTime(originRole.getCreateTime());
        vo.setUpdateTime(originRole.getUpdateTime());
        return vo;
    }
}
