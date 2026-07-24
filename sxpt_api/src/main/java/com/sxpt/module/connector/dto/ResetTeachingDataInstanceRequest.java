package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 重置教学业务数据实例请求。
 *
 * 业务功能：
 * 1. 承载练习或异常场景重新创建原平台业务数据后的新引用。
 * 2. 使用 Bean Validation 在接口层拦截缺失的新业务数据 ID 和过大的元数据摘要。
 *
 * 关键流程：
 * 1. Controller 接收请求并校验新的原平台业务数据引用。
 * 2. Controller 调用 Service 替换实例中的原平台业务数据摘要并累计重置次数。
 */
public class ResetTeachingDataInstanceRequest {

    @NotBlank(message = "原平台业务数据 ID 不能为空")
    @Size(max = 128, message = "原平台业务数据 ID 长度不能超过 128")
    private String externalBusinessId;

    @Size(max = 128, message = "原平台业务单据号长度不能超过 128")
    private String externalBusinessNo;

    @Size(max = 64, message = "原平台业务状态长度不能超过 64")
    private String externalStatus;

    @Size(max = 32768, message = "元数据摘要长度不能超过 32768")
    private String metadataJson;

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

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
