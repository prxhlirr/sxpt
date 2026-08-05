package com.sxpt.module.user.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.PasswordHashService;
import com.sxpt.module.user.dto.CreateTeachUserRequest;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.service.TeachUserService;
import com.sxpt.module.user.vo.TeachUserVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
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

    private final PasswordHashService passwordHashService;

    public TeachUserController(TeachUserService teachUserService, PasswordHashService passwordHashService) {
        this.teachUserService = teachUserService;
        this.passwordHashService = passwordHashService;
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
     * 更新教学平台用户基础信息。
     *
     * @param request 更新教学用户请求。
     * @return 已更新的教学平台用户。
     */
    @PostMapping("/update")
    public ApiResult<TeachUserVO> update(@Valid @RequestBody UpdateTeachUserRequest request) {
        TeachUser saved = teachUserService.updateTeachUser(request.getTenantId(), toUpdateEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 更新教学平台用户启停用状态。
     *
     * @param request 用户状态切换请求。
     * @return 已更新状态的教学平台用户。
     */
    @PostMapping("/status")
    public ApiResult<TeachUserVO> updateStatus(@Valid @RequestBody UpdateTeachUserStatusRequest request) {
        TeachUser saved = teachUserService.updateTeachUserStatus(
                request.getTenantId(),
                request.getId(),
                request.getStatus());
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询教学平台用户列表。
     *
     * 业务功能：为管理端用户管理页面提供用户主数据列表，支撑后续角色授权、单位绑定和任务发布范围选择。
     * 关键流程：通过 Service 按租户和软删除边界查询，再转换为 VO，避免前端依赖数据库实体细节。
     *
     * @param tenantId 租户 ID。
     * @return 教学平台用户列表。
     */
    @GetMapping
    public ApiResult<List<TeachUserVO>> list(@RequestParam String tenantId) {
        List<TeachUser> teachUsers = teachUserService.listTeachUsersByTenantId(tenantId);
        List<TeachUserVO> result = new ArrayList<>();
        for (TeachUser teachUser : teachUsers) {
            result.add(toVO(teachUser));
        }
        return ApiResult.success(result);
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
        teachUser.setStudentNo(request.getStudentNo());
        teachUser.setEmployeeNo(request.getEmployeeNo());
        fillInitialPassword(request, teachUser);
        return teachUser;
    }

    /**
     * 将更新请求转换为用户实体。
     *
     * @param request 更新教学用户请求。
     * @return 教学平台用户实体。
     */
    private TeachUser toUpdateEntity(UpdateTeachUserRequest request) {
        TeachUser teachUser = new TeachUser();
        teachUser.setId(request.getId());
        teachUser.setRealName(request.getRealName());
        teachUser.setPhone(request.getPhone());
        teachUser.setEmail(request.getEmail());
        teachUser.setUserType(request.getUserType());
        teachUser.setSourceType(request.getSourceType());
        teachUser.setStudentNo(request.getStudentNo());
        teachUser.setEmployeeNo(request.getEmployeeNo());
        return teachUser;
    }

    /**
     * 填充初始密码哈希。
     *
     * @param request 创建教学用户请求。
     * @param teachUser 教学平台用户实体。
     */
    private void fillInitialPassword(CreateTeachUserRequest request, TeachUser teachUser) {
        if (!StringUtils.hasText(request.getInitialPassword())) {
            if ("LOCAL".equals(request.getSourceType())) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            return;
        }
        String salt = passwordHashService.generateSalt();
        teachUser.setPasswordSalt(salt);
        teachUser.setPasswordHash(passwordHashService.hash(request.getInitialPassword(), salt));
        teachUser.setPasswordAlgorithm(PasswordHashService.ALGORITHM);
        teachUser.setPasswordIterations(PasswordHashService.DEFAULT_ITERATIONS);
        teachUser.setPasswordStatus("NORMAL");
        teachUser.setPasswordUpdatedTime(LocalDateTime.now());
        teachUser.setFailedLoginCount(0);
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
        vo.setStudentNo(teachUser.getStudentNo());
        vo.setEmployeeNo(teachUser.getEmployeeNo());
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

    public static class UpdateTeachUserRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;

        @NotBlank(message = "用户 ID 不能为空")
        @Size(max = 64, message = "用户 ID 长度不能超过 64")
        private String id;

        @NotBlank(message = "用户姓名不能为空")
        @Size(max = 128, message = "用户姓名长度不能超过 128")
        private String realName;

        @Size(max = 32, message = "手机号长度不能超过 32")
        private String phone;

        @Size(max = 128, message = "邮箱长度不能超过 128")
        private String email;

        @NotBlank(message = "用户类型不能为空")
        @Size(max = 32, message = "用户类型长度不能超过 32")
        private String userType;

        @NotBlank(message = "来源类型不能为空")
        @Size(max = 32, message = "来源类型长度不能超过 32")
        private String sourceType;

        @Size(max = 64, message = "学号长度不能超过 64")
        private String studentNo;

        @Size(max = 64, message = "工号长度不能超过 64")
        private String employeeNo;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String sourceType) { this.sourceType = sourceType; }
        public String getStudentNo() { return studentNo; }
        public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
        public String getEmployeeNo() { return employeeNo; }
        public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    }

    public static class UpdateTeachUserStatusRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;

        @NotBlank(message = "用户 ID 不能为空")
        @Size(max = 64, message = "用户 ID 长度不能超过 64")
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
