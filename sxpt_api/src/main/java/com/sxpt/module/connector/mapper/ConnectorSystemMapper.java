package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ConnectorSystem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原业务平台数据访问接口。
 *
 * 业务功能：
 * 1. 提供 connector_system 表的基础数据访问能力。
 * 2. 支撑原平台接入配置的新增、查询、更新和启停。
 *
 * 关键流程：
 * 1. Service 层负责业务校验和事务边界。
 * 2. Mapper 只负责持久化访问，避免承载原平台接入业务规则。
 */
@Mapper
public interface ConnectorSystemMapper extends BaseMapper<ConnectorSystem> {
}
