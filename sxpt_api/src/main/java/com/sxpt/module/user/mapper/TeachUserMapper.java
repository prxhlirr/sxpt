package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.TeachUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学平台用户 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teach_user 表的基础持久化能力。
 * 2. 为用户创建、查询、状态控制和后续角色授权提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责业务校验和默认值处理。
 * 2. Mapper 只承载 MyBatis Plus 的基础 CRUD 能力，不放业务判断。
 */
@Mapper
public interface TeachUserMapper extends BaseMapper<TeachUser> {
}
