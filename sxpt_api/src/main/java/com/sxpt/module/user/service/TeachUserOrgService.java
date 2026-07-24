package com.sxpt.module.user.service;

import com.sxpt.module.user.entity.TeachUserOrg;

/**
 * 教学用户组织关系服务。
 *
 * 业务功能：
 * 1. 维护用户与班级、课程班或分组之间的关系。
 * 2. 为后续课程成员、任务发布范围和学生可见任务提供稳定关系来源。
 *
 * 关键流程：
 * 1. Service 层负责校验关系最小字段和补齐生命周期字段。
 * 2. Mapper 层只负责数据库写入，唯一性由数据库索引保证。
 */
public interface TeachUserOrgService {

    /**
     * 添加用户到教学组织。
     *
     * @param teachUserOrg 用户组织关系实体，必须包含 ID、租户、用户 ID、组织 ID 和关系类型。
     * @return 已保存的用户组织关系实体。
     */
    TeachUserOrg addUserToOrg(TeachUserOrg teachUserOrg);

    /**
     * 从教学组织中移除用户。
     *
     * @param tenantId 租户 ID。
     * @param orgId 教学组织 ID。
     * @param userId 用户 ID。
     * @return 已软删除的用户组织关系实体。
     */
    TeachUserOrg removeUserFromOrg(String tenantId, String orgId, String userId);
}
