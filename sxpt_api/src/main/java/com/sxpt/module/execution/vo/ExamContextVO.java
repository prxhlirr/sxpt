package com.sxpt.module.execution.vo;

/**
 * 考试模式上下文返回对象。
 *
 * 业务功能：
 * 1. 向 SDK 返回考试模式所需的考试上下文、资源定位、评分快照和静默采集策略。
 * 2. 隔离通用 runtime-context 与考试模式专用响应，防止 SDK 在考试中启用提示和强引导。
 *
 * 关键流程：
 * 1. ExamContextService 校验 runtime-context 的 sdkMode 为 EXAM。
 * 2. Controller 返回本 VO，SDK 按考试上下文执行无提示和静默采集流程。
 */
public class ExamContextVO {

    private String contextId;

    private String tenantId;

    private String executionId;

    private String taskId;

    private String studentId;

    private String sdkMode;

    private String examContextJson;

    private String resourceSnapshotJson;

    private String evaluationSnapshotJson;

    private String silentCapturePolicyJson;

    private Boolean hintDisabled;

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getExamContextJson() {
        return examContextJson;
    }

    public void setExamContextJson(String examContextJson) {
        this.examContextJson = examContextJson;
    }

    public String getResourceSnapshotJson() {
        return resourceSnapshotJson;
    }

    public void setResourceSnapshotJson(String resourceSnapshotJson) {
        this.resourceSnapshotJson = resourceSnapshotJson;
    }

    public String getEvaluationSnapshotJson() {
        return evaluationSnapshotJson;
    }

    public void setEvaluationSnapshotJson(String evaluationSnapshotJson) {
        this.evaluationSnapshotJson = evaluationSnapshotJson;
    }

    public String getSilentCapturePolicyJson() {
        return silentCapturePolicyJson;
    }

    public void setSilentCapturePolicyJson(String silentCapturePolicyJson) {
        this.silentCapturePolicyJson = silentCapturePolicyJson;
    }

    public Boolean getHintDisabled() {
        return hintDisabled;
    }

    public void setHintDisabled(Boolean hintDisabled) {
        this.hintDisabled = hintDisabled;
    }
}
