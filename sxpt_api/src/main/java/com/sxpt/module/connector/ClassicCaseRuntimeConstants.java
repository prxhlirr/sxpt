package com.sxpt.module.connector;

/**
 * 经典案例运行态常量。
 *
 * 业务功能：
 * 1. 集中管理经典案例生成、启动和请求快照中的稳定编码，避免 Service、Controller 和测试中散落魔法字符串。
 * 2. 第一阶段仅开放数据创建链路，其它校验、归档、锁定类能力继续隐藏，降低经典案例闭环的认知和实现复杂度。
 *
 * 关键流程：
 * 1. 老师/专家复刻使用 TEACHING_REPLICA，携带完整脱敏 payload。
 * 2. 学生练习使用 STUDENT_DEMO，仅携带数据格式，由原平台学习环境自主生成 demo 数据。
 */
public final class ClassicCaseRuntimeConstants {

    public static final String REQUEST_MODE_CLASSIC_CASE = "CLASSIC_CASE";

    public static final String GENERATION_SOURCE_CLASSIC_CASE = "CLASSIC_CASE";

    public static final String GENERATION_MODE_REPLAY_CASE = "REPLAY_CASE";

    public static final String GENERATION_MODE_FORMAT_DEMO = "FORMAT_DEMO";

    public static final String USAGE_SCENE_TEACHING_REPLICA = "TEACHING_REPLICA";

    public static final String USAGE_SCENE_STUDENT_DEMO = "STUDENT_DEMO";

    public static final String STATUS_SUCCESS = "SUCCESS";

    public static final String TEMPLATE_ID_PREFIX = "classic-case:";

    private ClassicCaseRuntimeConstants() {
    }
}
