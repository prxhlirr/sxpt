package com.sxpt.module.execution.service;

import com.sxpt.module.execution.dto.ExamContextQueryRequest;
import com.sxpt.module.execution.vo.ExamContextVO;

/**
 * 考试模式上下文服务。
 *
 * 业务功能：
 * 1. 为 SDK 考试模式提供专用运行上下文。
 * 2. 校验通用 runtime-context 的 sdkMode，防止学习或练习上下文进入考试模式。
 *
 * 关键流程：
 * 1. 使用执行身份读取 runtime-context。
 * 2. 仅当 sdkMode 为 EXAM 时转换为 ExamContextVO。
 */
public interface ExamContextService {

    /**
     * 获取考试模式上下文。
     *
     * @param request 考试模式上下文查询请求。
     * @return 考试模式上下文。
     */
    ExamContextVO getExamContext(ExamContextQueryRequest request);
}
