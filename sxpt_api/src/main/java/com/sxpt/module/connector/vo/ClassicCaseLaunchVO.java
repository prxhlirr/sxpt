package com.sxpt.module.connector.vo;

/**
 * 经典案例生成并启动返回对象。
 *
 * 业务功能：
 * 1. 同时返回经典案例使用记录和原平台启动上下文，支撑前端一步进入原平台学习环境。
 * 2. usage 用于审计和展示生成结果，launchContext 用于跳转原平台并携带一次性 launchToken。
 *
 * 关键流程：
 * 1. 服务端先生成学习环境业务数据和 teaching_data_instance。
 * 2. 再复用现有 launchToken 链路创建启动上下文。
 */
public class ClassicCaseLaunchVO {

    private ClassicCaseUsageVO usage;

    private PlatformLaunchContextVO launchContext;

    public ClassicCaseUsageVO getUsage() {
        return usage;
    }

    public void setUsage(ClassicCaseUsageVO usage) {
        this.usage = usage;
    }

    public PlatformLaunchContextVO getLaunchContext() {
        return launchContext;
    }

    public void setLaunchContext(PlatformLaunchContextVO launchContext) {
        this.launchContext = launchContext;
    }
}
