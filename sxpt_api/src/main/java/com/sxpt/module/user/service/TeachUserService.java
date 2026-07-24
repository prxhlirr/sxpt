package com.sxpt.module.user.service;

import com.sxpt.module.user.entity.TeachUser;

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
}
