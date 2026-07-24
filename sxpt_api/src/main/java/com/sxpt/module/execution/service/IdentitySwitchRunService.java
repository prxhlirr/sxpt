package com.sxpt.module.execution.service;

import com.sxpt.module.execution.dto.IdentitySwitchRunRequest;
import com.sxpt.module.execution.vo.IdentitySwitchRunVO;

/**
 * 身份切换运行服务。
 *
 * 业务功能：
 * 1. 支撑 SDK 在学习、练习或考试过程中按 task_step 切换原平台单位和角色。
 * 2. 复用 launchToken 安全能力，不在身份切换模块重复实现令牌生成和校验。
 *
 * 关键流程：
 * 1. 根据任务和教学点查询步骤列表。
 * 2. 校验调用方指定的下一步骤存在且配置了原平台单位和角色。
 * 3. 创建下一步骤原平台启动上下文并返回明文 launchToken。
 */
public interface IdentitySwitchRunService {

    /**
     * 创建下一步骤身份切换运行上下文。
     *
     * @param request 身份切换运行请求。
     * @return 身份切换运行结果。
     */
    IdentitySwitchRunVO runSwitch(IdentitySwitchRunRequest request);
}
