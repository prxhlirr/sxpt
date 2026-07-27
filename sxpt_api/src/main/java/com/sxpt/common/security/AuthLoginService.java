package com.sxpt.common.security;

import com.sxpt.controller.AuthLoginRequest;
import com.sxpt.controller.AuthLoginResponse;

/**
 * 正式登录服务。
 *
 * 业务功能：
 * 1. 校验教学平台用户登录凭据。
 * 2. 登录成功后签发 JWT，并返回当前用户摘要。
 *
 * 关键流程：
 * 1. 根据 loginType 选择认证策略。
 * 2. PASSWORD 登录校验 teach_user 中的用户名、租户、密码状态和密码哈希。
 * 3. 后续 SSO 或原平台票据登录必须在该服务下扩展，不允许 Controller 直接签发 Token。
 */
public interface AuthLoginService {

    /**
     * 执行登录。
     *
     * @param request 登录请求。
     * @return 登录成功后的 Token 和用户摘要。
     */
    AuthLoginResponse login(AuthLoginRequest request);
}
