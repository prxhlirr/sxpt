package com.sxpt.module.capture.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.capture.dto.ReportCaptureEventRequest;
import com.sxpt.module.capture.entity.CaptureEvent;
import com.sxpt.module.capture.service.CaptureEventService;
import com.sxpt.module.capture.vo.CaptureEventVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SDK 采集事件接口。
 *
 * 业务功能：
 * 1. 提供 SDK 单条事件上报入口，支持点击、输入、选择、上传和页面状态变化。
 * 2. 提供按采集会话查询事件入口，为教师查看采集过程和动作草稿生成提供事实列表。
 *
 * 关键流程：
 * 1. 接收上报请求并触发 Bean Validation，优先拦截缺少 clientEventId 的非幂等事件。
 * 2. 将 DTO 转换为 CaptureEvent 实体并生成应用层主键。
 * 3. 调用 Service 完成幂等写入，再转换为 VO 返回给 SDK。
 */
@RestController
@RequestMapping("/api/v1/capture/events")
@ConditionalOnProperty(name = "sxpt.capture.event-controller.enabled", havingValue = "true", matchIfMissing = true)
public class CaptureEventController {

    private final CaptureEventService captureEventService;

    public CaptureEventController(CaptureEventService captureEventService) {
        this.captureEventService = captureEventService;
    }

    /**
     * 上报单条 SDK 采集事件。
     *
     * @param request SDK 采集事件上报请求。
     * @return 已保存或幂等命中的 SDK 采集事件。
     */
    @PostMapping("/report")
    public ApiResult<CaptureEventVO> report(@Valid @RequestBody ReportCaptureEventRequest request) {
        CaptureEvent saved = captureEventService.reportCaptureEvent(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询指定采集会话下的 SDK 事件。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return SDK 采集事件列表。
     */
    @GetMapping
    public ApiResult<List<CaptureEventVO>> listBySession(@RequestParam String tenantId,
                                                         @RequestParam String captureSessionId) {
        return ApiResult.success(toVOList(captureEventService.listBySession(tenantId, captureSessionId)));
    }

    /**
     * 将上报请求转换为数据库实体。
     *
     * @param request SDK 采集事件上报请求。
     * @return SDK 采集事件实体。
     */
    private CaptureEvent toEntity(ReportCaptureEventRequest request) {
        CaptureEvent captureEvent = new CaptureEvent();
        captureEvent.setId(generateId());
        captureEvent.setTenantId(request.getTenantId());
        captureEvent.setCaptureSessionId(request.getCaptureSessionId());
        captureEvent.setSdkSessionId(request.getSdkSessionId());
        captureEvent.setClientEventId(request.getClientEventId());
        captureEvent.setEventType(request.getEventType());
        captureEvent.setEventTime(request.getEventTime());
        captureEvent.setSequenceNo(request.getSequenceNo());
        captureEvent.setRetryCount(request.getRetryCount());
        captureEvent.setPageUrl(request.getPageUrl());
        captureEvent.setTargetText(request.getTargetText());
        captureEvent.setTargetLocator(request.getTargetLocator());
        captureEvent.setTargetStableKey(request.getTargetStableKey());
        captureEvent.setInputValueMasked(request.getInputValueMasked());
        captureEvent.setEventPayloadJson(request.getEventPayloadJson());
        return captureEvent;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param events SDK 采集事件实体列表。
     * @return SDK 采集事件展示对象列表。
     */
    private List<CaptureEventVO> toVOList(List<CaptureEvent> events) {
        List<CaptureEventVO> result = new ArrayList<>();
        for (CaptureEvent event : events) {
            result.add(toVO(event));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param event SDK 采集事件实体。
     * @return SDK 采集事件展示对象。
     */
    private CaptureEventVO toVO(CaptureEvent event) {
        CaptureEventVO vo = new CaptureEventVO();
        vo.setId(event.getId());
        vo.setTenantId(event.getTenantId());
        vo.setCaptureSessionId(event.getCaptureSessionId());
        vo.setSdkSessionId(event.getSdkSessionId());
        vo.setClientEventId(event.getClientEventId());
        vo.setEventType(event.getEventType());
        vo.setEventTime(event.getEventTime());
        vo.setSequenceNo(event.getSequenceNo());
        vo.setRetryCount(event.getRetryCount());
        vo.setPageUrl(event.getPageUrl());
        vo.setTargetText(event.getTargetText());
        vo.setTargetLocator(event.getTargetLocator());
        vo.setTargetStableKey(event.getTargetStableKey());
        vo.setInputValueMasked(event.getInputValueMasked());
        vo.setArchiveStatus(event.getArchiveStatus());
        vo.setStatus(event.getStatus());
        vo.setCreateTime(event.getCreateTime());
        return vo;
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
