package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;

import java.util.List;

/**
 * 数据需求明细生成服务。
 *
 * 业务功能：
 * 1. 基于数据需求批次生成可提交给原平台的数据需求明细。
 * 2. 将学生、题目、单位、角色、动作和评分点快照固化为逐条约束，后续原平台只按这些约束创建真实业务数据。
 *
 * 关键流程：
 * 1. 调用方先创建 DataRequirement 批次。
 * 2. 调用本服务传入一组学生/单位/角色约束。
 * 3. 服务批量写入 DataRequirementItem，并回写批次 expectedCount。
 */
public interface DataRequirementGenerationService {

    /**
     * 按参与者约束生成数据需求明细。
     *
     * @param request 生成请求。
     * @return 生成结果。
     */
    GenerateResult generateRequirementItems(GenerateRequest request);

    /**
     * 数据需求明细生成请求。
     *
     * 业务功能：
     * 1. 承载批次 ID、批量请求 ID、创建人和参与者约束。
     * 2. 允许同一批次按不同触发来源生成不同的 requestBatchId，便于幂等和审计。
     */
    class GenerateRequest {

        private String requirementId;

        private String requestBatchId;

        private String createBy;

        private String updateBy;

        private List<ParticipantRequirement> participants;

        public String getRequirementId() {
            return requirementId;
        }

        public void setRequirementId(String requirementId) {
            this.requirementId = requirementId;
        }

        public String getRequestBatchId() {
            return requestBatchId;
        }

        public void setRequestBatchId(String requestBatchId) {
            this.requestBatchId = requestBatchId;
        }

        public String getCreateBy() {
            return createBy;
        }

        public void setCreateBy(String createBy) {
            this.createBy = createBy;
        }

        public String getUpdateBy() {
            return updateBy;
        }

        public void setUpdateBy(String updateBy) {
            this.updateBy = updateBy;
        }

        public List<ParticipantRequirement> getParticipants() {
            return participants;
        }

        public void setParticipants(List<ParticipantRequirement> participants) {
            this.participants = participants;
        }
    }

    /**
     * 单个学生或业务参与者的数据需求约束。
     *
     * 业务功能：
     * 1. 表达一个学生在指定题目、协作片段、单位和角色下需要的原平台业务数据。
     * 2. 保留动作约束和评分点快照，为后续校验和评分提供发布时依据。
     */
    class ParticipantRequirement {

        private String studentId;

        private String questionId;

        private String examAttemptId;

        private String questionAttemptId;

        private String collaborationUnitId;

        private Long segmentNo;

        private String actorType;

        private String ownerExternalOrgId;

        private String ownerExternalOrgName;

        private String requiredExternalOrgId;

        private String requiredExternalOrgName;

        private String requiredExternalRoleId;

        private String requiredExternalRoleName;

        private String dataScopeJson;

        private String requiredActionsJson;

        private String scorePointSnapshotJson;

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
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

        public String getCollaborationUnitId() {
            return collaborationUnitId;
        }

        public void setCollaborationUnitId(String collaborationUnitId) {
            this.collaborationUnitId = collaborationUnitId;
        }

        public Long getSegmentNo() {
            return segmentNo;
        }

        public void setSegmentNo(Long segmentNo) {
            this.segmentNo = segmentNo;
        }

        public String getActorType() {
            return actorType;
        }

        public void setActorType(String actorType) {
            this.actorType = actorType;
        }

        public String getOwnerExternalOrgId() {
            return ownerExternalOrgId;
        }

        public void setOwnerExternalOrgId(String ownerExternalOrgId) {
            this.ownerExternalOrgId = ownerExternalOrgId;
        }

        public String getOwnerExternalOrgName() {
            return ownerExternalOrgName;
        }

        public void setOwnerExternalOrgName(String ownerExternalOrgName) {
            this.ownerExternalOrgName = ownerExternalOrgName;
        }

        public String getRequiredExternalOrgId() {
            return requiredExternalOrgId;
        }

        public void setRequiredExternalOrgId(String requiredExternalOrgId) {
            this.requiredExternalOrgId = requiredExternalOrgId;
        }

        public String getRequiredExternalOrgName() {
            return requiredExternalOrgName;
        }

        public void setRequiredExternalOrgName(String requiredExternalOrgName) {
            this.requiredExternalOrgName = requiredExternalOrgName;
        }

        public String getRequiredExternalRoleId() {
            return requiredExternalRoleId;
        }

        public void setRequiredExternalRoleId(String requiredExternalRoleId) {
            this.requiredExternalRoleId = requiredExternalRoleId;
        }

        public String getRequiredExternalRoleName() {
            return requiredExternalRoleName;
        }

        public void setRequiredExternalRoleName(String requiredExternalRoleName) {
            this.requiredExternalRoleName = requiredExternalRoleName;
        }

        public String getDataScopeJson() {
            return dataScopeJson;
        }

        public void setDataScopeJson(String dataScopeJson) {
            this.dataScopeJson = dataScopeJson;
        }

        public String getRequiredActionsJson() {
            return requiredActionsJson;
        }

        public void setRequiredActionsJson(String requiredActionsJson) {
            this.requiredActionsJson = requiredActionsJson;
        }

        public String getScorePointSnapshotJson() {
            return scorePointSnapshotJson;
        }

        public void setScorePointSnapshotJson(String scorePointSnapshotJson) {
            this.scorePointSnapshotJson = scorePointSnapshotJson;
        }
    }

    /**
     * 数据需求明细生成结果。
     *
     * 业务功能：
     * 1. 返回被展开的批次和明细列表。
     * 2. 让调用方直接进入数据准备任务创建和编排执行。
     */
    class GenerateResult {

        private final DataRequirement requirement;

        private final List<DataRequirementItem> items;

        public GenerateResult(DataRequirement requirement, List<DataRequirementItem> items) {
            this.requirement = requirement;
            this.items = items;
        }

        public DataRequirement getRequirement() {
            return requirement;
        }

        public List<DataRequirementItem> getItems() {
            return items;
        }
    }
}
