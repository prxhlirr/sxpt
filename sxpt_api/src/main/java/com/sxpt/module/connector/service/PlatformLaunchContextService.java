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
     * 创建进入原平台首页的启动上下文并返回本次明文 launchToken。
     *
     * 业务功能：
     * 1. 支撑 v2 方案中“教学平台只进入原平台首页”的新链路。
     * 2. 不要求启动前已经存在 TeachingDataInstance，避免教学平台继续维护原平台模块路径和造数结果。
     *
     * 关键流程：
     * 1. 调用方提交用户、原平台、场景、SDK 模式和原平台首页地址。
     * 2. Service 生成明文 token、保存 token hash，并把 targetUrl 固定为原平台首页。
     * 3. 原平台后续通过 verify 获取教学上下文，再在用户点击页面时复制或生成业务数据。
     *
     * @param launchContext 平台首页启动上下文，必须包含租户、用户、原平台、场景、SDK 模式和原平台首页地址。
     * @return 已保存上下文与本次明文 launchToken。
     */
    CreatedLaunchContext createPlatformHomeLaunchContext(PlatformLaunchContext launchContext);

    /**
     * 校验原平台提交的明文 launchToken 并返回启动上下文。
     *
     * @param tenantId 租户 ID，用于隔离不同租户下的 token。
     * @param launchToken 原平台提交的明文 launchToken。
     * @return 已校验的启动上下文。
     */
    PlatformLaunchContext verifyLaunchToken(String tenantId, String launchToken);

    /**
     * 解析原平台提交的明文 launchToken，但不推进启动上下文状态。
     *
     * 业务功能：
     * 1. 支撑原平台在已建立 teachingSession 后继续使用 launchToken 注册 DataSession。
     * 2. 避免 DataSession 注册重复调用 verify 导致 CREATED 之外的状态被拒绝。
     *
     * 关键流程：
     * 1. 根据租户和 token hash 定位启动上下文。
     * 2. 校验未删除、未过期、未失败。
     * 3. 返回启动上下文，不更新 verifiedTime、verifyRequestId 和 launchStatus。
     *
     * @param tenantId 租户 ID。
     * @param launchToken 明文 launchToken。
     * @return 可用于运行时注册的启动上下文。
     */
    PlatformLaunchContext resolveLaunchToken(String tenantId, String launchToken);

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
