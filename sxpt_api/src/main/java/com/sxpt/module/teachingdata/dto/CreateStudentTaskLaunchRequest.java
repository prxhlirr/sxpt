package com.sxpt.module.teachingdata.dto;

/**
 * 学生按教学任务进入原平台的启动请求。
 *
 * 业务功能：
 * 1. 承载学生端进入原平台时允许前端提交的最小业务参数。
 * 2. 前端只提交任务、场景和执行上下文，不提交学生 ID、分配记录 ID、原平台单位或角色。
 *
 * 关键流程：
 * 1. Service 从服务端登录态解析当前学生身份。
 * 2. Service 按任务和场景查询该学生已分配的数据实例。
 * 3. Service 基于分配记录创建 launchToken 并返回原平台跳转地址。
 */
public class CreateStudentTaskLaunchRequest {

    private String taskId;

    private String sceneType;

    private String executionId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
}
