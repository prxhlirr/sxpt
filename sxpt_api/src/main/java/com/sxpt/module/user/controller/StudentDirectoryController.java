package com.sxpt.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.user.entity.TeachOrg;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.entity.TeachUserOrg;
import com.sxpt.module.user.mapper.TeachOrgMapper;
import com.sxpt.module.user.mapper.TeachUserMapper;
import com.sxpt.module.user.mapper.TeachUserOrgMapper;
import com.sxpt.module.user.vo.StudentDirectoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@Profile("!test")
public class StudentDirectoryController {

    private final TeachUserMapper userMapper;
    private final TeachUserOrgMapper userOrgMapper;
    private final TeachOrgMapper orgMapper;

    public StudentDirectoryController(
            TeachUserMapper userMapper,
            TeachUserOrgMapper userOrgMapper,
            TeachOrgMapper orgMapper
    ) {
        this.userMapper = userMapper;
        this.userOrgMapper = userOrgMapper;
        this.orgMapper = orgMapper;
    }

    @GetMapping("/students")
    public ApiResult<List<StudentDirectoryVO>> listStudents() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        if (!StringUtils.hasText(currentUser.getTenantId())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        if (!currentUser.hasAnyRole("ADMIN", "TEACHER")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        String tenantId = currentUser.getTenantId();
        List<TeachUser> students = userMapper.selectList(new LambdaQueryWrapper<TeachUser>()
                .eq(TeachUser::getTenantId, tenantId)
                .eq(TeachUser::getUserType, "STUDENT")
                .eq(TeachUser::getStatus, "ACTIVE")
                .eq(TeachUser::getDeleted, Boolean.FALSE));
        List<TeachUserOrg> relations = userOrgMapper.selectList(new LambdaQueryWrapper<TeachUserOrg>()
                .eq(TeachUserOrg::getTenantId, tenantId)
                .eq(TeachUserOrg::getStatus, "ACTIVE")
                .eq(TeachUserOrg::getDeleted, Boolean.FALSE));
        List<TeachOrg> orgs = orgMapper.selectList(new LambdaQueryWrapper<TeachOrg>()
                .eq(TeachOrg::getTenantId, tenantId)
                .eq(TeachOrg::getStatus, "ACTIVE")
                .eq(TeachOrg::getDeleted, Boolean.FALSE));
        Map<String, TeachUserOrg> relationByUser = new LinkedHashMap<String, TeachUserOrg>();
        for (TeachUserOrg relation : relations) {
            relationByUser.putIfAbsent(relation.getUserId(), relation);
        }
        Map<String, TeachOrg> orgById = new LinkedHashMap<String, TeachOrg>();
        for (TeachOrg org : orgs) {
            orgById.put(org.getId(), org);
        }
        List<StudentDirectoryVO> result = new ArrayList<StudentDirectoryVO>();
        for (TeachUser student : students) {
            TeachUserOrg relation = relationByUser.get(student.getId());
            TeachOrg org = relation == null ? null : orgById.get(relation.getOrgId());
            StudentDirectoryVO item = new StudentDirectoryVO();
            item.setStudentId(student.getId());
            item.setStudentName(student.getRealName());
            item.setUsername(student.getUsername());
            item.setStudentNo(student.getStudentNo());
            item.setUnitId(org == null ? "" : org.getId());
            item.setUnitName(org == null ? "未分班" : org.getOrgName());
            result.add(item);
        }
        return ApiResult.success(result);
    }
}
