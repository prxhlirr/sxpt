package com.sxpt.module.connector.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 替换考试题目业务场景请求。
 *
 * 业务功能：
 * 1. 承载考试发布或组卷后需要固化的题目业务场景清单。
 * 2. 支撑学生考试时按题目造数和评分，不依赖原平台模块路径维护。
 *
 * 关键流程：
 * 1. 考试发布方提交 examAttemptId 和题目清单。
 * 2. 服务全量替换 exam_question_scene。
 * 3. 学生进入原平台后 verify launchToken 获取该清单。
 */
public class ReplaceExamQuestionScenesRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "任务执行 ID 长度不能超过 64")
    private String executionId;

    @NotBlank(message = "考试尝试 ID 不能为空")
    @Size(max = 64, message = "考试尝试 ID 长度不能超过 64")
    private String examAttemptId;

    @NotBlank(message = "操作人 ID 不能为空")
    @Size(max = 64, message = "操作人 ID 长度不能超过 64")
    private String operatorId;

    @Valid
    @NotEmpty(message = "考试题目业务场景不能为空")
    private List<ExamQuestionSceneItemRequest> scenes;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getExamAttemptId() {
        return examAttemptId;
    }

    public void setExamAttemptId(String examAttemptId) {
        this.examAttemptId = examAttemptId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public List<ExamQuestionSceneItemRequest> getScenes() {
        return scenes;
    }

    public void setScenes(List<ExamQuestionSceneItemRequest> scenes) {
        this.scenes = scenes;
    }
}
