package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建原平台运行时启动请求。
 *
 * 业务功能：
 * 1. 承载 v2 方案中进入原平台首页所需的最小教学上下文。
 * 2. 不接收原平台模块路径和业务数据 ID，避免教学平台继续维护原平台内部页面结构。
 *
 * 关键流程：
 * 1. Controller 接收老师或学生进入原平台的请求。
 * 2. Service 根据 connectorSystemId 查询原平台首页地址。
 * 3. Service 创建平台首页 launchToken，并返回可直接跳转的 launchUrl。
 */
public class CreateOriginRuntimeLaunchRequest {

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @Size(max = 64, message = "执行 ID 长度不能超过 64")
    private String executionId;

    @Size(max = 64, message = "备案采集会话 ID 长度不能超过 64")
    private String captureSessionId;

    @Size(max = 64, message = "练习尝试 ID 长度不能超过 64")
    private String practiceAttemptId;

    @Size(max = 64, message = "考试尝试 ID 长度不能超过 64")
    private String examAttemptId;

    @Size(max = 64, message = "题目作答实例 ID 长度不能超过 64")
    private String questionAttemptId;

    @NotBlank(message = "场景类型不能为空")
    @Size(max = 32, message = "场景类型长度不能超过 32")
    private String sceneType;

    @NotBlank(message = "SDK 模式不能为空")
    @Size(max = 32, message = "SDK 模式长度不能超过 32")
    private String sdkMode;

    @NotBlank(message = "参与方类型不能为空")
    @Size(max = 64, message = "参与方类型长度不能超过 64")
    private String actorType;

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getCaptureSessionId() {
        return captureSessionId;
    }

    public void setCaptureSessionId(String captureSessionId) {
        this.captureSessionId = captureSessionId;
    }

    public String getPracticeAttemptId() {
        return practiceAttemptId;
    }

    public void setPracticeAttemptId(String practiceAttemptId) {
        this.practiceAttemptId = practiceAttemptId;
    }

    public String getExamAttemptId() {
        return examAttemptId;
    }

    public void setExamAttemptId(String examAttemptId) {
        this.examAttemptId = examAttemptId;
    }

    public String getQuestionAttemptId() {
        return questionAttemptId;
    }

    public void setQuestionAttemptId(String questionAttemptId) {
        this.questionAttemptId = questionAttemptId;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }
}
