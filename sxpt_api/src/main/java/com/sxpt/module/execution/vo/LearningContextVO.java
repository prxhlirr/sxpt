package com.sxpt.module.execution.vo;

/**
 * 学习模式上下文返回对象。
 *
 * 业务功能：
 * 1. 向 SDK 返回学习模式所需的步骤说明、遮罩策略、教学点和资源高亮快照。
 * 2. 隔离通用 runtime-context 与学习模式专用响应，便于后续扩展练习和考试模式。
 *
 * 关键流程：
 * 1. LearningContextService 校验 runtime-context 的 sdkMode 为 LEARNING。
 * 2. Controller 返回本 VO，SDK 按学习上下文展示引导和高亮。
 */
public class LearningContextVO {

    private String contextId;

    private String tenantId;

    private String executionId;

    private String taskId;

    private String studentId;

    private String sdkMode;

    private String learningContextJson;

    private String overlayPolicyJson;

    private String teachingPointSnapshotJson;

    private String resourceSnapshotJson;

    private String guideSnapshotJson;

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

    public String getLearningContextJson() {
        return learningContextJson;
    }

    public void setLearningContextJson(String learningContextJson) {
        this.learningContextJson = learningContextJson;
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

    public String getGuideSnapshotJson() {
        return guideSnapshotJson;
    }

    public void setGuideSnapshotJson(String guideSnapshotJson) {
        this.guideSnapshotJson = guideSnapshotJson;
    }
}
