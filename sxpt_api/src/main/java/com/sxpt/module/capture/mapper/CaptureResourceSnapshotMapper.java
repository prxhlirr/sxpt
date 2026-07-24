package com.sxpt.module.capture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采集资源快照 Mapper。
 *
 * 业务功能：
 * 1. 绑定 capture_resource_snapshot 表的基础持久化能力。
 * 2. 为页面摘要、关键元素摘要和后续正式资源沉淀提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责快照范围、DEBUG 规则和租户边界。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，保持持久化层简单。
 */
@Mapper
public interface CaptureResourceSnapshotMapper extends BaseMapper<CaptureResourceSnapshot> {
}
