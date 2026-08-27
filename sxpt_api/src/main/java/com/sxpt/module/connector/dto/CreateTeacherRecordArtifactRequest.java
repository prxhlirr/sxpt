package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建老师备案产物请求。
 *
 * 业务功能：
 * 1. 承载老师备案 DataSession 沉淀为教学资产时的最小参数。
 * 2. 避免教学平台维护原平台模块路径，只围绕 DataSession 做资产沉淀。
 *
 * 关键流程：
 * 1. 原平台完成备案并注册 RECORD DataSession。
 * 2. 教学平台或遮罩层调用本接口，把该 DataSession 固化为老师备案产物。
 */
public class CreateTeacherRecordArtifactRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "数据会话 ID 不能为空")
    @Size(max = 64, message = "数据会话 ID 长度不能超过 64")
    private String dataSessionId;

    @NotBlank(message = "操作人 ID 不能为空")
    @Size(max = 64, message = "操作人 ID 长度不能超过 64")
    private String operatorId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getDataSessionId() {
        return dataSessionId;
    }

    public void setDataSessionId(String dataSessionId) {
        this.dataSessionId = dataSessionId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
}
