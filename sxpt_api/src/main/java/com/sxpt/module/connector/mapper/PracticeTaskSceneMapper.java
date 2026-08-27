package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.PracticeTaskScene;
import org.apache.ibatis.annotations.Mapper;

/**
 * 练习任务业务场景 Mapper。
 *
 * 业务功能：
 * 1. 绑定 practice_task_scene 表的基础持久化能力。
 * 2. 支撑 verify launchToken 时返回本次练习任务的多个 businessSceneCode。
 *
 * 关键流程：
 * 1. 老师发布练习任务时写入场景绑定。
 * 2. 原平台 verify token 时按 taskId 查询任务清单。
 */
@Mapper
public interface PracticeTaskSceneMapper extends BaseMapper<PracticeTaskScene> {
}
