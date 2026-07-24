package com.sxpt.module.capture.enums;

import org.springframework.util.StringUtils;

/**
 * 采集动作草稿类型枚举。
 *
 * 业务功能：
 * 1. 统一定义从 SDK 采集事件转换成动作草稿时允许自动生成的动作类型。
 * 2. 为后续草稿生成、草稿确认、步骤发布和评分项生成提供稳定的类型入口。
 *
 * 关键流程：
 * 1. 草稿生成服务读取 capture_event.event_type 后，通过 fromEventType 转换为动作类型。
 * 2. 自动生成草稿时使用 actionNamePrefix 组合页面元素文本，形成老师可确认的候选动作名称。
 * 3. 后续发布 task_step 和 evaluation_item 时使用 value，避免业务代码散落魔法字符串。
 */
public enum CaptureActionType {

    /**
     * 点击按钮、链接、菜单等可交互元素。
     */
    CLICK("CLICK", "点击", "点击", true),

    /**
     * 填写输入框或文本域。
     */
    INPUT("INPUT", "输入", "填写", true),

    /**
     * 选择下拉、单选、多选等选项。
     */
    SELECT("SELECT", "选择", "选择", true),

    /**
     * 上传文件或附件。
     */
    UPLOAD("UPLOAD", "上传", "上传", true),

    /**
     * 页面跳转动作，由 CLICK 和页面状态变化归并生成。
     */
    NAVIGATE("NAVIGATE", "进入页面", "进入", false),

    /**
     * 页面或业务状态确认动作。
     */
    VERIFY_STATE("VERIFY_STATE", "确认状态", "确认", false);

    private final String value;

    private final String label;

    private final String actionNamePrefix;

    private final boolean singleEventSupported;

    CaptureActionType(String value, String label, String actionNamePrefix, boolean singleEventSupported) {
        this.value = value;
        this.label = label;
        this.actionNamePrefix = actionNamePrefix;
        this.singleEventSupported = singleEventSupported;
    }

    /**
     * 获取持久化和接口传输使用的动作类型值。
     *
     * @return 动作类型值。
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取面向老师展示的动作类型名称。
     *
     * @return 动作类型中文名称。
     */
    public String getLabel() {
        return label;
    }

    /**
     * 获取自动生成动作草稿名称时使用的中文前缀。
     *
     * @return 动作名称前缀。
     */
    public String getActionNamePrefix() {
        return actionNamePrefix;
    }

    /**
     * 判断该类型是否允许在 MVP 阶段由单个采集事件直接生成。
     *
     * @return true 表示可由单事件直接生成草稿。
     */
    public boolean isSingleEventSupported() {
        return singleEventSupported;
    }

    /**
     * 判断外部传入值是否等于当前动作类型。
     *
     * @param value 待判断的动作类型值。
     * @return true 表示同一动作类型。
     */
    public boolean sameValue(String value) {
        return this.value.equals(value);
    }

    /**
     * 按动作类型值解析枚举。
     *
     * @param value 动作类型值。
     * @return 匹配的动作类型；未命中时返回 null，让调用方决定是否过滤事件。
     */
    public static CaptureActionType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        for (CaptureActionType actionType : values()) {
            if (actionType.value.equals(value)) {
                return actionType;
            }
        }
        return null;
    }

    /**
     * 按 SDK 采集事件类型解析可生成的动作草稿类型。
     *
     * @param eventType SDK 采集事件类型。
     * @return 匹配的动作草稿类型；无法直接生成时返回 null。
     */
    public static CaptureActionType fromEventType(String eventType) {
        CaptureActionType actionType = fromValue(eventType);
        if (actionType == null || !actionType.singleEventSupported) {
            return null;
        }
        return actionType;
    }

    /**
     * 根据目标文本生成老师可读的草稿动作名称。
     *
     * @param targetText SDK 采集到的页面元素文本。
     * @param fallbackName 目标文本缺失时使用的兜底名称。
     * @return 草稿动作名称。
     */
    public String buildActionName(String targetText, String fallbackName) {
        String text = StringUtils.hasText(targetText) ? targetText.trim() : fallbackName;
        if (!StringUtils.hasText(text)) {
            text = label;
        }
        return actionNamePrefix + text;
    }
}
