package com.sxpt.module.capture.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.capture.dto.ReportCaptureResourceSnapshotRequest;
import com.sxpt.module.capture.entity.CaptureResourceSnapshot;
import com.sxpt.module.capture.service.CaptureResourceSnapshotService;
import com.sxpt.module.capture.vo.CaptureResourceSnapshotVO;
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
 * 采集资源快照接口。
 *
 * 业务功能：
 * 1. 提供 SDK 上报页面摘要和目标元素摘要的入口。
 * 2. 提供按采集会话查询资源快照的入口，为动作草稿生成和教师复核提供证据。
 *
 * 关键流程：
 * 1. 接收上报请求并触发 Bean Validation，前置拦截缺少摘要 JSON 的无效快照。
 * 2. 将 DTO 转换为 CaptureResourceSnapshot 实体并生成应用层主键。
 * 3. 调用 Service 完成 DEBUG_FULL_DOM 边界校验和持久化。
 */
@RestController
@RequestMapping("/api/v1/capture/resource-snapshots")
@ConditionalOnProperty(name = "sxpt.capture.resource-snapshot-controller.enabled", havingValue = "true", matchIfMissing = true)
public class CaptureResourceSnapshotController {

    private final CaptureResourceSnapshotService snapshotService;

    public CaptureResourceSnapshotController(CaptureResourceSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * 上报单条采集资源快照。
     *
     * @param request 采集资源快照上报请求。
     * @return 已保存的采集资源快照。
     */
    @PostMapping("/report")
    public ApiResult<CaptureResourceSnapshotVO> report(@Valid @RequestBody ReportCaptureResourceSnapshotRequest request) {
        CaptureResourceSnapshot saved = snapshotService.reportSnapshot(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询指定采集会话下的资源快照。
     *
     * @param tenantId 租户 ID。
     * @param captureSessionId 采集会话 ID。
     * @return 资源快照列表。
     */
    @GetMapping
    public ApiResult<List<CaptureResourceSnapshotVO>> listBySession(@RequestParam String tenantId,
                                                                    @RequestParam String captureSessionId) {
        return ApiResult.success(toVOList(snapshotService.listBySession(tenantId, captureSessionId)));
    }

    /**
     * 将上报请求转换为数据库实体。
     *
     * @param request 采集资源快照上报请求。
     * @return 采集资源快照实体。
     */
    private CaptureResourceSnapshot toEntity(ReportCaptureResourceSnapshotRequest request) {
        CaptureResourceSnapshot snapshot = new CaptureResourceSnapshot();
        snapshot.setId(generateId());
        snapshot.setTenantId(request.getTenantId());
        snapshot.setCaptureSessionId(request.getCaptureSessionId());
        snapshot.setPageUrl(request.getPageUrl());
        snapshot.setPageTitle(request.getPageTitle());
        snapshot.setResourceType(request.getResourceType());
        snapshot.setSnapshotScope(request.getSnapshotScope());
        snapshot.setResourceName(request.getResourceName());
        snapshot.setResourceLocator(request.getResourceLocator());
        snapshot.setElementSnapshotJson(request.getElementSnapshotJson());
        snapshot.setMetadataJson(request.getMetadataJson());
        snapshot.setSnapshotHash(request.getSnapshotHash());
        snapshot.setExpireTime(request.getExpireTime());
        return snapshot;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param snapshots 采集资源快照实体列表。
     * @return 采集资源快照展示对象列表。
     */
    private List<CaptureResourceSnapshotVO> toVOList(List<CaptureResourceSnapshot> snapshots) {
        List<CaptureResourceSnapshotVO> result = new ArrayList<>();
        for (CaptureResourceSnapshot snapshot : snapshots) {
            result.add(toVO(snapshot));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param snapshot 采集资源快照实体。
     * @return 采集资源快照展示对象。
     */
    private CaptureResourceSnapshotVO toVO(CaptureResourceSnapshot snapshot) {
        CaptureResourceSnapshotVO vo = new CaptureResourceSnapshotVO();
        vo.setId(snapshot.getId());
        vo.setTenantId(snapshot.getTenantId());
        vo.setCaptureSessionId(snapshot.getCaptureSessionId());
        vo.setPageUrl(snapshot.getPageUrl());
        vo.setPageTitle(snapshot.getPageTitle());
        vo.setResourceType(snapshot.getResourceType());
        vo.setSnapshotScope(snapshot.getSnapshotScope());
        vo.setResourceName(snapshot.getResourceName());
        vo.setResourceLocator(snapshot.getResourceLocator());
        vo.setElementSnapshotJson(snapshot.getElementSnapshotJson());
        vo.setMetadataJson(snapshot.getMetadataJson());
        vo.setSnapshotHash(snapshot.getSnapshotHash());
        vo.setArchiveStatus(snapshot.getArchiveStatus());
        vo.setExpireTime(snapshot.getExpireTime());
        vo.setStatus(snapshot.getStatus());
        vo.setCreateTime(snapshot.getCreateTime());
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
