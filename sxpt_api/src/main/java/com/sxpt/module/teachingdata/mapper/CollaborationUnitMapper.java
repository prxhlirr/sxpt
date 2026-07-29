package com.sxpt.module.teachingdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.CollaborationUnit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协作单元数据访问接口。
 *
 * 业务功能：
 * 1. 提供 collaboration_unit 表的基础数据访问能力。
 * 2. 支撑多角色协作单元创建、状态迁移、整体重置和审计查询。
 *
 * 关键流程：
 * 1. Service 层负责协作单元整体重置、并发控制和状态流转。
 * 2. Mapper 只负责持久化访问，避免承载协作业务规则。
 */
@Mapper
public interface CollaborationUnitMapper extends BaseMapper<CollaborationUnit> {
}
