package com.sxpt.module.capture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.capture.entity.CaptureActionDraft;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.enums.CaptureActionType;
import com.sxpt.module.capture.mapper.CaptureActionDraftMapper;
import com.sxpt.module.capture.mapper.CaptureEventMapper;
import com.sxpt.module.capture.service.CaptureActionDraftGenerateService;
import com.sxpt.module.capture.service.CaptureActionDraftService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 采集事件自动生成动作草稿服务实现。
 *
 * 业务功能：
 * 1. 将 CLICK、INPUT、SELECT、UPLOAD 等单事件转换为老师可确认的动作草稿。
 * 2. 通过 event_id 幂等校验避免同一采集事件重复生成草稿。
 *
 * 关键流程：
 * 1. 校验租户、采集会话和操作人，确保生成动作有明确归属。
 * 2. 加载未删除事件并按采集顺序处理，只接收 MVP 阶段支持的单事件类型。
 * 3. 构造动作草稿后委托 CaptureActionDraftService 写入，复用既有默认值和状态规则。
 */
@Service
@Profile("!test")
public class CaptureActionDraftGenerateServiceImpl implements CaptureActionDraftGenerateService {

    private final CaptureEventMapper captureEventMapper;

    private final CaptureActionDraftMapper captureActionDraftMapper;

    private final CaptureActionDraftService captureActionDraftService;

    public CaptureActionDraftGenerateServiceImpl(CaptureEventMapper captureEventMapper,
                                                 CaptureActionDraftMapper captureActionDraftMapper,
                                                 CaptureActionDraftService captureActionDraftService) {
        this.captureEventMapper = captureEventMapper;
        this.captureActionDraftMapper = captureActionDraftMapper;
        this.captureActionDraftService = captureActionDraftService;
    }

    /**
     * 按采集会话自动生成动作草稿。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @param operatorId 触发生成的老师或系统操作人 ID。
     * @return 本次新生成的动作草稿列表。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CaptureActionDraft> generateDraftsFromSession(String tenantId, String captureSessionId,
                                                              String operatorId) {
        requireText(tenantId);
        requireText(captureSessionId);
        requireText(operatorId);
        List<CaptureEvent> events = captureEventMapper.selectList(new QueryWrapper<CaptureEvent>()
                .eq("tenant_id", tenantId)
                .eq("capture_session_id", captureSessionId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sequence_no", "event_time"));
        List<CaptureActionDraft> generatedDrafts = new ArrayList<>();
        int index = 0;
        while (index < events.size()) {
            CaptureEvent event = events.get(index);
            int nextIndex = nextEventIndexAfterGroup(events, index);
            List<CaptureEvent> eventGroup = events.subList(index, nextIndex);
            CaptureActionDraft draft = buildDraftForGroup(eventGroup, operatorId);
            if (draft == null || existsDraftForAnyEvent(eventGroup)) {
                index = nextIndex;
                continue;
            }
            generatedDrafts.add(captureActionDraftService.createActionDraft(draft));
            index = nextIndex;
        }
        return generatedDrafts;
    }

    /**
     * 计算当前事件归并后的下一处理位置。
     *
     * @param events 已按采集顺序排序的事件列表。
     * @param index 当前事件位置。
     * @return 下一个未处理事件位置。
     */
    private int nextEventIndexAfterGroup(List<CaptureEvent> events, int index) {
        if (canMergeClickState(events, index)) {
            return index + 2;
        }
        if (canMergeSelectClickQuery(events, index)) {
            return index + 2;
        }
        return nextEventIndexAfterInputGroup(events, index);
    }

    /**
     * 判断当前位置是否满足 CLICK 后紧跟页面 STATE 变化。
     *
     * @param events 已按采集顺序排序的事件列表。
     * @param index 当前事件位置。
     * @return 可以归并为导航动作时返回 true。
     */
    private boolean canMergeClickState(List<CaptureEvent> events, int index) {
        if (index + 1 >= events.size()) {
            return false;
        }
        CaptureEvent clickEvent = events.get(index);
        CaptureEvent stateEvent = events.get(index + 1);
        return clickEvent != null
                && stateEvent != null
                && "CLICK".equals(clickEvent.getEventType())
                && "STATE".equals(stateEvent.getEventType())
                && StringUtils.hasText(stateEvent.getPageUrl())
                && !sameText(clickEvent.getPageUrl(), stateEvent.getPageUrl());
    }

    /**
     * 判断当前位置是否满足 SELECT 后紧跟查询类 CLICK。
     *
     * @param events 已按采集顺序排序的事件列表。
     * @param index 当前事件位置。
     * @return 可以归并为业务查询动作时返回 true。
     */
    private boolean canMergeSelectClickQuery(List<CaptureEvent> events, int index) {
        if (index + 1 >= events.size()) {
            return false;
        }
        CaptureEvent selectEvent = events.get(index);
        CaptureEvent clickEvent = events.get(index + 1);
        return selectEvent != null
                && clickEvent != null
                && "SELECT".equals(selectEvent.getEventType())
                && "CLICK".equals(clickEvent.getEventType())
                && sameText(selectEvent.getPageUrl(), clickEvent.getPageUrl())
                && isQueryClick(clickEvent);
    }

