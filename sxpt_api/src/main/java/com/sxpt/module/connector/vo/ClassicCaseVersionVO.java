package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 经典案例版本返回对象。
 *
 * 业务功能：
 * 1. 返回经典案例版本的摘要信息，支撑后台页面查看版本列表和当前版本。
 * 2. 默认不返回完整脱敏 payload，避免大 JSON 在列表和详情接口中被无意义传播。
 */
public class ClassicCaseVersionVO {

    private String id;

    private String caseAssetId;

    private Integer versionNo;

    private String payloadSchemaVersion;

    private String payloadHash;

    private String status;

    private String createBy;

    private LocalDateTime createTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
