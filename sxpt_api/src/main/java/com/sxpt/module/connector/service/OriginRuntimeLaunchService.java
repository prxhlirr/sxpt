package com.sxpt.module.connector.service;

import com.sxpt.module.connector.dto.CreateOriginRuntimeLaunchRequest;
import com.sxpt.module.connector.vo.OriginRuntimeLaunchVO;

/**
 * 原平台运行时启动服务。
 *
 * 业务功能：
 * 1. 提供 v2 方案的平台首页启动能力。
 * 2. 屏蔽底层 PlatformLaunchContext 的创建细节，避免业务入口继续传入原平台模块路径。
 *
 * 关键流程：
 * 1. 根据 connectorSystemId 查询原平台基础配置。
 * 2. 基于当前教学用户和请求场景创建平台首页 launchToken。
 * 3. 拼接原平台 `/teaching/launch` 地址返回给教学平台前端。
 */
public interface OriginRuntimeLaunchService {

    /**
     * 创建进入原平台首页的运行时启动地址。
     *
     * @param request 创建运行时启动请求。
     * @return 原平台运行时启动返回对象。
     */
    OriginRuntimeLaunchVO createLaunch(CreateOriginRuntimeLaunchRequest request);
}