    /**
     * 根据事件组构造动作草稿。
     *
     * @param eventGroup 当前候选事件组。
     * @param operatorId 操作人 ID。
     * @return 动作草稿；事件组不支持时返回 null。
     */
    private CaptureActionDraft buildDraftForGroup(List<CaptureEvent> eventGroup, String operatorId) {
        if (eventGroup == null || eventGroup.isEmpty()) {
            return null;
        }
        if (eventGroup.size() == 2 && canMergeClickState(eventGroup, 0)) {
            return buildNavigateDraft(eventGroup.get(0), eventGroup.get(1), operatorId);
        }
        if (eventGroup.size() == 2 && canMergeSelectClickQuery(eventGroup, 0)) {
            return buildQueryDraft(eventGroup.get(0), eventGroup.get(1), operatorId);
        }
        return buildDraftIfSupported(eventGroup.get(0), operatorId);
    }

    /**
     * 构造页面导航动作草稿。
     *
     * @param clickEvent 触发页面变化的点击事件。
     * @param stateEvent 点击后的页面状态事件。
     * @param operatorId 操作人 ID。
     * @return 页面导航动作草稿。
     */
    private CaptureActionDraft buildNavigateDraft(CaptureEvent clickEvent, CaptureEvent stateEvent, String operatorId) {
        CaptureActionDraft draft = new CaptureActionDraft();
        draft.setId(generateId());
        draft.setTenantId(clickEvent.getTenantId());
        draft.setCaptureSessionId(clickEvent.getCaptureSessionId());
        draft.setEventId(clickEvent.getId());
        draft.setActionType(CaptureActionType.NAVIGATE.getValue());
        draft.setActionName(CaptureActionType.NAVIGATE.buildActionName(
                stateEvent.getTargetText(), fallbackActionName(stateEvent)));
        draft.setSequenceNo(clickEvent.getSequenceNo());
        draft.setSuggestedOperationName(draft.getActionName());
        draft.setCreateBy(operatorId);
        draft.setUpdateBy(operatorId);
        return draft;
    }

    /**
     * 构造业务查询动作草稿。
     *
     * @param selectEvent 查询条件选择事件。
     * @param clickEvent 触发查询的点击事件。
     * @param operatorId 操作人 ID。
     * @return 业务查询动作草稿。
     */
    private CaptureActionDraft buildQueryDraft(CaptureEvent selectEvent, CaptureEvent clickEvent, String operatorId) {
        CaptureActionDraft draft = new CaptureActionDraft();
        draft.setId(generateId());
        draft.setTenantId(selectEvent.getTenantId());
        draft.setCaptureSessionId(selectEvent.getCaptureSessionId());
        draft.setEventId(clickEvent.getId());
        draft.setActionType(CaptureActionType.CLICK.getValue());
        draft.setActionName(buildQueryActionName(selectEvent, clickEvent));
        draft.setSequenceNo(selectEvent.getSequenceNo());
        draft.setSuggestedOperationName(draft.getActionName());
        draft.setCreateBy(operatorId);
        draft.setUpdateBy(operatorId);
        return draft;
    }

    /**
     * 计算连续 INPUT 事件归并后的下一处理位置。
     *
     * 业务逻辑：只有相邻、同页面、同控件的 INPUT 才属于同一个填写动作，
     * 这样既能消除逐字输入带来的草稿噪音，也不会跨按钮点击或字段切换误合并教师操作。
     *
     * @param events 已按采集顺序排序的事件列表。
     * @param index 当前事件位置。
     * @return 下一个未处理事件位置。
     */
    private int nextEventIndexAfterInputGroup(List<CaptureEvent> events, int index) {
        CaptureEvent current = events.get(index);
        if (!"INPUT".equals(current.getEventType())) {
            return index + 1;
        }
        int nextIndex = index + 1;
        while (nextIndex < events.size() && canMergeInput(current, events.get(nextIndex))) {
            nextIndex++;
        }
        return nextIndex;
    }

    /**
     * 判断两个 INPUT 事件是否可以归并为一个填写动作。
     *
     * @param first 当前归并组的首个 INPUT 事件。
     * @param next 候选后续事件。
     * @return 可以归并时返回 true。
     */
    private boolean canMergeInput(CaptureEvent first, CaptureEvent next) {
        if (next == null || !"INPUT".equals(next.getEventType())) {
            return false;
        }
        if (!sameText(first.getPageUrl(), next.getPageUrl())) {
            return false;
        }
        String firstControlKey = controlKey(first);
        String nextControlKey = controlKey(next);
        return StringUtils.hasText(firstControlKey) && firstControlKey.equals(nextControlKey);
    }

