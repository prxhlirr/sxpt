package com.sxpt.module.capture.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.capture.dto.CreateCaptureSessionRequest;
import com.sxpt.module.capture.entity.CaptureSession;
import com.sxpt.module.capture.service.CaptureSessionService;
import com.sxpt.module.capture.vo.CaptureSessionVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 备案采集会话接口。
 *
 * 业务功能：
 * 1. 提供教师开始备案采集的创建入口。
 * 2. 提供教师查看会话列表和结束采集的接口。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation。
 * 2. 将 DTO 转换为 CaptureSession 实体，并生成应用层主键。
 * 3. 调用 Service 完成会话写入、查询或结束。
 * 4. 将实体转换为 VO，避免前端依赖数据库 Entity。
 */
@RestController
@RequestMapping("/api/v1/capture/sessions")
@ConditionalOnProperty(name = "sxpt.capture.session-controller.enabled", havingValue = "true", matchIfMissing = true)
public class CaptureSessionController {

    private final CaptureSessionService captureSessionService;

    public CaptureSessionController(CaptureSessionService captureSessionService) {
        this.captureSessionService = captureSessionService;
    }

    /**
     * 创建备案采集会话。
     *
     * @param request 创建备案采集会话请求。
     * @return 已创建的备案采集会话。
     */
    @PostMapping("/create")
    public ApiResult<CaptureSessionVO> create(@Valid @RequestBody CreateCaptureSessionRequest request) {
        CaptureSession saved = captureSessionService.createCaptureSession(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询教师的备案采集会话。
     *
     * @param tenantId 租户 ID。
     * @param teacherId 教师用户 ID。
     * @return 备案采集会话列表。
     */
    @GetMapping
    public ApiResult<List<CaptureSessionVO>> listByTeacher(@RequestParam String tenantId,
                                                           @RequestParam String teacherId) {
        return ApiResult.success(toVOList(captureSessionService.listByTeacher(tenantId, teacherId)));
    }

    /**
     * 结束备案采集会话。
     *
     * @param id 备案采集会话 ID。
     * @return 已结束的备案采集会话。
     */
    @PostMapping("/{id}/finish")
    public ApiResult<CaptureSessionVO> finish(@PathVariable String id) {
        return ApiResult.success(toVO(captureSessionService.finishCaptureSession(id)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建备案采集会话请求。
     * @return 备案采集会话实体。
     */
    private CaptureSession toEntity(CreateCaptureSessionRequest request) {
        CaptureSession captureSession = new CaptureSession();
        captureSession.setId(generateId());
        captureSession.setTenantId(request.getTenantId());
        captureSession.setConnectorSystemId(request.getConnectorSystemId());
        captureSession.setTeacherId(request.getTeacherId());
        captureSession.setSessionName(request.getSessionName());
        captureSession.setBusinessName(request.getBusinessName());
        captureSession.setCaptureMode(request.getCaptureMode());
        captureSession.setStartUrl(request.getStartUrl());
        captureSession.setCreateBy(request.getTeacherId());
        return captureSession;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param sessions 备案采集会话实体列表。
     * @return 备案采集会话展示对象列表。
     */
    private List<CaptureSessionVO> toVOList(List<CaptureSession> sessions) {
        List<CaptureSessionVO> result = new ArrayList<>();
        for (CaptureSession session : sessions) {
            result.add(toVO(session));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param session 备案采集会话实体。
     * @return 备案采集会话展示对象。
     */
    private CaptureSessionVO toVO(CaptureSession session) {
        CaptureSessionVO vo = new CaptureSessionVO();
        vo.setId(session.getId());
        vo.setTenantId(session.getTenantId());
        vo.setConnectorSystemId(session.getConnectorSystemId());
        vo.setTeacherId(session.getTeacherId());
        vo.setSessionName(session.getSessionName());
        vo.setBusinessName(session.getBusinessName());
        vo.setCaptureMode(session.getCaptureMode());
        vo.setStartUrl(session.getStartUrl());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());
        vo.setSessionStatus(session.getSessionStatus());
        vo.setStatus(session.getStatus());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
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
