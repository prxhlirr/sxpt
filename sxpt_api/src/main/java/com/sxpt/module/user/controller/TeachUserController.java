package com.sxpt.module.user.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.user.dto.CreateTeachUserRequest;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.service.TeachUserService;
import com.sxpt.module.user.vo.TeachUserVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

/**
 * 教学平台用户接口。
 *
 * 业务功能：
 * 1. 提供教师、学生、管理员等教学平台用户的创建入口。
 * 2. 为后续角色授权、班级关系、任务发布和学习执行提供用户基础数据。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation。
 * 2. 将 DTO 转换为实体，补充应用层主键。
 * 3. 调用 Service 完成业务校验和持久化。
 * 4. 将实体转换为 VO，避免直接返回数据库 Entity。
 */
@RestController
@RequestMapping("/api/v1/user")
@ConditionalOnProperty(name = "sxpt.user.controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachUserController {

    private final TeachUserService teachUserService;

    public TeachUserController(TeachUserService teachUserService) {
        this.teachUserService = teachUserService;
    }

    /**
     * 创建教学平台用户。
     *
     * @param request 创建教学用户请求。
     * @return 已创建的教学平台用户。
     */
    @PostMapping("/create")
    public ApiResult<TeachUserVO> create(@Valid @RequestBody CreateTeachUserRequest request) {
        TeachUser saved = teachUserService.createTeachUser(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建教学用户请求。
     * @return 教学平台用户实体。
     */
    private TeachUser toEntity(CreateTeachUserRequest request) {
        TeachUser teachUser = new TeachUser();
        teachUser.setId(generateId());
        teachUser.setTenantId(request.getTenantId());
        teachUser.setUsername(request.getUsername());
        teachUser.setRealName(request.getRealName());
        teachUser.setPhone(request.getPhone());
        teachUser.setEmail(request.getEmail());
        teachUser.setUserType(request.getUserType());
        teachUser.setSourceType(request.getSourceType());
        teachUser.setExternalInfoJson(request.getExternalInfoJson());
        return teachUser;
    }

    /**
     * 将实体转换为前端返回对象。
     *
     * @param teachUser 教学平台用户实体。
     * @return 教学平台用户返回对象。
     */
    private TeachUserVO toVO(TeachUser teachUser) {
        TeachUserVO vo = new TeachUserVO();
        vo.setId(teachUser.getId());
        vo.setTenantId(teachUser.getTenantId());
        vo.setUsername(teachUser.getUsername());
        vo.setRealName(teachUser.getRealName());
        vo.setPhone(teachUser.getPhone());
        vo.setEmail(teachUser.getEmail());
        vo.setUserType(teachUser.getUserType());
        vo.setSourceType(teachUser.getSourceType());
        vo.setStatus(teachUser.getStatus());
        vo.setCreateTime(teachUser.getCreateTime());
        vo.setUpdateTime(teachUser.getUpdateTime());
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
