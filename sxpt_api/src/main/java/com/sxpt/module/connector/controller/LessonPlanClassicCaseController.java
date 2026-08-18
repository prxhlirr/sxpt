package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.dto.ClassicCaseConfigValidationRequest;
import com.sxpt.module.connector.service.ClassicCaseService;
import com.sxpt.module.connector.vo.LessonPlanClassicCaseOptionVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/** 教案创建与保存阶段的经典案例选择、锁版校验接口。 */
@RestController
@RequestMapping("/api/v1/lesson-plans")
public class LessonPlanClassicCaseController {

    private final ClassicCaseService classicCaseService;

    public LessonPlanClassicCaseController(ClassicCaseService classicCaseService) {
        this.classicCaseService = classicCaseService;
    }

    @GetMapping("/classic-case-options")
    public ApiResult<List<LessonPlanClassicCaseOptionVO>> options(
            @RequestParam String businessModuleCode,
            @RequestParam(required = false) String connectorSystemId,
            @RequestParam(required = false) String keyword) {
        CurrentUserContext.CurrentUser user = requireOperator();
        return ApiResult.success(classicCaseService.listClassicCaseOptions(
                user.getTenantId(), businessModuleCode, connectorSystemId, keyword));
    }

    @PostMapping("/classic-case-config/validate")
    public ApiResult<LessonPlanClassicCaseOptionVO> validate(
            @Valid @RequestBody ClassicCaseConfigValidationRequest request) {
        CurrentUserContext.CurrentUser user = requireOperator();
        return ApiResult.success(classicCaseService.validateClassicCaseConfig(user.getTenantId(), request));
    }

    private CurrentUserContext.CurrentUser requireOperator() {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        if (!user.hasAnyRole("ADMIN", "TEACHER", "EXPERT")) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return user;
    }
}
