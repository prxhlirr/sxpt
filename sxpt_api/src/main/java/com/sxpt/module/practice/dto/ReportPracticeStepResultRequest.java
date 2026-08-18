package com.sxpt.module.practice.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 练习步骤结果上报请求。
 *
 * 业务功能：
 * 1. 承载某次练习中单个关键步骤的完成状态、得分和证据摘要。
 * 2. 以 attemptId 和 taskStepId 作为幂等业务键，避免 SDK 重试造成重复步骤结果。
 *
 * 关键流程：
 * 1. Controller 先执行 Bean Validation，拒绝缺少核心上下文的请求。
 * 2. Service 再按业务键查找已有结果，存在则更新，不存在则插入。
 */
public class ReportPracticeStepResultRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "练习次数 ID 不能为空")
    @Size(max = 64, message = "练习次数 ID 长度不能超过 64")
    private String attemptId;

    @Size(max = 64, message = "执行 ID 长度不能超过 64")
    private String executionId;

    @Size(max = 64, message = "学生 ID 长度不能超过 64")
    private String studentId;

    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @NotBlank(message = "任务步骤 ID 不能为空")
    @Size(max = 64, message = "任务步骤 ID 长度不能超过 64")
    private String taskStepId;

    @Size(max = 64, message = "步骤编码长度不能超过 64")
    private String stepCode;

    @NotNull(message = "步骤序号不能为空")
    @Min(value = 1, message = "步骤序号必须从 1 开始")
    private Long sequenceNo;

    @NotBlank(message = "步骤结果状态不能为空")
    @Size(max = 32, message = "步骤结果状态长度不能超过 32")
    private String resultStatus;

    private BigDecimal score;

    private BigDecimal maxScore;

    private Boolean passFlag;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long errorCount;

    private Long hintCount;

    private Long retryCount;

    private String evidenceJson;

    @Size(max = 512, message = "反馈文本长度不能超过 512")
    private String feedback;

    @Size(max = 64, message = "创建人长度不能超过 64")
    private String createBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    public String getTaskStepId() {
        return taskStepId;
    }

    public void setTaskStepId(String taskStepId) {
        this.taskStepId = taskStepId;
    }

    public String getStepCode() {
        return stepCode;
    }

    public void setStepCode(String stepCode) {
        this.stepCode = stepCode;
    }

    public Long getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Long sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public Boolean getPassFlag() {
        return passFlag;
    }

    public void setPassFlag(Boolean passFlag) {
        this.passFlag = passFlag;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
    }

    public Long getHintCount() {
        return hintCount;
    }

    public void setHintCount(Long hintCount) {
        this.hintCount = hintCount;
    }

    public Long getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Long retryCount) {
        this.retryCount = retryCount;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
