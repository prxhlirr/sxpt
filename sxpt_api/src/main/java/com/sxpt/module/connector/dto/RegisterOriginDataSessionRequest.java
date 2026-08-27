package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 原平台数据会话注册请求。
 *
 * 业务功能：
 * 1. 承载原平台在业务页面完成造数或复制后返回给教学平台的业务数据引用。
 * 2. 使用 idempotencyKey 保证重复点击、刷新或重试不会产生重复 DataSession。
 *
 * 关键流程：
 * 1. 原平台读取 teachingSession 和页面教学标签。
 * 2. 原平台生成或复制业务数据后提交该请求。
 * 3. 教学平台解析 launchToken，幂等创建 TeachingDataInstance 和 OriginDataSession。
 */
public class RegisterOriginDataSessionRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "启动令牌不能为空")
    @Size(max = 512, message = "启动令牌长度不能超过 512")
    private String launchToken;

    @NotBlank(message = "业务场景编码不能为空")
    @Size(max = 128, message = "业务场景编码长度不能超过 128")
    private String businessSceneCode;

    @Size(max = 255, message = "业务场景名称长度不能超过 255")
    private String businessSceneName;

    @Size(max = 64, message = "来源数据会话 ID 长度不能超过 64")
    private String sourceDataSessionId;

    @Size(max = 128, message = "来源原平台业务数据 ID 长度不能超过 128")
    private String sourceExternalBusinessId;

    @Size(max = 64, message = "题目尝试 ID 长度不能超过 64")
    private String questionAttemptId;

    @NotBlank(message = "原平台业务数据 ID 不能为空")
    @Size(max = 128, message = "原平台业务数据 ID 长度不能超过 128")
    private String externalBusinessId;

    @Size(max = 128, message = "原平台业务单号长度不能超过 128")
    private String externalBusinessNo;

    @Size(max = 64, message = "原平台业务状态长度不能超过 64")
    private String externalStatus;

    @Size(max = 1024, message = "原平台业务入口地址长度不能超过 1024")
    private String entryUrl;

    @Size(max = 32768, message = "数据规格快照长度不能超过 32768")
    private String dataSpecSnapshotJson;

    @Size(max = 32768, message = "原平台载荷快照长度不能超过 32768")
    private String originPayloadSnapshotJson;

    @NotBlank(message = "幂等键不能为空")
    @Size(max = 255, message = "幂等键长度不能超过 255")
    private String idempotencyKey;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLaunchToken() {
        return launchToken;
    }

    public void setLaunchToken(String launchToken) {
        this.launchToken = launchToken;
    }

    public String getBusinessSceneCode() {
        return businessSceneCode;
    }

    public void setBusinessSceneCode(String businessSceneCode) {
        this.businessSceneCode = businessSceneCode;
    }

    public String getBusinessSceneName() {
        return businessSceneName;
    }

    public void setBusinessSceneName(String businessSceneName) {
        this.businessSceneName = businessSceneName;
    }

    public String getSourceDataSessionId() {
        return sourceDataSessionId;
    }

    public void setSourceDataSessionId(String sourceDataSessionId) {
        this.sourceDataSessionId = sourceDataSessionId;
    }

    public String getSourceExternalBusinessId() {
        return sourceExternalBusinessId;
    }

    public void setSourceExternalBusinessId(String sourceExternalBusinessId) {
        this.sourceExternalBusinessId = sourceExternalBusinessId;
    }

    public String getQuestionAttemptId() {
        return questionAttemptId;
    }

    public void setQuestionAttemptId(String questionAttemptId) {
        this.questionAttemptId = questionAttemptId;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getExternalBusinessNo() {
        return externalBusinessNo;
    }

    public void setExternalBusinessNo(String externalBusinessNo) {
        this.externalBusinessNo = externalBusinessNo;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public String getEntryUrl() {
        return entryUrl;
    }

    public void setEntryUrl(String entryUrl) {
        this.entryUrl = entryUrl;
    }

    public String getDataSpecSnapshotJson() {
        return dataSpecSnapshotJson;
    }

    public void setDataSpecSnapshotJson(String dataSpecSnapshotJson) {
        this.dataSpecSnapshotJson = dataSpecSnapshotJson;
    }

    public String getOriginPayloadSnapshotJson() {
        return originPayloadSnapshotJson;
    }

    public void setOriginPayloadSnapshotJson(String originPayloadSnapshotJson) {
        this.originPayloadSnapshotJson = originPayloadSnapshotJson;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
