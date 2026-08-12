package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ClassicCaseUsage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 经典案例使用记录 Mapper。
 *
 * 业务功能：
 * 1. 提供 classic_case_usage 表的基础持久化能力。
 * 2. 调用学习环境、结果校验和审计字段组装由 Service 层负责。
 */
@Mapper
public interface ClassicCaseUsageMapper extends BaseMapper<ClassicCaseUsage> {
}
