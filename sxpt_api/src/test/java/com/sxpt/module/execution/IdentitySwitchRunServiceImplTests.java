package com.sxpt.module.execution;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.execution.dto.IdentitySwitchRunRequest;
import com.sxpt.module.execution.service.IdentitySwitchRunService;
import com.sxpt.module.execution.service.impl.IdentitySwitchRunServiceImpl;
import com.sxpt.module.execution.vo.IdentitySwitchRunVO;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 身份切换运行服务测试。
 *
 * 业务功能：
 * 1. 验证学生按 task_step 切换身份时会创建下一步骤 launchToken。
 * 2. 验证无效 SDK 模式、缺失步骤和缺失原平台身份要求会被拒绝。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 TaskStepService 和 PlatformLaunchContextService。
 * 2. 捕获 PlatformLaunchContext，确认下一步骤单位角色来自 task_step。
 */
class IdentitySwitchRunServiceImplTests {

    private final TaskStepService taskStepService = mock(TaskStepService.class);

    private final PlatformLaunchContextService platformLaunchContextService =
            mock(PlatformLaunchContextService.class);

    private final IdentitySwitchRunService service =
            new IdentitySwitchRunServiceImpl(taskStepService, platformLaunchContextService);

    /**
     * 校验身份切换成功创建下一步骤 launchToken。
     */
    @Test
    void runSwitchShouldCreateNextStepLaunchContext() {
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Arrays.asList(buildCurrentStep(), buildNextStep()));
        when(platformLaunchContextService.createLaunchContext(any(PlatformLaunchContext.class)))
                .thenReturn(new PlatformLaunchContextService.CreatedLaunchContext(
                        buildSavedLaunchContext(), "ctx_token_002"));

        IdentitySwitchRunVO result = service.runSwitch(buildValidRequest());

        assertEquals("launch_002", result.getLaunchContextId());
        assertEquals("ctx_token_002", result.getLaunchToken());
        assertEquals("step_002", result.getNextStepId());
        assertEquals(Long.valueOf(2), result.getNextSegmentNo());
        assertEquals("role_ext_002", result.getRequiredExternalRoleId());
        assertTrue(result.getSwitchConfirmRequired());

        ArgumentCaptor<PlatformLaunchContext> captor = ArgumentCaptor.forClass(PlatformLaunchContext.class);
        verify(platformLaunchContextService).createLaunchContext(captor.capture());
        PlatformLaunchContext launchContext = captor.getValue();
        assertEquals("student_001", launchContext.getUserId());
        assertEquals("PRACTICE", launchContext.getSdkMode());
        assertEquals("PRACTICE", launchContext.getSceneType());
        assertEquals("org_ext_002", launchContext.getRequiredExternalOrgId());
        assertEquals("role_ext_002", launchContext.getRequiredExternalRoleId());
        assertEquals(Long.valueOf(2), launchContext.getSegmentNo());
    }

    /**
     * 校验未知 SDK 模式被拒绝。
     */
    @Test
    void runSwitchShouldRejectUnknownSdkMode() {
        IdentitySwitchRunRequest request = buildValidRequest();
        request.setSdkMode("CAPTURE");

        assertThrows(BusinessException.class, () -> service.runSwitch(request));
    }

    /**
     * 校验下一步骤不存在时被拒绝。
     */
    @Test
    void runSwitchShouldRejectMissingNextStep() {
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Arrays.asList(buildCurrentStep()));

        assertThrows(BusinessException.class, () -> service.runSwitch(buildValidRequest()));
    }

    /**
     * 校验下一步骤缺少原平台角色时被拒绝。
     */
    @Test
    void runSwitchShouldRejectStepWithoutExternalRole() {
        TaskStep nextStep = buildNextStep();
        nextStep.setRequiredExternalRoleId(" ");
        when(taskStepService.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001"))
                .thenReturn(Arrays.asList(buildCurrentStep(), nextStep));

        assertThrows(BusinessException.class, () -> service.runSwitch(buildValidRequest()));
    }

    /**
     * 构造身份切换运行请求。
     *
     * @return 身份切换运行请求。
     */
    private IdentitySwitchRunRequest buildValidRequest() {
        IdentitySwitchRunRequest request = new IdentitySwitchRunRequest();
        request.setTenantId("tenant_001");
        request.setStudentId("student_001");
        request.setConnectorSystemId("connector_001");
        request.setTaskId("task_001");
        request.setTeachingPointId("tp_001");
        request.setExecutionId("execution_001");
        request.setCurrentStepId("step_001");
        request.setNextStepId("step_002");
        request.setDataInstanceId("data_001");
        request.setSdkMode("PRACTICE");
        request.setTargetUrl("https://origin.example.com/practice/audit");
        request.setExternalBusinessId("biz_001");
        request.setExternalBusinessNo("NO_001");
        return request;
    }

    /**
     * 构造当前步骤。
     *
     * @return 当前任务步骤。
     */
    private TaskStep buildCurrentStep() {
        TaskStep taskStep = new TaskStep();
        taskStep.setId("step_001");
        taskStep.setTenantId("tenant_001");
        taskStep.setTaskId("task_001");
        taskStep.setTeachingPointId("tp_001");
        taskStep.setSegmentNo(1L);
        return taskStep;
    }

    /**
     * 构造下一步骤。
     *
     * @return 下一任务步骤。
     */
    private TaskStep buildNextStep() {
        TaskStep taskStep = new TaskStep();
        taskStep.setId("step_002");
        taskStep.setTenantId("tenant_001");
        taskStep.setTaskId("task_001");
        taskStep.setTeachingPointId("tp_001");
        taskStep.setSegmentNo(2L);
        taskStep.setActorType("AUDITOR");
        taskStep.setRequiredExternalOrgId("org_ext_002");
        taskStep.setRequiredExternalOrgName("审核单位");
        taskStep.setRequiredExternalRoleId("role_ext_002");
        taskStep.setRequiredExternalRoleName("审核人");
        taskStep.setSwitchConfirmRequired(Boolean.TRUE);
        taskStep.setSwitchDecisionSource("TASK_STEP");
        taskStep.setSwitchStrategy("LAUNCH_TOKEN");
        return taskStep;
    }

    /**
     * 构造 launchToken 服务返回的已保存启动上下文。
     *
     * @return 已保存启动上下文。
     */
    private PlatformLaunchContext buildSavedLaunchContext() {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId("launch_002");
        launchContext.setTenantId("tenant_001");
        launchContext.setUserId("student_001");
        launchContext.setConnectorSystemId("connector_001");
        launchContext.setTaskId("task_001");
        launchContext.setTeachingPointId("tp_001");
        launchContext.setExecutionId("execution_001");
        launchContext.setSceneType("PRACTICE");
        launchContext.setSdkMode("PRACTICE");
        launchContext.setTargetUrl("https://origin.example.com/practice/audit");
        launchContext.setSegmentNo(2L);
        launchContext.setActorType("AUDITOR");
        launchContext.setRequiredExternalOrgId("org_ext_002");
        launchContext.setRequiredExternalOrgName("审核单位");
        launchContext.setRequiredExternalRoleId("role_ext_002");
        launchContext.setRequiredExternalRoleName("审核人");
        launchContext.setExpireTime(LocalDateTime.now().plusMinutes(5));
        return launchContext;
    }
}
