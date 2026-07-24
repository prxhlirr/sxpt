package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.execution.dto.IdentitySwitchRunRequest;
import com.sxpt.module.execution.service.IdentitySwitchRunService;
import com.sxpt.module.execution.vo.IdentitySwitchRunVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 身份切换运行接口。
 *
 * 业务功能：
 * 1. 为 SDK 提供按任务步骤切换原平台单位和角色的运行态入口。
 * 2. 返回下一步骤 launchToken，使原平台继续使用既有 verify 和 used 回写闭环。
 *
 * 关键流程：
 * 1. SDK 提交任务、教学点、执行 ID、下一步骤和目标地址。
 * 2. Service 校验 task_step 中的身份要求并生成 launchToken。
 * 3. SDK 使用 launchToken 跳转原平台完成身份切换。
 */
@RestController
@RequestMapping("/api/v1/sdk/identity-switches")
@Validated
@ConditionalOnProperty(name = "sxpt.execution.identity-switch-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class IdentitySwitchRunController {

    private final IdentitySwitchRunService identitySwitchRunService;

    public IdentitySwitchRunController(IdentitySwitchRunService identitySwitchRunService) {
        this.identitySwitchRunService = identitySwitchRunService;
    }

    /**
     * 创建下一步骤身份切换运行上下文。
     *
     * @param request 身份切换运行请求。
     * @return 身份切换运行结果。
     */
    @PostMapping("/run")
    public ApiResult<IdentitySwitchRunVO> run(@Valid @RequestBody IdentitySwitchRunRequest request) {
        return ApiResult.success(identitySwitchRunService.runSwitch(request));
    }
}
