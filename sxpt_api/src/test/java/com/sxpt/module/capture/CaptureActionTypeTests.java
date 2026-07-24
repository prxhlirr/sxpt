package com.sxpt.module.capture;

import com.sxpt.module.capture.enums.CaptureActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 采集动作草稿类型枚举测试。
 *
 * 业务功能：
 * 1. 验证 SDK 采集事件类型可以被稳定转换为动作草稿类型。
 * 2. 验证动作名称生成规则稳定，避免后续草稿生成服务重复拼接文案。
 *
 * 关键流程：
 * 1. 使用纯单元测试验证枚举，不依赖 Spring、数据库和 Redis。
 * 2. 先覆盖 MVP 单事件生成所需能力，再为后续多事件归并保留扩展空间。
 */
class CaptureActionTypeTests {

    /**
     * 验证 MVP 支持的单事件类型可以转换为动作草稿类型。
     */
    @Test
    void fromEventTypeShouldReturnSingleEventActionType() {
        assertSame(CaptureActionType.CLICK, CaptureActionType.fromEventType("CLICK"));
        assertSame(CaptureActionType.INPUT, CaptureActionType.fromEventType("INPUT"));
        assertSame(CaptureActionType.SELECT, CaptureActionType.fromEventType("SELECT"));
        assertSame(CaptureActionType.UPLOAD, CaptureActionType.fromEventType("UPLOAD"));
    }

    /**
     * 验证非单事件类型不会被第一版自动草稿生成服务直接处理。
     */
    @Test
    void fromEventTypeShouldRejectNonSingleEventActionType() {
        assertNull(CaptureActionType.fromEventType("NAVIGATE"));
        assertNull(CaptureActionType.fromEventType("VERIFY_STATE"));
        assertNull(CaptureActionType.fromEventType("SCROLL"));
        assertNull(CaptureActionType.fromEventType(" "));
    }

    /**
     * 验证动作类型值判断使用统一枚举入口，避免业务代码散落字符串比较。
     */
    @Test
    void sameValueShouldCompareActionTypeValue() {
        assertTrue(CaptureActionType.CLICK.sameValue("CLICK"));
        assertFalse(CaptureActionType.CLICK.sameValue("INPUT"));
    }

    /**
     * 验证动作名称优先使用页面元素文本，帮助老师快速确认草稿含义。
     */
    @Test
    void buildActionNameShouldUseTargetText() {
        assertEquals("点击保存", CaptureActionType.CLICK.buildActionName("保存", "按钮1"));
        assertEquals("填写客户名称", CaptureActionType.INPUT.buildActionName("客户名称", "输入框1"));
        assertEquals("选择单据类型", CaptureActionType.SELECT.buildActionName("单据类型", "选择框1"));
        assertEquals("上传附件", CaptureActionType.UPLOAD.buildActionName("附件", "上传控件1"));
    }

    /**
     * 验证目标文本缺失时使用兜底名称，避免生成空动作草稿。
     */
    @Test
    void buildActionNameShouldUseFallbackNameWhenTargetTextMissing() {
        assertEquals("点击客户管理页面按钮3", CaptureActionType.CLICK.buildActionName(" ", "客户管理页面按钮3"));
    }
}
