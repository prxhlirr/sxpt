package com.sxpt.module.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.execution.entity.TaskExecution;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生任务执行主记录 Mapper。
 *
 * 业务功能：
 * 1. 提供 task_execution 表的基础持久化能力。
 * 2. 为学生端开始、提交、评分回写提供统一数据入口。
 *
 * 关键流程：
 * 1. Service 通过 Mapper 创建 RUNNING 执行记录。
 * 2. Service 通过 Mapper 查询并更新执行状态、得分和结果摘要。
 */
@Mapper
public interface TaskExecutionMapper extends BaseMapper<TaskExecution> {
}
