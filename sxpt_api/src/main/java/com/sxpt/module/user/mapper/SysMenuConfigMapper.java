package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.SysMenuConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统菜单配置 Mapper。
 *
 * 业务功能：绑定 sys_menu_config 表的基础持久化能力。
 * 关键流程：业务校验放在 Service，Mapper 仅承载 MyBatis Plus 基础 CRUD。
 */
@Mapper
public interface SysMenuConfigMapper extends BaseMapper<SysMenuConfig> {
}
