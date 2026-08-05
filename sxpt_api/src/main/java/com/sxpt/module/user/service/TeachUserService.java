package com.sxpt.module.user.service;

import com.sxpt.module.user.entity.TeachUser;

import java.util.List;

/**
 * 教学平台用户服务。
 *
 * 业务功能：
 * 1. 维护教师、学生、管理员在教学平台内的最小用户镜像。
 * 2. 为后续角色授权、班级关系、任务发布和学习执行提供稳定用户来源。
 *
 * 关键流程：
 * 1. Service 层负责校验用户基础字段和补齐生命周期字段。
 * 2. Mapper 层只负责数据库写入，不承载业务判断。
 */
public interface TeachUserService {

    /**
     * 创建教学平台用户。
     *
     * @param teachUser 教学平台用户实体，必须包含 ID、租户、账号、姓名、用户类型和来源类型。
     * @return 已保存的教学平台用户实体。
     */
    TeachUser createTeachUser(TeachUser teachUser);

    /**
     * 更新教学平台用户基础信息。
     *
     * @param tenantId 租户 ID。
     * @param teachUser 用户实体，必须包含 ID 和可编辑字段。
     * @return 已更新的用户实体。
     */
    TeachUser updateTeachUser(String tenantId, TeachUser teachUser);

    /**
     * 切换教学平台用户启停用状态。
     *
     * @param tenantId 租户 ID。
     * @param id 用户 ID。
     * @param status 目标状态。
     * @return 已更新状态的用户实体。
     */
    TeachUser updateTeachUserStatus(String tenantId, String id, String status);

    /**
     * 查询租户下的教学平台用户列表。
     *
     * 业务功能：为后台基础配置页面提供用户可视化维护入口，避免管理员只能通过初始化脚本确认老师、学生和管理员账号。
     * 关键流程：按租户和软删除边界读取用户，保持用户主数据不会跨租户展示。
     *
     * @param tenantId 租户 ID。
     * @return 教学平台用户列表。
     */
    List<TeachUser> listTeachUsersByTenantId(String tenantId);
}
