package com.sxpt.module.teachingdata.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.service.DataInstanceAllocationService;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataPrepareJobService;
import com.sxpt.module.teachingdata.service.DataRequirementItemService;
import com.sxpt.module.teachingdata.service.DataRequirementService;
import com.sxpt.module.teachingdata.service.TeachingDataPoolService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 数据准备后台联调接口。
 *
 * 业务功能：
 * 1. 为前端数据准备管理页提供批次创建、批次查询、明细查询、任务查询和触发准备入口。
 * 2. 暂时暴露教学平台侧最小闭环，帮助通过页面交互发现批次、attempt、单位角色和状态流转问题。
 *
 * 关键流程：
 * 1. 老师或管理员先创建 DataRequirement 批次。
 * 2. 页面提交参与者约束并触发 DataPrepareFacadeService。
 * 3. 页面刷新批次、明细和任务列表，观察幂等、成功、失败和原平台返回字段。
 */
@RestController
@RequestMapping("/api/v1/teaching-data")
@ConditionalOnProperty(name = "sxpt.teaching-data.admin-controller.enabled", havingValue = "true", matchIfMissing = true)
public class DataPrepareAdminController {

    private final DataRequirementService dataRequirementService;

    private final DataRequirementItemService dataRequirementItemService;

    private final DataPrepareJobService dataPrepareJobService;

    private final DataPrepareFacadeService dataPrepareFacadeService;

    private final TeachingDataPoolService teachingDataPoolService;

    private final DataInstanceAllocationService dataInstanceAllocationService;

    public DataPrepareAdminController(DataRequirementService dataRequirementService,
                                      DataRequirementItemService dataRequirementItemService,
                                      DataPrepareJobService dataPrepareJobService,
                                      DataPrepareFacadeService dataPrepareFacadeService,
                                      TeachingDataPoolService teachingDataPoolService,
                                      DataInstanceAllocationService dataInstanceAllocationService) {
        this.dataRequirementService = dataRequirementService;
        this.dataRequirementItemService = dataRequirementItemService;
        this.dataPrepareJobService = dataPrepareJobService;
        this.dataPrepareFacadeService = dataPrepareFacadeService;
        this.teachingDataPoolService = teachingDataPoolService;
        this.dataInstanceAllocationService = dataInstanceAllocationService;
    }

    /**
     * 创建数据需求批次。
     *
     * @param requirement 数据需求批次。
     * @return 已创建的数据需求批次。
     */
    @PostMapping("/requirements/create")
    public ApiResult<DataRequirement> createRequirement(@RequestBody DataRequirement requirement) {
        if (!StringUtils.hasText(requirement.getId())) {
            requirement.setId(generateId());
        }
        if (!StringUtils.hasText(requirement.getRequirementCode())) {
            requirement.setRequirementCode("REQ-" + System.currentTimeMillis());
        }
        return ApiResult.success(dataRequirementService.createDataRequirement(requirement));
    }

    /**
     * 查询指定任务和场景的数据需求批次。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @return 数据需求批次列表。
     */
    @GetMapping("/requirements")
    public ApiResult<List<DataRequirement>> listRequirements(@RequestParam String tenantId,
                                                             @RequestParam String taskId,
                                                             @RequestParam String sceneType) {
        return ApiResult.success(dataRequirementService.listByTaskAndScene(tenantId, taskId, sceneType));
    }

    /**
     * 查询数据需求批次下的需求明细。
     *
     * @param requirementId 数据需求批次 ID。
     * @param tenantId 租户 ID。
     * @return 数据需求明细列表。
     */
    @GetMapping("/requirements/{requirementId}/items")
    public ApiResult<List<DataRequirementItem>> listRequirementItems(@PathVariable String requirementId,
                                                                     @RequestParam String tenantId) {
        return ApiResult.success(dataRequirementItemService.listByRequirement(tenantId, requirementId));
    }

    /**
     * 查询指定任务和场景的数据准备任务。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @return 数据准备任务列表。
     */
    @GetMapping("/jobs")
    public ApiResult<List<DataPrepareJob>> listJobs(@RequestParam String tenantId,
                                                    @RequestParam String taskId,
                                                    @RequestParam String sceneType) {
        return ApiResult.success(dataPrepareJobService.listByTaskAndScene(tenantId, taskId, sceneType));
    }

    /**
     * 触发数据准备。
     *
     * @param request 数据准备触发请求。
     * @return 执行后的数据准备任务。
     */
    @PostMapping("/prepare/execute")
    public ApiResult<DataPrepareJob> prepareAndExecute(
            @RequestBody DataPrepareFacadeService.PrepareAndExecuteRequest request) {
        return ApiResult.success(dataPrepareFacadeService.prepareAndExecute(request));
    }

    /**
     * 查询指定批次下的数据池。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 数据池列表。
     */
    @GetMapping("/pools")
    public ApiResult<List<TeachingDataPool>> listPools(@RequestParam String tenantId,
                                                       @RequestParam String requirementId) {
        return ApiResult.success(teachingDataPoolService.listByRequirement(tenantId, requirementId));
    }

    /**
     * 从数据池领取一条可用实例。
     *
     * @param request 数据领取请求。
     * @return 分配记录。
     */
    @PostMapping("/allocations/acquire")
    public ApiResult<DataInstanceAllocation> acquire(
            @RequestBody DataInstanceAllocationService.AcquireReadyInstanceRequest request) {
        return ApiResult.success(dataInstanceAllocationService.acquireReadyInstance(request));
    }

    /**
     * 生成应用层主键。
     *
     * @return 无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
