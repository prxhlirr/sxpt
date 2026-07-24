package com.sxpt.module.practice.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 练习过程分汇总返回对象。
 *
 * 业务功能：
 * 1. 向教师端展示学生在某任务教学点下的练习过程分。
 * 2. 隐藏 deleted 等内部持久化字段，保持接口契约稳定。
 *
 * 关键流程：
 * 1. Controller 从 PracticeScoreSummary 实体提取可展示字段。
 * 2. 前端使用 practiceCount、completionRate 和 weakStepJson 展示练习画像。
 */
public class PracticeScoreSummaryVO {

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

    private String status;

    private LocalDateTime createTime;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
