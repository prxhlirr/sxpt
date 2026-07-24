package com.sxpt.module.execution.service;

import com.sxpt.module.execution.dto.PracticeContextQueryRequest;
import com.sxpt.module.execution.vo.PracticeContextVO;

/**
 * 练习模式上下文服务。
 *
 * 业务功能：
 * 1. 为 SDK 练习模式提供专用运行上下文。
 * 2. 校验通用 runtime-context 的 sdkMode，防止学习或考试上下文进入练习模式。
 *
 * 关键流程：
 * 1. 使用执行身份读取 runtime-context。
 * 2. 仅当 sdkMode 为 PRACTICE 时转换为 PracticeContextVO。
 */
public interface PracticeContextService {

    /**
     * 获取练习模式上下文。
     *
     * @param request 练习模式上下文查询请求。
     * @return 练习模式上下文。
     */
    PracticeContextVO getPracticeContext(PracticeContextQueryRequest request);
}
