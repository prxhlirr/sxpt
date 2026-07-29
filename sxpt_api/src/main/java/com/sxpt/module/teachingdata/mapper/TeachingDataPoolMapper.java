package com.sxpt.module.teachingdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学数据池数据访问接口。
 *
 * 业务功能：
 * 1. 提供 teaching_data_pool 表的基础数据访问能力。
 * 2. 支撑数据池创建、统计回写、数据领取和考试锁定前置查询。
 *
 * 关键流程：
 * 1. Service 层负责数据池容量、状态迁移和并发领取规则。
 * 2. Mapper 只负责持久化访问，避免承载数据分配业务判断。
 */
@Mapper
public interface TeachingDataPoolMapper extends BaseMapper<TeachingDataPool> {
}
