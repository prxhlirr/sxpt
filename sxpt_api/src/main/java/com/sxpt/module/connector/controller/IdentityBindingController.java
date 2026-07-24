package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.CreateIdentityBindingRequest;
import com.sxpt.module.connector.entity.IdentityBinding;
import com.sxpt.module.connector.service.IdentityBindingService;
import com.sxpt.module.connector.vo.IdentityBindingVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

/**
 * 原平台身份绑定接口。
 *
 * 业务功能：
 * 1. 提供教学用户与原平台账号绑定关系的创建入口。
 * 2. 提供 launchToken 生成前按教学用户和原平台配置定位绑定身份的查询入口。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，避免无效绑定进入业务层。
 * 2. 将 DTO 转换为 IdentityBinding 实体，并在应用层生成主键。
 * 3. 调用 Service 完成绑定写入或查询。
 * 4. 将实体转换为 VO，避免向前端暴露原平台角色和组织快照 JSON。
 */
@RestController
@RequestMapping("/api/v1/connector/identity-bindings")
@ConditionalOnProperty(name = "sxpt.connector.identity-binding-controller.enabled", havingValue = "true", matchIfMissing = true)
public class IdentityBindingController {

    private final IdentityBindingService identityBindingService;

    public IdentityBindingController(IdentityBindingService identityBindingService) {
        this.identityBindingService = identityBindingService;
    }

    /**
     * 创建教学用户与原平台账号的身份绑定。
     *
     * @param request 创建身份绑定请求。
     * @return 已创建的身份绑定展示对象。
     */
    @PostMapping("/create")
    public ApiResult<IdentityBindingVO> create(@Valid @RequestBody CreateIdentityBindingRequest request) {
        IdentityBinding saved = identityBindingService.createIdentityBinding(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询教学用户在指定原平台下的有效身份绑定。
     *
     * @param tenantId 租户 ID。
     * @param userId 教学用户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 有效身份绑定展示对象。
     */
    @GetMapping("/user")
    public ApiResult<IdentityBindingVO> getByUserAndConnector(@RequestParam String tenantId,
                                                              @RequestParam String userId,
                                                              @RequestParam String connectorSystemId) {
        IdentityBinding binding = identityBindingService.getBindingByUserAndConnector(tenantId, userId, connectorSystemId);
        return ApiResult.success(toVO(binding));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建身份绑定请求。
     * @return 身份绑定实体。
     */
    private IdentityBinding toEntity(CreateIdentityBindingRequest request) {
        IdentityBinding binding = new IdentityBinding();
        binding.setId(generateId());
        binding.setTenantId(request.getTenantId());
        binding.setUserId(request.getUserId());
        binding.setConnectorSystemId(request.getConnectorSystemId());
        binding.setExternalUserId(request.getExternalUserId());
        binding.setExternalUsername(request.getExternalUsername());
        binding.setExternalRoleJson(request.getExternalRoleJson());
        binding.setExternalOrgJson(request.getExternalOrgJson());
        binding.setBindingType(request.getBindingType());
        return binding;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param binding 身份绑定实体。
     * @return 身份绑定展示对象。
     */
    private IdentityBindingVO toVO(IdentityBinding binding) {
        IdentityBindingVO vo = new IdentityBindingVO();
        vo.setId(binding.getId());
        vo.setTenantId(binding.getTenantId());
        vo.setUserId(binding.getUserId());
        vo.setConnectorSystemId(binding.getConnectorSystemId());
        vo.setExternalUserId(binding.getExternalUserId());
        vo.setExternalUsername(binding.getExternalUsername());
        vo.setBindingType(binding.getBindingType());
        vo.setLastLoginTime(binding.getLastLoginTime());
        vo.setStatus(binding.getStatus());
        vo.setCreateTime(binding.getCreateTime());
        vo.setUpdateTime(binding.getUpdateTime());
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
