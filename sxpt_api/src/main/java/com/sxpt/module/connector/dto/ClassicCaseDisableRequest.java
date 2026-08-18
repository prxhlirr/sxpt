package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** OA 原平台停用经典案例请求。 */
public class ClassicCaseDisableRequest {

    @NotBlank(message = "案例编码不能为空")
    @Size(max = 128, message = "案例编码长度不能超过 128")
    private String caseCode;

    @NotBlank(message = "停用原因不能为空")
    @Size(max = 1024, message = "停用原因长度不能超过 1024")
    private String reason;

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
