package com.sxpt.module.capture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.capture.entity.CaptureSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 备案采集会话 Mapper。
 *
 * 业务功能：
 * 1. 绑定 capture_session 表的基础持久化能力。
 * 2. 为教师开始备案、结束采集和查看历史采集会话提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责租户隔离、教师边界和会话状态流转。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD 能力，避免在持久化层混入业务判断。
 */
@Mapper
public interface CaptureSessionMapper extends BaseMapper<CaptureSession> {
}
