package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 经典案例版本实体。
 *
 * 业务功能：
 * 1. 映射 classic_case_version 表，保存每次原平台推送形成的脱敏案例版本。
 * 2. 区分老师/专家复刻使用的完整脱敏 payload 和学生练习使用的数据格式。
 *
 * 关键流程：
 * 1. 同一 caseAssetId 每次导入新增一个 versionNo。
 * 2. desensitizedCasePayloadJson 用于学习环境一比一复刻，caseDataFormatJson 用于学生 demo 数据生成。
 */
@TableName("classic_case_version")
public class ClassicCaseVersion {

    @TableId
    private String id;

    private String tenantId;

    private String caseAssetId;

    private Integer versionNo;

    private String payloadSchemaVersion;

    private String desensitizedCasePayloadJson;

    private String caseDataFormatJson;

    private String identityBindingJson;

    private String desensitizePolicyJson;

    private String payloadHash;

    private String createBy;

    private LocalDateTime createTime;

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

    public String getCaseAssetId() {
        return caseAssetId;
    }

    public void setCaseAssetId(String caseAssetId) {
        this.caseAssetId = caseAssetId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getPayloadSchemaVersion() {
        return payloadSchemaVersion;
    }

    public void setPayloadSchemaVersion(String payloadSchemaVersion) {
        this.payloadSchemaVersion = payloadSchemaVersion;
    }

    public String getDesensitizedCasePayloadJson() {
        return desensitizedCasePayloadJson;
    }

    public void setDesensitizedCasePayloadJson(String desensitizedCasePayloadJson) {
        this.desensitizedCasePayloadJson = desensitizedCasePayloadJson;
    }

    public String getCaseDataFormatJson() {
        return caseDataFormatJson;
    }

    public void setCaseDataFormatJson(String caseDataFormatJson) {
        this.caseDataFormatJson = caseDataFormatJson;
    }

    public String getIdentityBindingJson() {
        return identityBindingJson;
    }

    public void setIdentityBindingJson(String identityBindingJson) {
        this.identityBindingJson = identityBindingJson;
    }

    public String getDesensitizePolicyJson() {
        return desensitizePolicyJson;
    }

    public void setDesensitizePolicyJson(String desensitizePolicyJson) {
        this.desensitizePolicyJson = desensitizePolicyJson;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
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
