package com.sxpt.module.connector.service;

import com.sxpt.module.connector.dto.RegisterOriginDataSessionRequest;
import com.sxpt.module.connector.vo.OriginDataSessionVO;

/**
 * 原平台数据会话服务。
 *
 * 业务功能：
 * 1. 接收原平台自造数或复制数据后的注册请求。
 * 2. 幂等创建教学数据实例和原平台数据会话。
 *
 * 关键流程：
 * 1. 解析 launchToken 得到教学上下文。
 * 2. 保存或复用 TeachingDataInstance。
 * 3. 保存或复用 OriginDataSession。
 * 4. 返回遮罩层初始化所需的绑定上下文。
 */
public interface OriginDataSessionService {

    /**
     * 注册原平台数据会话。
     *
     * @param request 原平台数据会话注册请求。
     * @return 原平台数据会话返回对象。
     */
    OriginDataSessionVO register(RegisterOriginDataSessionRequest request);
}
