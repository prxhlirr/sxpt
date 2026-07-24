package com.sxpt.module.execution.service;

import com.sxpt.module.execution.dto.LearningContextQueryRequest;
import com.sxpt.module.execution.vo.LearningContextVO;

/**
 * 学习模式上下文服务。
 *
 * 业务功能：
 * 1. 为 SDK 学习模式提供专用运行上下文。
 * 2. 校验通用 runtime-context 的 sdkMode，防止练习或考试上下文进入学习模式。
 *
 * 关键流程：
 * 1. 使用执行身份读取 runtime-context。
 * 2. 仅当 sdkMode 为 LEARNING 时转换为 LearningContextVO。
 */
public interface LearningContextService {

    /**
     * 获取学习模式上下文。
     *
     * @param request 学习模式上下文查询请求。
     * @return 学习模式上下文。
     */
    LearningContextVO getLearningContext(LearningContextQueryRequest request);
}
