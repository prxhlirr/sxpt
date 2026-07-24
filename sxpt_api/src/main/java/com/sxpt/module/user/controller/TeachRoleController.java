package com.sxpt.module.user.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.user.dto.CreateTeachRoleRequest;
import com.sxpt.module.user.entity.TeachRole;
import com.sxpt.module.user.service.TeachRoleService;
import com.sxpt.module.user.vo.TeachRoleVO;
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
 * 教学平台角色接口。
 *
 * 业务功能：
 * 1. 提供教学平台内部角色的创建入口，用于后续用户授权和权限判断。
 * 2. 将 HTTP 请求模型与数据库实体隔离，避免前端依赖持久化细节。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截无效入参。
 * 2. 将 DTO 转换为实体，并在应用层生成主键。
 * 3. 调用 Service 完成业务校验和持久化。
 * 4. 将保存后的实体转换为 VO，按统一响应结构返回。
 */
@RestController
@RequestMapping("/api/v1/roles")
@ConditionalOnProperty(name = "sxpt.role.controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachRoleController {

    private final TeachRoleService teachRoleService;

    public TeachRoleController(TeachRoleService teachRoleService) {
        this.teachRoleService = teachRoleService;
    }

    /**
     * 创建教学平台角色。
     *
     * @param request 创建教学角色请求。
     * @return 已创建的教学平台角色。
     */
    @PostMapping("/create")
    public ApiResult<TeachRoleVO> create(@Valid @RequestBody CreateTeachRoleRequest request) {
        TeachRole saved = teachRoleService.createTeachRole(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询教学平台角色列表。
     *
     * @param tenantId 租户 ID。
     * @return 教学平台角色列表。
     */
    @GetMapping
    public ApiResult<List<TeachRoleVO>> list(@RequestParam String tenantId) {
        List<TeachRole> teachRoles = teachRoleService.listTeachRolesByTenantId(tenantId);
        List<TeachRoleVO> result = new ArrayList<>();
        for (TeachRole teachRole : teachRoles) {
            result.add(toVO(teachRole));
        }
        return ApiResult.success(result);
    }

    /**
     * 将创建请求转换为角色实体。
     *
     * @param request 创建教学角色请求。
     * @return 教学平台角色实体。
     */
    private TeachRole toEntity(CreateTeachRoleRequest request) {
        TeachRole teachRole = new TeachRole();
        teachRole.setId(generateId());
        teachRole.setTenantId(request.getTenantId());
        teachRole.setRoleCode(request.getRoleCode());
        teachRole.setRoleName(request.getRoleName());
        teachRole.setDescription(request.getDescription());
        return teachRole;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param teachRole 教学平台角色实体。
     * @return 教学平台角色返回对象。
     */
    private TeachRoleVO toVO(TeachRole teachRole) {
        TeachRoleVO vo = new TeachRoleVO();
        vo.setId(teachRole.getId());
        vo.setTenantId(teachRole.getTenantId());
        vo.setRoleCode(teachRole.getRoleCode());
        vo.setRoleName(teachRole.getRoleName());
        vo.setDescription(teachRole.getDescription());
        vo.setStatus(teachRole.getStatus());
        vo.setCreateTime(teachRole.getCreateTime());
        vo.setUpdateTime(teachRole.getUpdateTime());
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
