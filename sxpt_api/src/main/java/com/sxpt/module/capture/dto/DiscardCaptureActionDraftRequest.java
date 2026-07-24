package com.sxpt.module.capture.dto;

import javax.validation.constraints.Size;

/**
 * 丢弃页面动作草稿请求。
 *
 * 业务功能：
 * 1. 承载教师丢弃动作草稿时的更新人信息。
 * 2. 保持丢弃操作显式经过接口调用，避免系统自动删除采集事实。
 *
 * 关键流程：
 * 1. Controller 接收请求并传递更新人 ID。
 * 2. Service 仅允许 PENDING 草稿流转为 DISCARDED，保留草稿记录用于追溯。
 */
public class DiscardCaptureActionDraftRequest {

    @Size(max = 64, message = "更新人 ID 长度不能超过 64")
    private String updateBy;

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
