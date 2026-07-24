package com.sxpt.module.user.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.user.dto.CreateTeachOrgRequest;
import com.sxpt.module.user.entity.TeachOrg;
import com.sxpt.module.user.service.TeachOrgService;
import com.sxpt.module.user.vo.TeachOrgVO;
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
 * 教学组织接口。
 *
 * 业务功能：
 * 1. 提供班级、课程班和分组的创建入口。
 * 2. 提供租户下教学组织列表查询能力，为课程和任务发布选择班级提供基础数据。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation。
 * 2. 将 DTO 转换为实体，并在应用层生成主键。
 * 3. 调用 Service 完成业务校验、持久化或列表查询。
 * 4. 将实体转换为 VO，按统一响应结构返回。
 */
@RestController
@RequestMapping("/api/v1/orgs")
@ConditionalOnProperty(name = "sxpt.org.controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachOrgController {

    private final TeachOrgService teachOrgService;

    public TeachOrgController(TeachOrgService teachOrgService) {
        this.teachOrgService = teachOrgService;
    }

    /**
     * 创建教学组织。
     *
     * @param request 创建教学组织请求。
     * @return 已创建的教学组织。
     */
    @PostMapping("/create")
    public ApiResult<TeachOrgVO> create(@Valid @RequestBody CreateTeachOrgRequest request) {
        TeachOrg saved = teachOrgService.createTeachOrg(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询教学组织列表。
     *
     * @param tenantId 租户 ID。
     * @return 教学组织列表。
     */
    @GetMapping
    public ApiResult<List<TeachOrgVO>> list(@RequestParam String tenantId) {
        List<TeachOrg> teachOrgs = teachOrgService.listTeachOrgsByTenantId(tenantId);
        List<TeachOrgVO> result = new ArrayList<>();
        for (TeachOrg teachOrg : teachOrgs) {
            result.add(toVO(teachOrg));
        }
        return ApiResult.success(result);
    }

    /**
     * 将创建请求转换为教学组织实体。
     *
     * @param request 创建教学组织请求。
     * @return 教学组织实体。
     */
    private TeachOrg toEntity(CreateTeachOrgRequest request) {
        TeachOrg teachOrg = new TeachOrg();
        teachOrg.setId(generateId());
        teachOrg.setTenantId(request.getTenantId());
        teachOrg.setParentId(request.getParentId());
        teachOrg.setOrgCode(request.getOrgCode());
        teachOrg.setOrgName(request.getOrgName());
        teachOrg.setOrgType(request.getOrgType());
        return teachOrg;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param teachOrg 教学组织实体。
     * @return 教学组织返回对象。
     */
    private TeachOrgVO toVO(TeachOrg teachOrg) {
        TeachOrgVO vo = new TeachOrgVO();
        vo.setId(teachOrg.getId());
        vo.setTenantId(teachOrg.getTenantId());
        vo.setParentId(teachOrg.getParentId());
        vo.setOrgCode(teachOrg.getOrgCode());
        vo.setOrgName(teachOrg.getOrgName());
        vo.setOrgType(teachOrg.getOrgType());
        vo.setStatus(teachOrg.getStatus());
        vo.setCreateTime(teachOrg.getCreateTime());
        vo.setUpdateTime(teachOrg.getUpdateTime());
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
