package com.sxpt.module.teachingdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.CollaborationSegmentAllocation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协作片段分配数据访问接口。
 *
 * 业务功能：
 * 1. 提供 collaboration_segment_allocation 表的基础数据访问能力。
 * 2. 支撑协作单元下的学生、单位、角色、片段分配和进度查询。
 *
 * 关键流程：
 * 1. Service 层负责片段分配唯一性、状态迁移和整体协作进度计算。
 * 2. Mapper 只负责持久化访问，避免承载协作业务规则。
 */
@Mapper
public interface CollaborationSegmentAllocationMapper extends BaseMapper<CollaborationSegmentAllocation> {
}
