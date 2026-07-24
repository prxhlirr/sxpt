package com.sxpt.module.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.practice.entity.PracticeStepResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 练习步骤结果 Mapper。
 *
 * 业务功能：
 * 1. 绑定 practice_step_result 表的基础持久化能力。
 * 2. 由 Service 组合查询条件，Mapper 仅承载 MyBatis Plus 标准数据访问。
 *
 * 关键流程：
 * 1. Service 调用 selectOne 判断同一步骤结果是否已存在。
 * 2. 根据幂等命中结果执行 insert 或 updateById。
 */
@Mapper
public interface PracticeStepResultMapper extends BaseMapper<PracticeStepResult> {
}
