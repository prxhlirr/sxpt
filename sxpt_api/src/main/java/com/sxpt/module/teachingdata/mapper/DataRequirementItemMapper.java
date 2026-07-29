package com.sxpt.module.teachingdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据需求明细数据访问接口。
 *
 * 业务功能：
 * 1. 提供 data_requirement_item 表的基础数据访问能力。
 * 2. 支撑数据准备逐条请求、逐条回填、逐条校验和失败重试。
 *
 * 关键流程：
 * 1. Service 层负责按学生、题目、单位、角色生成需求明细。
 * 2. Mapper 只负责持久化访问，避免承载原平台数据生成和校验规则。
 */
@Mapper
public interface DataRequirementItemMapper extends BaseMapper<DataRequirementItem> {
}
