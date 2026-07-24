package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.PlatformLaunchContext;

/**
 * 原平台启动上下文服务。
 *
 * 业务功能：
 * 1. 创建教学平台跳转原平台前所需的启动上下文。
 * 2. 生成一次性短 launchToken，并只将 token hash 保存到数据库。
 *
 * 关键流程：
 * 1. 调用方提交用户、原平台、场景、SDK 模式和目标地址等业务上下文。
 * 2. Service 生成明文 token、计算 hash、补齐过期时间和默认状态。
 * 3. 明文 token 只随本次返回值返回，后续数据库查询只依赖 hash。
 */
public interface PlatformLaunchContextService {

    /**
     * 创建原平台启动上下文并返回本次明文 launchToken。
     *
     * @param launchContext 原平台启动上下文，必须包含 ID、租户、用户、原平台、场景、SDK 模式和目标地址。
     * @return 已保存上下文与本次明文 launchToken。
     */
    CreatedLaunchContext createLaunchContext(PlatformLaunchContext launchContext);

    /**
     * 校验原平台提交的明文 launchToken 并返回启动上下文。
     *
     * @param tenantId 租户 ID，用于隔离不同租户下的 token。
     * @param launchToken 原平台提交的明文 launchToken。
     * @return 已校验的启动上下文。
     */
    PlatformLaunchContext verifyLaunchToken(String tenantId, String launchToken);

    /**
     * 标记原平台已完成 session 建立。
     *
     * @param id 启动上下文 ID。
     * @return 已标记使用的启动上下文。
     */
    PlatformLaunchContext markLaunchContextUsed(String id);

    /**
     * 标记原平台启动失败并记录失败原因。
     *
     * @param id 启动上下文 ID。
     * @param errorMessage 启动失败原因。
     * @return 已标记失败的启动上下文。
     */
    PlatformLaunchContext markLaunchContextFailed(String id, String errorMessage);

    /**
     * 已创建的启动上下文结果。
     *
     * 业务功能：
     * 1. 同时承载持久化后的启动上下文和只出现一次的明文 token。
     * 2. 防止调用方为了拿 token 明文而读取数据库字段，确保数据库只保存 hash。
     *
     * 关键流程：
     * 1. Service 创建上下文后构造该结果对象。
     * 2. Controller 后续只把 launchToken、expireTime 等必要字段返回给前端。
     */
    class CreatedLaunchContext {

        private final PlatformLaunchContext launchContext;

        private final String launchToken;

        /**
         * 构造已创建启动上下文结果。
         *
         * @param launchContext 已保存的启动上下文。
         * @param launchToken 本次明文 launchToken。
         */
        public CreatedLaunchContext(PlatformLaunchContext launchContext, String launchToken) {
            this.launchContext = launchContext;
            this.launchToken = launchToken;
        }

        public PlatformLaunchContext getLaunchContext() {
            return launchContext;
        }

        public String getLaunchToken() {
            return launchToken;
        }
    }
}
