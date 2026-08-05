package com.sxpt.module.user.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.user.dto.AddTeachUserOrgRequest;
import com.sxpt.module.user.dto.RemoveTeachUserOrgRequest;
import com.sxpt.module.user.entity.TeachUserOrg;
import com.sxpt.module.user.service.TeachUserOrgService;
import com.sxpt.module.user.vo.TeachUserOrgVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 教学用户组织关系接口。
 *
 * 业务功能：
 * 1. 提供添加用户到班级、课程班或分组的入口。
 * 2. 为课程成员、任务发布范围和学生可见任务提供基础成员关系。
 *
 * 关键流程：
 * 1. 接收添加成员请求并触发 Bean Validation。
 * 2. 将 DTO 转换为实体，并在应用层生成主键。
 * 3. 调用 Service 完成关系写入和默认字段补齐。
 * 4. 将保存后的实体转换为 VO 返回。
 */
@RestController
@RequestMapping("/api/v1/orgs/users")
@ConditionalOnProperty(name = "sxpt.user-org.controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachUserOrgController {

    private final TeachUserOrgService teachUserOrgService;

    public TeachUserOrgController(TeachUserOrgService teachUserOrgService) {
        this.teachUserOrgService = teachUserOrgService;
    }

    /**
     * 添加用户到教学组织。
     *
     * @param request 添加用户到教学组织请求。
     * @return 已保存的用户组织关系。
     */
    @PostMapping("/add")
    public ApiResult<TeachUserOrgVO> add(@Valid @RequestBody AddTeachUserOrgRequest request) {
        TeachUserOrg saved = teachUserOrgService.addUserToOrg(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询用户教学组织关系列表。
     *
     * 业务功能：为管理端单位绑定页面提供当前成员关系，支撑班级、课程班和分组成员可视化维护。
     * 关键流程：按租户查询后转换为 VO，前端结合用户和单位列表展示可读名称。
     *
     * @param tenantId 租户 ID。
     * @return 用户教学组织关系列表。
     */
    @GetMapping
    public ApiResult<List<TeachUserOrgVO>> list(@RequestParam String tenantId) {
        List<TeachUserOrg> relations = teachUserOrgService.listUserOrgsByTenantId(tenantId);
        List<TeachUserOrgVO> result = new ArrayList<>();
        for (TeachUserOrg relation : relations) {
            result.add(toVO(relation));
        }
        return ApiResult.success(result);
    }

    /**
     * 从教学组织移除用户。
     *
     * @param request 从教学组织移除用户请求。
     * @return 已软删除的用户组织关系。
     */
    @PostMapping("/remove")
    public ApiResult<TeachUserOrgVO> remove(@Valid @RequestBody RemoveTeachUserOrgRequest request) {
        TeachUserOrg removed = teachUserOrgService.removeUserFromOrg(
                request.getTenantId(),
                request.getOrgId(),
                request.getUserId());
        return ApiResult.success(toVO(removed));
    }

    /**
     * 更新用户单位绑定关系启停用状态。
     *
     * @param request 用户单位状态切换请求。
     * @return 已更新状态的用户单位关系。
     */
    @PostMapping("/status")
    public ApiResult<TeachUserOrgVO> updateStatus(@Valid @RequestBody UpdateTeachUserOrgStatusRequest request) {
        TeachUserOrg saved = teachUserOrgService.updateUserOrgStatus(
                request.getTenantId(),
                request.getId(),
                request.getStatus());
        return ApiResult.success(toVO(saved));
    }

    /**
     * 将添加成员请求转换为用户组织关系实体。
     *
     * @param request 添加用户到教学组织请求。
     * @return 用户组织关系实体。
     */
    private TeachUserOrg toEntity(AddTeachUserOrgRequest request) {
        TeachUserOrg teachUserOrg = new TeachUserOrg();
        teachUserOrg.setId(generateId());
        teachUserOrg.setTenantId(request.getTenantId());
        teachUserOrg.setUserId(request.getUserId());
        teachUserOrg.setOrgId(request.getOrgId());
        teachUserOrg.setRelationType(request.getRelationType());
        return teachUserOrg;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param teachUserOrg 用户组织关系实体。
     * @return 用户组织关系返回对象。
     */
    private TeachUserOrgVO toVO(TeachUserOrg teachUserOrg) {
        TeachUserOrgVO vo = new TeachUserOrgVO();
        vo.setId(teachUserOrg.getId());
        vo.setTenantId(teachUserOrg.getTenantId());
        vo.setUserId(teachUserOrg.getUserId());
        vo.setOrgId(teachUserOrg.getOrgId());
        vo.setRelationType(teachUserOrg.getRelationType());
        vo.setStatus(teachUserOrg.getStatus());
        vo.setCreateTime(teachUserOrg.getCreateTime());
        vo.setUpdateTime(teachUserOrg.getUpdateTime());
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

    public static class UpdateTeachUserOrgStatusRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;

        @NotBlank(message = "用户单位关系 ID 不能为空")
        @Size(max = 64, message = "用户单位关系 ID 长度不能超过 64")
        private String id;

        @NotBlank(message = "状态不能为空")
        @Size(max = 32, message = "状态长度不能超过 32")
        private String status;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
