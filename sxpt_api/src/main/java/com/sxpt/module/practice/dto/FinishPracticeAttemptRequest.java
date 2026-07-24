package com.sxpt.module.practice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 完成练习次数请求。
 *
 * 业务功能：
 * 1. 承载一次练习结束时回写状态、得分和过程统计所需的数据。
 * 2. 支持 COMPLETED、ABANDONED、ERROR 三种结束状态，RUNNING 只能由开始接口创建。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation。
 * 2. Service 校验当前练习仍处于 RUNNING，再回写结束时间、耗时和统计结果。
 */
public class FinishPracticeAttemptRequest {

    @NotBlank(message = "练习次数 ID 不能为空")
    @Size(max = 64, message = "练习次数 ID 长度不能超过 64")
    private String id;

    @NotBlank(message = "结束状态不能为空")
    @Size(max = 32, message = "结束状态长度不能超过 32")
    private String attemptStatus;

    private LocalDateTime endTime;

    private BigDecimal score;

    private BigDecimal maxScore;

    private Boolean passFlag;

    private Long errorCount;

    private Long hintCount;

    private Long rollbackCount;

    @Size(max = 64, message = "更新人 ID 长度不能超过 64")
    private String updateBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAttemptStatus() {
        return attemptStatus;
    }

    public void setAttemptStatus(String attemptStatus) {
        this.attemptStatus = attemptStatus;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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

    public Long getRollbackCount() {
        return rollbackCount;
    }

    public void setRollbackCount(Long rollbackCount) {
        this.rollbackCount = rollbackCount;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
