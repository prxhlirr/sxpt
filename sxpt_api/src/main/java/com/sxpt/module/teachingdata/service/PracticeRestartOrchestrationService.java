package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.dto.CreateStudentTaskLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;

/**
 * 学生重新练习编排服务。
 *
 * 业务功能：围绕一次历史练习分配记录创建新的练习 attempt，复用统一造数门面生成新的原平台业务数据，
 * 并把新数据重新分配给当前登录学生。
 *
 * 关键流程：校验当前学生、校验重练来源、继承旧分配和需求明细中的题目/流程/单位/角色上下文，
 * 调用 DataPrepareFacadeService 生成新数据，再调用 DataInstanceAllocationService 领取新实例，
 * 最后将旧分配历史化以避免新旧练习数据混用。
 */
public interface PracticeRestartOrchestrationService {

    /**
     * 基于指定历史分配记录重新生成练习数据。
     *
     * @param request 学生重练请求；前端只允许提交任务、场景、执行上下文和来源分配 ID。
     * @return 新生成并分配给当前学生的数据分配记录。
     */
    DataInstanceAllocation restartCurrentStudentTaskData(CreateStudentTaskLaunchRequest request);
}
