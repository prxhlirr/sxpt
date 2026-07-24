package com.sxpt.module.practice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 练习过程分汇总实体。
 *
 * 业务功能：
 * 1. 映射 practice_score_summary 表，按学生、任务和教学点保存练习过程分。
 * 2. 汇总练习次数、完成率、最高分、最近分、平均分和薄弱步骤摘要。
 *
 * 关键流程：
 * 1. 教师点击重算或定时任务触发汇总生成。
 * 2. Service 读取 practice_attempt 和 practice_step_result 后按唯一范围幂等写入。
 */
@TableName("practice_score_summary")
public class PracticeScoreSummary {

    @TableId
    private String id;

    private String tenantId;

    private String studentId;

    private String classId;

    private String courseId;

    private String taskId;

    private String teachingPointId;

    private Long practiceCount;

    private Long completeCount;

    private BigDecimal bestScore;

    private BigDecimal lastScore;

    private BigDecimal avgScore;

    private BigDecimal completionRate;

    private BigDecimal finalPracticeScore;

    private String scorePolicy;

    private String weakStepJson;

    private LocalDateTime summaryTime;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String status;

    private Boolean deleted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
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

    public Long getPracticeCount() {
        return practiceCount;
    }

    public void setPracticeCount(Long practiceCount) {
        this.practiceCount = practiceCount;
    }

    public Long getCompleteCount() {
        return completeCount;
    }

    public void setCompleteCount(Long completeCount) {
        this.completeCount = completeCount;
    }

    public BigDecimal getBestScore() {
        return bestScore;
    }

    public void setBestScore(BigDecimal bestScore) {
        this.bestScore = bestScore;
    }

    public BigDecimal getLastScore() {
        return lastScore;
    }

    public void setLastScore(BigDecimal lastScore) {
        this.lastScore = lastScore;
    }

    public BigDecimal getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(BigDecimal avgScore) {
        this.avgScore = avgScore;
    }

    public BigDecimal getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(BigDecimal completionRate) {
        this.completionRate = completionRate;
    }

    public BigDecimal getFinalPracticeScore() {
        return finalPracticeScore;
    }

    public void setFinalPracticeScore(BigDecimal finalPracticeScore) {
        this.finalPracticeScore = finalPracticeScore;
    }

    public String getScorePolicy() {
        return scorePolicy;
    }

    public void setScorePolicy(String scorePolicy) {
        this.scorePolicy = scorePolicy;
    }

    public String getWeakStepJson() {
        return weakStepJson;
    }

    public void setWeakStepJson(String weakStepJson) {
        this.weakStepJson = weakStepJson;
    }

    public LocalDateTime getSummaryTime() {
        return summaryTime;
    }

    public void setSummaryTime(LocalDateTime summaryTime) {
        this.summaryTime = summaryTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
