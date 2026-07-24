package com.sxpt.module.capture.vo;

import java.util.List;

/**
 * 页面动作草稿自动生成结果返回对象。
 *
 * 业务功能：
 * 1. 向教师端返回本次自动生成的草稿数量，便于页面展示明确反馈。
 * 2. 同步返回新生成的草稿列表，便于前端在生成后直接刷新待确认区域。
 *
 * 关键流程：
 * 1. Controller 调用草稿生成服务得到本次新增草稿。
 * 2. Controller 将实体转换为草稿 VO 后放入本结果对象，避免直接暴露持久化实体。
 */
public class CaptureActionDraftGenerateResultVO {

    private Integer generatedCount;

    private List<CaptureActionDraftVO> drafts;

    public Integer getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(Integer generatedCount) {
        this.generatedCount = generatedCount;
    }

    public List<CaptureActionDraftVO> getDrafts() {
        return drafts;
    }

    public void setDrafts(List<CaptureActionDraftVO> drafts) {
        this.drafts = drafts;
    }
}
