package com.sxpt.module.teachingdata.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.teachingdata.dto.CreateAuthoringLaunchRequest;
import com.sxpt.module.teachingdata.service.AuthoringLaunchService;
import com.sxpt.module.teachingdata.vo.AuthoringLaunchVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/** HTTP boundary for trusted teacher authoring data launches. */
@RestController
@RequestMapping("/api/v1/teaching-data/authoring-launches")
@ConditionalOnProperty(name = "sxpt.teaching-data.admin-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class AuthoringLaunchController {

    private final AuthoringLaunchService authoringLaunchService;

    public AuthoringLaunchController(AuthoringLaunchService authoringLaunchService) {
        this.authoringLaunchService = authoringLaunchService;
    }

    @PostMapping("/create")
    public ApiResult<AuthoringLaunchVO> create(@Valid @RequestBody CreateAuthoringLaunchRequest request) {
        return ApiResult.success(authoringLaunchService.createLaunch(request));
    }
}
