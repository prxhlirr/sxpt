package com.sxpt.module.user.service;

import com.sxpt.module.user.entity.TeachUserRole;

/**
 * 教学平台用户角色关系服务。
 *
 * 业务功能：
 * 1. 维护教学用户与教学角色之间的授权关系。
 * 2. 为后续教师、学生、管理员等教学平台权限判断提供稳定授权来源。
 *
 * 关键流程：
 * 1. Service 层负责校验授权关系的最小字段和补齐生命周期字段。
 * 2. Mapper 层只负责数据库写入，唯一性由数据库索引保证。
 */
public interface TeachUserRoleService {

    /**
     * 为教学用户授予教学平台角色。
     *
     * @param teachUserRole 用户角色关系实体，必须包含 ID、租户、用户 ID 和角色 ID。
     * @return 已保存的用户角色关系实体。
     */
    TeachUserRole grantUserRole(TeachUserRole teachUserRole);
}
