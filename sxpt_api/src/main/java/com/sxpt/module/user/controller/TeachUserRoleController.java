package com.sxpt.module.user.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.user.dto.GrantTeachUserRoleRequest;
import com.sxpt.module.user.entity.TeachUserRole;
import com.sxpt.module.user.service.TeachUserRoleService;
import com.sxpt.module.user.vo.TeachUserRoleVO;
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
 * 教学平台用户角色关系接口。
 *
 * 业务功能：
 * 1. 提供教学用户授予教学角色的入口。
 * 2. 为后续教学管理权限、学生学习权限和管理员复核权限提供基础授权关系。
 *
 * 关键流程：
 * 1. 接收授权请求并触发 Bean Validation。
 * 2. 将 DTO 转换为实体，并在应用层生成主键。
 * 3. 调用 Service 完成授权写入和默认字段补齐。
 * 4. 将保存后的实体转换为 VO 返回。
 */
@RestController
@RequestMapping("/api/v1/user-roles")
@ConditionalOnProperty(name = "sxpt.user-role.controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachUserRoleController {

    private final TeachUserRoleService teachUserRoleService;

    public TeachUserRoleController(TeachUserRoleService teachUserRoleService) {
        this.teachUserRoleService = teachUserRoleService;
    }

    /**
     * 为教学用户授予教学平台角色。
     *
     * @param request 授予教学用户角色请求。
     * @return 已保存的用户角色关系。
     */
    @PostMapping("/grant")
    public ApiResult<TeachUserRoleVO> grant(@Valid @RequestBody GrantTeachUserRoleRequest request) {
        TeachUserRole saved = teachUserRoleService.grantUserRole(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询用户角色授权关系列表。
     *
     * 业务功能：为管理端用户角色绑定页面提供当前授权关系，支撑管理员复核老师、学生和管理员角色来源。
     * 关键流程：按租户查询后统一转换为 VO，前端再结合用户和角色列表展示可读名称。
     *
     * @param tenantId 租户 ID。
     * @return 用户角色授权关系列表。
     */
    @GetMapping
    public ApiResult<List<TeachUserRoleVO>> list(@RequestParam String tenantId) {
        List<TeachUserRole> relations = teachUserRoleService.listUserRolesByTenantId(tenantId);
        List<TeachUserRoleVO> result = new ArrayList<>();
        for (TeachUserRole relation : relations) {
            result.add(toVO(relation));
        }
        return ApiResult.success(result);
    }

    /**
     * 将授权请求转换为用户角色关系实体。
     *
     * @param request 授予教学用户角色请求。
     * @return 用户角色关系实体。
     */
    private TeachUserRole toEntity(GrantTeachUserRoleRequest request) {
        TeachUserRole teachUserRole = new TeachUserRole();
        teachUserRole.setId(generateId());
        teachUserRole.setTenantId(request.getTenantId());
        teachUserRole.setUserId(request.getUserId());
        teachUserRole.setRoleId(request.getRoleId());
        teachUserRole.setGrantSource(request.getGrantSource());
        return teachUserRole;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param teachUserRole 用户角色关系实体。
     * @return 用户角色关系返回对象。
     */
    private TeachUserRoleVO toVO(TeachUserRole teachUserRole) {
        TeachUserRoleVO vo = new TeachUserRoleVO();
        vo.setId(teachUserRole.getId());
        vo.setTenantId(teachUserRole.getTenantId());
        vo.setUserId(teachUserRole.getUserId());
        vo.setRoleId(teachUserRole.getRoleId());
        vo.setGrantSource(teachUserRole.getGrantSource());
        vo.setStatus(teachUserRole.getStatus());
        vo.setCreateTime(teachUserRole.getCreateTime());
        vo.setUpdateTime(teachUserRole.getUpdateTime());
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
