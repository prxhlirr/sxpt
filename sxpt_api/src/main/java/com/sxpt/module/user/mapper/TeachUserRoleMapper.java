package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.TeachUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学平台用户角色关联 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teach_user_role 表的基础持久化能力。
 * 2. 为用户授权、角色查询和权限判断提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责授权来源、租户隔离和重复授权规则。
 * 2. Mapper 只承载 MyBatis Plus 的基础 CRUD 能力，不放业务判断。
 */
@Mapper
public interface TeachUserRoleMapper extends BaseMapper<TeachUserRole> {
}
