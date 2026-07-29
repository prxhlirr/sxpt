package com.sxpt.module.teachingdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据实例分配数据访问接口。
 *
 * 业务功能：
 * 1. 提供 data_instance_allocation 表的基础数据访问能力。
 * 2. 支撑学生领取数据实例、释放实例、消费实例和协作片段审计。
 *
 * 关键流程：
 * 1. Service 层负责并发领取、唯一分配和状态迁移。
 * 2. Mapper 只负责持久化访问，避免承载数据分配规则。
 */
@Mapper
public interface DataInstanceAllocationMapper extends BaseMapper<DataInstanceAllocation> {
}
