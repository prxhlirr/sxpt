package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.PlatformCapability;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台能力注册数据访问接口。
 *
 * 业务功能：
 * 1. 提供 platform_capability 表的基础持久化能力。
 * 2. 支撑策略启用前校验原平台是否已声明数据创建、锁定、结果校验等关键能力。
 *
 * 关键流程：
 * 1. Service 层根据租户、原平台和能力编码查询能力注册记录。
 * 2. Mapper 只承载数据访问，能力组合规则由 Service 层集中维护。
 */
@Mapper
public interface PlatformCapabilityMapper extends BaseMapper<PlatformCapability> {
}
