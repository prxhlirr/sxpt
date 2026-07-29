package com.sxpt.module.teachingdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据准备任务数据访问接口。
 *
 * 业务功能：
 * 1. 提供 data_prepare_job 表的基础数据访问能力。
 * 2. 支撑批量调用原平台、失败重试、任务审计和后台任务查询。
 *
 * 关键流程：
 * 1. Service 层负责创建任务、调用原平台 Adapter 和推进状态机。
 * 2. Mapper 只负责持久化访问，避免承载外部系统调用和补偿逻辑。
 */
@Mapper
public interface DataPrepareJobMapper extends BaseMapper<DataPrepareJob> {
}
