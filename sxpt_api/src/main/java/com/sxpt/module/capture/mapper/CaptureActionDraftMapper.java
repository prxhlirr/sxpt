package com.sxpt.module.capture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import org.apache.ibatis.annotations.Mapper;

/**
 * 页面动作草稿 Mapper。
 *
 * 业务功能：
 * 1. 绑定 capture_action_draft 表的基础持久化能力。
 * 2. 为草稿生成、草稿查询和后续教师确认提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责租户边界、状态默认值和草稿排序规则。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface CaptureActionDraftMapper extends BaseMapper<CaptureActionDraft> {
}
