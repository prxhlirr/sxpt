package com.sxpt.module.capture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.capture.entity.CaptureEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * SDK 采集事件 Mapper。
 *
 * 业务功能：
 * 1. 绑定 capture_event 表的基础持久化能力。
 * 2. 为 SDK 事件上报、会话事件查询和后续动作草稿生成提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责租户边界、幂等和默认值处理。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，保持持久化层简单可测。
 */
@Mapper
public interface CaptureEventMapper extends BaseMapper<CaptureEvent> {
}
