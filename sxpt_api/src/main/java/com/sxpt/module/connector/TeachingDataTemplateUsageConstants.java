package com.sxpt.module.connector;

/**
 * 教学数据模板用途常量。
 *
 * 业务功能：
 * 1. 区分普通造数模板、经典案例还原模板和经典案例 demo 模板。
 * 2. 避免使用 supportMode 混承载模板用途，保持历史普通造数语义稳定。
 *
 * 关键流程：
 * 1. 普通模板默认使用 NORMAL，保证已接入系统无需调整。
 * 2. 经典案例运行时按使用场景映射为 CLASSIC_CASE_REPLAY 或 CLASSIC_CASE_DEMO。
 */
public final class TeachingDataTemplateUsageConstants {

    public static final String NORMAL = "NORMAL";

    public static final String CLASSIC_CASE_REPLAY = "CLASSIC_CASE_REPLAY";

    public static final String CLASSIC_CASE_DEMO = "CLASSIC_CASE_DEMO";

    private TeachingDataTemplateUsageConstants() {
    }
}