    /**
     * 判断点击事件是否属于查询触发按钮。
     *
     * @param event 点击事件。
     * @return 属于查询类点击时返回 true。
     */
    private boolean isQueryClick(CaptureEvent event) {
        String targetText = StringUtils.hasText(event.getTargetText()) ? event.getTargetText().trim().toLowerCase() : "";
        return targetText.contains("查询")
                || targetText.contains("搜索")
                || targetText.contains("检索")
                || targetText.contains("筛选")
                || targetText.contains("确定")
                || targetText.contains("search")
                || targetText.contains("query")
                || targetText.contains("filter");
    }

    /**
     * 根据单个采集事件构造动作草稿。
     *
     * @param event 采集事件。
     * @param operatorId 操作人 ID。
     * @return 可生成的动作草稿；事件不适合生成时返回 null。
     */
    private CaptureActionDraft buildDraftIfSupported(CaptureEvent event, String operatorId) {
        if (event == null || !StringUtils.hasText(event.getId()) || event.getSequenceNo() == null) {
            return null;
        }
        CaptureActionType actionType = CaptureActionType.fromEventType(event.getEventType());
        if (actionType == null) {
            return null;
        }
        CaptureActionDraft draft = new CaptureActionDraft();
        draft.setId(generateId());
        draft.setTenantId(event.getTenantId());
        draft.setCaptureSessionId(event.getCaptureSessionId());
        draft.setEventId(event.getId());
        draft.setActionType(actionType.getValue());
        draft.setActionName(actionType.buildActionName(event.getTargetText(), fallbackActionName(event)));
        draft.setSequenceNo(event.getSequenceNo());
        draft.setSuggestedOperationName(draft.getActionName());
        draft.setCreateBy(operatorId);
        draft.setUpdateBy(operatorId);
        return draft;
    }

    /**
     * 判断采集事件是否已经生成过动作草稿。
     *
     * @param event 采集事件。
     * @return true 表示已存在草稿。
     */
    private boolean existsDraftForEvent(CaptureEvent event) {
        CaptureActionDraft existed = captureActionDraftMapper.selectOne(new QueryWrapper<CaptureActionDraft>()
                .eq("tenant_id", event.getTenantId())
                .eq("capture_session_id", event.getCaptureSessionId())
                .eq("event_id", event.getId())
                .eq("deleted", Boolean.FALSE));
        return existed != null;
    }

    /**
     * 判断归并事件组内是否已有草稿。
     *
     * @param events 同一个候选动作下的采集事件。
     * @return 任一事件已生成草稿时返回 true。
     */
    private boolean existsDraftForAnyEvent(List<CaptureEvent> events) {
        for (CaptureEvent event : events) {
            if (existsDraftForEvent(event)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 提取控件稳定标识。
     *
     * @param event 采集事件。
     * @return 控件稳定标识。
     */
    private String controlKey(CaptureEvent event) {
        if (StringUtils.hasText(event.getTargetStableKey())) {
            return event.getTargetStableKey().trim();
        }
        if (StringUtils.hasText(event.getTargetLocator())) {
            return event.getTargetLocator().trim();
        }
        return null;
    }

    /**
     * 生成业务查询动作名称。
     *
     * @param selectEvent 查询条件选择事件。
     * @param clickEvent 查询触发点击事件。
     * @return 老师可确认的查询动作名称。
     */
    private String buildQueryActionName(CaptureEvent selectEvent, CaptureEvent clickEvent) {
        if (StringUtils.hasText(selectEvent.getTargetText())) {
            return "按" + selectEvent.getTargetText().trim() + "查询";
        }
        if (StringUtils.hasText(clickEvent.getTargetText())) {
            return "执行" + clickEvent.getTargetText().trim();
        }
        return "执行业务查询";
    }

    /**
     * 比较两个文本字段是否同值。
     *
     * @param left 左侧文本。
     * @param right 右侧文本。
     * @return 同值时返回 true。
     */
    private boolean sameText(String left, String right) {
        String normalizedLeft = StringUtils.hasText(left) ? left.trim() : "";
        String normalizedRight = StringUtils.hasText(right) ? right.trim() : "";
        return normalizedLeft.equals(normalizedRight);
    }

    /**
     * 生成目标文本缺失时使用的兜底动作名称。
     *
     * @param event 采集事件。
     * @return 兜底名称。
     */
    private String fallbackActionName(CaptureEvent event) {
        String pageUrl = StringUtils.hasText(event.getPageUrl()) ? event.getPageUrl().trim() : "页面";
        return pageUrl + "动作" + event.getSequenceNo();
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验文本。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 生成动作草稿主键。
     *
     * @return 32 位无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
