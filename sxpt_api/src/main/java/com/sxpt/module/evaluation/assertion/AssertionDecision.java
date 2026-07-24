package com.sxpt.module.evaluation.assertion;

/**
 * 评分断言判定结果。
 *
 * 业务功能：
 * 1. 承载单个评分项是否命中、未命中原因和证据引用。
 * 2. 让不同断言策略返回统一结果，避免自动评分主流程感知每种断言的内部细节。
 *
 * 关键流程：
 * 1. 策略命中时返回 matched，并按证据类型携带 traceId 或 apiResourceId。
 * 2. 策略未命中时返回 notMatched，并携带稳定 reason 供 evidenceJson 复核。
 */
public class AssertionDecision {

    private final boolean matched;

    private final String reason;

    private final String hitTraceId;

    private final String apiResourceId;

    private AssertionDecision(boolean matched, String reason, String hitTraceId, String apiResourceId) {
        this.matched = matched;
        this.reason = reason;
        this.hitTraceId = hitTraceId;
        this.apiResourceId = apiResourceId;
    }

    public static AssertionDecision matchedTrace(String hitTraceId) {
        return new AssertionDecision(true, "MATCHED", hitTraceId, null);
    }

    public static AssertionDecision matchedExternal(String apiResourceId) {
        return new AssertionDecision(true, "MATCHED", null, apiResourceId);
    }

    public static AssertionDecision notMatched(String reason) {
        return new AssertionDecision(false, reason, null, null);
    }

    public static AssertionDecision notMatched(String reason, String hitTraceId, String apiResourceId) {
        return new AssertionDecision(false, reason, hitTraceId, apiResourceId);
    }

    public boolean isMatched() {
        return matched;
    }

    public String getReason() {
        return reason;
    }

    public String getHitTraceId() {
        return hitTraceId;
    }

    public String getApiResourceId() {
        return apiResourceId;
    }
}
