package com.sxpt.module.capture.vo;

import java.util.List;

/**
 * 老师备案编排结果返回对象。
 *
 * 业务功能：
 * 1. 汇总一次老师备案编排的阶段执行结果，支撑教师端展示“哪些资产已经准备好、哪些仍需修复”。
 * 2. 避免前端直接理解采集、资源、教学步骤和评分项多个底层服务的内部细节。
 *
 * 关键流程：
 * 1. 编排服务按采集会话生成动作草稿，并推动已确认草稿发布为教学资产。
 * 2. 编排服务调用发布前完整性校验后，将校验结果和缺陷清单写入本对象。
 */
public class TeacherFilingOrchestrationResultVO {

    private String captureSessionId;

    private String taskId;

    private String teachingPointId;

    private String evaluationRuleId;

    private Integer generatedDraftCount;

    private Integer publishedStepCount;

    private Integer backfilledEvaluationItemCount;

    private Boolean readyToPublish;

    private List<String> defectMessages;

    public String getCaptureSessionId() {
        return captureSessionId;
    }

    public void setCaptureSessionId(String captureSessionId) {
        this.captureSessionId = captureSessionId;
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

    public String getEvaluationRuleId() {
        return evaluationRuleId;
    }

    public void setEvaluationRuleId(String evaluationRuleId) {
        this.evaluationRuleId = evaluationRuleId;
    }

    public Integer getGeneratedDraftCount() {
        return generatedDraftCount;
    }

    public void setGeneratedDraftCount(Integer generatedDraftCount) {
        this.generatedDraftCount = generatedDraftCount;
    }

    public Integer getPublishedStepCount() {
        return publishedStepCount;
    }

    public void setPublishedStepCount(Integer publishedStepCount) {
        this.publishedStepCount = publishedStepCount;
    }

    public Integer getBackfilledEvaluationItemCount() {
        return backfilledEvaluationItemCount;
    }

    public void setBackfilledEvaluationItemCount(Integer backfilledEvaluationItemCount) {
        this.backfilledEvaluationItemCount = backfilledEvaluationItemCount;
    }

    public Boolean getReadyToPublish() {
        return readyToPublish;
    }

    public void setReadyToPublish(Boolean readyToPublish) {
        this.readyToPublish = readyToPublish;
    }

    public List<String> getDefectMessages() {
        return defectMessages;
    }

    public void setDefectMessages(List<String> defectMessages) {
        this.defectMessages = defectMessages;
    }
}
