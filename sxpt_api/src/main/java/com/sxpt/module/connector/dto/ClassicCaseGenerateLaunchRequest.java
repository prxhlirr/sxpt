package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 经典案例生成并启动请求。
 *
 * 业务功能：
 * 1. 在经典案例生成学习环境业务数据后，立即生成进入原平台所需的 launchToken。
 * 2. 避免前端先调生成接口、再手工拼装启动上下文，减少老师备案/教学和学生练习入口的割裂感。
 *
 * 关键流程：
 * 1. 继承经典案例生成请求字段，保持老师复刻和学生 demo 的载荷分流规则不变。
 * 2. 额外提供 sdkMode 和 segmentNo，用于创建平台启动上下文。
 */
public class ClassicCaseGenerateLaunchRequest extends ClassicCaseGenerateRequest {

    @NotBlank(message = "SDK 模式不能为空")
    @Size(max = 32, message = "SDK 模式长度不能超过 32")
    private String sdkMode;

    private Long segmentNo;

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public Long getSegmentNo() {
        return segmentNo;
    }

    public void setSegmentNo(Long segmentNo) {
        this.segmentNo = segmentNo;
    }
}
