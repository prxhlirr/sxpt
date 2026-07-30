package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统角色权限绑定 Mapper。
 *
 * 业务功能：绑定 sys_role_permission 表的基础持久化能力。
 * 关键流程：授权关系的重复校验由数据库唯一索引兜底，Service 负责租户和必填字段校验。
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {
}
