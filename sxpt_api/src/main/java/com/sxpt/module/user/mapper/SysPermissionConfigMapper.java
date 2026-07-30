package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.SysPermissionConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统权限配置 Mapper。
 *
 * 业务功能：绑定 sys_permission_config 表的基础持久化能力。
 * 关键流程：权限编码唯一性由数据库索引兜底，应用层负责入参完整性。
 */
@Mapper
public interface SysPermissionConfigMapper extends BaseMapper<SysPermissionConfig> {
}
