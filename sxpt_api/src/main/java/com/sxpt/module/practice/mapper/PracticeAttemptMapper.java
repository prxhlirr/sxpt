package com.sxpt.module.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.practice.entity.PracticeAttempt;
import org.apache.ibatis.annotations.Mapper;

/**
 * 练习次数 Mapper。
 *
 * 业务功能：
 * 1. 绑定 practice_attempt 表的基础持久化能力。
 * 2. 为练习次数创建、完成回写和学生练习记录查询提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责次数计算、状态流转和默认值处理。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，保持持久化层简单可测。
 */
@Mapper
public interface PracticeAttemptMapper extends BaseMapper<PracticeAttempt> {
}
