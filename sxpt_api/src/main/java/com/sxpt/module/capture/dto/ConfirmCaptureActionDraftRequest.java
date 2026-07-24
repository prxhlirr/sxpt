package com.sxpt.module.capture.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 确认页面动作草稿请求。
 *
 * 业务功能：
 * 1. 承载教师确认动作草稿时填写或修正的业务操作名称、教学步骤名称和提示内容。
 * 2. 将系统生成建议与教师最终确认结果分离，保证发布教学步骤时使用教师确认内容。
 * 3. 允许教师在确认时绑定正式资源，保证后续发布教学步骤可以引用稳定资源。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation 基础校验。
 * 2. Service 仅允许 PENDING 草稿流转为 CONFIRMED，避免重复确认覆盖结果。
 */
public class ConfirmCaptureActionDraftRequest {

    @NotBlank(message = "教师确认业务操作名称不能为空")
    @Size(max = 128, message = "教师确认业务操作名称长度不能超过 128")
    private String confirmedOperationName;

    @NotBlank(message = "教师确认教学步骤名称不能为空")
    @Size(max = 128, message = "教师确认教学步骤名称长度不能超过 128")
    private String confirmedStepName;

    @Size(max = 4096, message = "学习提示长度不能超过 4096")
    private String guideContent;

    @Size(max = 4096, message = "练习提示长度不能超过 4096")
    private String practiceHint;

    @Size(max = 64, message = "正式资源 ID 长度不能超过 64")
    private String connectorResourceId;

    @Size(max = 64, message = "更新人 ID 长度不能超过 64")
    private String updateBy;

    public String getConfirmedOperationName() {
        return confirmedOperationName;
    }

    public void setConfirmedOperationName(String confirmedOperationName) {
        this.confirmedOperationName = confirmedOperationName;
    }

    public String getConfirmedStepName() {
        return confirmedStepName;
    }

    public void setConfirmedStepName(String confirmedStepName) {
        this.confirmedStepName = confirmedStepName;
    }

    public String getGuideContent() {
        return guideContent;
    }

    public void setGuideContent(String guideContent) {
        this.guideContent = guideContent;
    }

    public String getPracticeHint() {
        return practiceHint;
    }

    public void setPracticeHint(String practiceHint) {
        this.practiceHint = practiceHint;
    }

    public String getConnectorResourceId() {
        return connectorResourceId;
    }

    public void setConnectorResourceId(String connectorResourceId) {
        this.connectorResourceId = connectorResourceId;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
