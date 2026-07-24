package com.sxpt.module.execution.vo;

/**
 * 练习模式上下文返回对象。
 *
 * 业务功能：
 * 1. 向 SDK 返回练习模式所需的弱提示、资源定位、练习上下文和重开策略快照。
 * 2. 隔离通用 runtime-context 与练习模式专用响应，便于后续接入练习次数和练习结果。
 *
 * 关键流程：
 * 1. PracticeContextService 校验 runtime-context 的 sdkMode 为 PRACTICE。
 * 2. Controller 返回本 VO，SDK 按练习上下文执行弱提示和独立练习流程。
 */
public class PracticeContextVO {

    private String contextId;

    private String tenantId;

    private String executionId;

    private String taskId;

    private String studentId;

    private String sdkMode;

    private String practiceContextJson;

    private String overlayPolicyJson;

    private String teachingPointSnapshotJson;

    private String resourceSnapshotJson;

    private String hintPolicyJson;

    private String retryPolicyJson;

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

    public String getPracticeContextJson() {
        return practiceContextJson;
    }

    public void setPracticeContextJson(String practiceContextJson) {
        this.practiceContextJson = practiceContextJson;
    }

    public String getOverlayPolicyJson() {
        return overlayPolicyJson;
    }

    public void setOverlayPolicyJson(String overlayPolicyJson) {
        this.overlayPolicyJson = overlayPolicyJson;
    }

    public String getTeachingPointSnapshotJson() {
        return teachingPointSnapshotJson;
    }

    public void setTeachingPointSnapshotJson(String teachingPointSnapshotJson) {
        this.teachingPointSnapshotJson = teachingPointSnapshotJson;
    }

    public String getResourceSnapshotJson() {
        return resourceSnapshotJson;
    }

    public void setResourceSnapshotJson(String resourceSnapshotJson) {
        this.resourceSnapshotJson = resourceSnapshotJson;
    }

    public String getHintPolicyJson() {
        return hintPolicyJson;
    }

    public void setHintPolicyJson(String hintPolicyJson) {
        this.hintPolicyJson = hintPolicyJson;
    }

    public String getRetryPolicyJson() {
        return retryPolicyJson;
    }

    public void setRetryPolicyJson(String retryPolicyJson) {
        this.retryPolicyJson = retryPolicyJson;
    }
}
