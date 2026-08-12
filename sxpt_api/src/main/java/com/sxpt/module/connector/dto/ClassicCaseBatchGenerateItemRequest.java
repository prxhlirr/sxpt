package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 经典案例批量生成请求项。
 *
 * 业务功能：
 * 1. 表达一个学生、题目、角色上下文对应的一条 demo 数据生成需求。
 * 2. 让批量生成接口一次调用原平台学习环境，避免用循环单条请求伪装批量。
 *
 * 关键流程：
 * 1. requestItemId 用于和原平台逐条响应对账；调用方不传时由服务端生成。
 * 2. participantContextJson 只描述当前学生/题目的参与方上下文，不承载生成规则。
 */
public class ClassicCaseBatchGenerateItemRequest {

    @Size(max = 128, message = "请求明细 ID 长度不能超过 128")
    private String requestItemId;

    @NotBlank(message = "使用人不能为空")
    @Size(max = 64, message = "使用人长度不能超过 64")
    private String ownerUserId;

    @Size(max = 64, message = "题目 ID 长度不能超过 64")
    private String questionId;

    private String participantContextJson;

    private String requiredExternalOrgId;

    private String requiredExternalRoleId;

    private String actorType;

    public String getRequestItemId() {
        return requestItemId;
    }

    public void setRequestItemId(String requestItemId) {
        this.requestItemId = requestItemId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getParticipantContextJson() {
        return participantContextJson;
    }

    public void setParticipantContextJson(String participantContextJson) {
        this.participantContextJson = participantContextJson;
    }

    public String getRequiredExternalOrgId() {
        return requiredExternalOrgId;
    }

    public void setRequiredExternalOrgId(String requiredExternalOrgId) {
        this.requiredExternalOrgId = requiredExternalOrgId;
    }

    public String getRequiredExternalRoleId() {
        return requiredExternalRoleId;
    }

    public void setRequiredExternalRoleId(String requiredExternalRoleId) {
        this.requiredExternalRoleId = requiredExternalRoleId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }
}
