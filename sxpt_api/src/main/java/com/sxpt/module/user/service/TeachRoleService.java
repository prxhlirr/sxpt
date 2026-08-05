package com.sxpt.module.user.service;

import com.sxpt.module.user.entity.TeachRole;

import java.util.List;

/**
 * 教学平台角色服务。
 *
 * 业务功能：
 * 1. 维护教学平台内部教师、学生、管理员等角色定义。
 * 2. 为后续用户授权和权限判断提供稳定角色来源。
 *
 * 关键流程：
 * 1. Service 层负责校验角色基础字段和补齐生命周期字段。
 * 2. Mapper 层只负责数据库写入，不承载业务判断。
 */
public interface TeachRoleService {

    /**
     * 创建教学平台角色。
     *
     * @param teachRole 教学平台角色实体，必须包含 ID、租户、角色编码和角色名称。
     * @return 已保存的教学平台角色实体。
     */
    TeachRole createTeachRole(TeachRole teachRole);

    /**
     * 更新教学平台角色基础信息。
     *
     * @param tenantId 租户 ID。
     * @param teachRole 角色实体，必须包含 ID 和可编辑字段。
     * @return 已更新的角色实体。
     */
    TeachRole updateTeachRole(String tenantId, TeachRole teachRole);

    /**
     * 切换教学平台角色启停用状态。
     *
     * @param tenantId 租户 ID。
     * @param id 角色 ID。
     * @param status 目标状态。
     * @return 已更新状态的角色实体。
     */
    TeachRole updateTeachRoleStatus(String tenantId, String id, String status);

    /**
     * 查询指定租户下的教学平台角色列表。
     *
     * @param tenantId 租户 ID。
     * @return 指定租户下未删除的教学平台角色列表。
     */
    List<TeachRole> listTeachRolesByTenantId(String tenantId);
}
