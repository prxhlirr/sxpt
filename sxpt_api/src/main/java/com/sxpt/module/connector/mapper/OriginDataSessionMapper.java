package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.OriginDataSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台数据会话 Mapper。
 *
 * 业务功能：
 * 1. 绑定 origin_data_session 表的基础持久化能力。
 * 2. 支撑 DataSession 注册、幂等查询和后续过程证据追溯。
 *
 * 关键流程：
 * 1. Service 负责幂等键、教学上下文和业务数据校验。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD 能力，不放业务规则。
 */
@Mapper
public interface OriginDataSessionMapper extends BaseMapper<OriginDataSession> {
}
