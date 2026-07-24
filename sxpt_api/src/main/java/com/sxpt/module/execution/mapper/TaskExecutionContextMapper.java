package com.sxpt.module.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务执行上下文快照 Mapper。
 *
 * 业务功能：
 * 1. 提供 task_execution_context 表的基础持久化能力。
 * 2. 支撑学生进入任务时写入上下文快照，以及后续按执行记录读取快照。
 *
 * 关键流程：
 * 1. Service 负责业务校验和默认值补齐。
 * 2. Mapper 只负责 MyBatis Plus 标准 CRUD，保持持久化层简单稳定。
 */
@Mapper
public interface TaskExecutionContextMapper extends BaseMapper<TaskExecutionContext> {
}
