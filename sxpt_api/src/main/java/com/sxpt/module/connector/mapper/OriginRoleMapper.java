package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.OriginRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台角色字典数据访问接口。
 *
 * 业务功能：
 * 1. 提供 origin_role 表的基础持久化能力。
 * 2. 支撑业务模块流程参与方配置页按原平台查询可选角色。
 *
 * 关键流程：
 * 1. Service 层负责租户、原平台和角色编码唯一性校验。
 * 2. Mapper 只承载标准 CRUD，避免把业务规则散落到 SQL 层。
 */
@Mapper
public interface OriginRoleMapper extends BaseMapper<OriginRole> {
}
