package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台启动上下文 Mapper。
 *
 * 业务功能：
 * 1. 绑定 platform_launch_context 表的基础持久化能力。
 * 2. 为 launchToken 生成、校验和启动状态回写提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责 token 生成、hash 计算、过期时间和状态规则。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD 能力，不放业务判断。
 */
@Mapper
public interface PlatformLaunchContextMapper extends BaseMapper<PlatformLaunchContext> {
}
