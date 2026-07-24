package com.sxpt.module.execution.service;

import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.vo.RuntimeContextVO;

/**
 * SDK 运行上下文服务。
 *
 * 业务功能：
 * 1. 为 SDK 提供任务运行初始化所需的上下文。
 * 2. 基于已固化的 task_execution_context 快照返回配置，保证历史执行稳定。
 *
 * 关键流程：
 * 1. 使用租户和任务执行 ID 定位唯一上下文快照。
 * 2. 将快照转换为 SDK 专用 VO，隐藏数据库生命周期字段。
 */
public interface RuntimeContextService {

    /**
     * 获取 SDK 运行上下文。
     *
     * @param request SDK 运行上下文查询请求。
     * @return SDK 运行上下文。
     */
    RuntimeContextVO getRuntimeContext(RuntimeContextQueryRequest request);
}
