package com.sxpt.module.teachingdata;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.connector.service.BusinessModuleService;
import com.sxpt.module.connector.service.ConnectorSystemService;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.connector.support.BusinessSsoLaunchUrlBuilder;
import com.sxpt.module.teachingdata.dto.CreateAuthoringLaunchRequest;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.service.DataPrepareFacadeService;
import com.sxpt.module.teachingdata.service.DataRequirementService;
import com.sxpt.module.teachingdata.service.impl.AuthoringLaunchServiceImpl;
import com.sxpt.module.teachingdata.vo.AuthoringLaunchVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthoringLaunchServiceImplTests {

    private ConnectorSystemService connectorSystemService;
    private BusinessModuleService businessModuleService;
    private ModuleDataStrategyService moduleDataStrategyService;
    private TeachingDataTemplateService teachingDataTemplateService;
    private BusinessModuleProcessChainService processChainService;
    private DataRequirementService dataRequirementService;
    private DataPrepareFacadeService dataPrepareFacadeService;
    private TeachingDataInstanceMapper teachingDataInstanceMapper;
    private PlatformLaunchContextService platformLaunchContextService;
    private BusinessSsoLaunchUrlBuilder urlBuilder;
    private AuthoringLaunchServiceImpl service;

    @BeforeEach
    void setUp() {
        connectorSystemService = mock(ConnectorSystemService.class);
        businessModuleService = mock(BusinessModuleService.class);
        moduleDataStrategyService = mock(ModuleDataStrategyService.class);
        teachingDataTemplateService = mock(TeachingDataTemplateService.class);
        processChainService = mock(BusinessModuleProcessChainService.class);
        dataRequirementService = mock(DataRequirementService.class);
        dataPrepareFacadeService = mock(DataPrepareFacadeService.class);
        teachingDataInstanceMapper = mock(TeachingDataInstanceMapper.class);
        platformLaunchContextService = mock(PlatformLaunchContextService.class);
        urlBuilder = mock(BusinessSsoLaunchUrlBuilder.class);
        service = new AuthoringLaunchServiceImpl(
                connectorSystemService,
                businessModuleService,
                moduleDataStrategyService,
                teachingDataTemplateService,
                processChainService,
                dataRequirementService,
                dataPrepareFacadeService,
                teachingDataInstanceMapper,
                platformLaunchContextService,
                urlBuilder);
        CurrentUserContext.set(currentTeacher());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void shouldCreateFreshRecordInstanceAndBindCaptureLaunchForEveryRequest() {
        stubRecordConfiguration();
        when(dataRequirementService.createDataRequirement(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dataPrepareFacadeService.prepareAndExecute(any()))
                .thenReturn(successJob("job-1", "batch-1"), successJob("job-2", "batch-2"));
        when(teachingDataInstanceMapper.selectOne(any()))
                .thenReturn(readyInstance("instance-1"), readyInstance("instance-2"));
        when(platformLaunchContextService.createLaunchContext(any()))
                .thenReturn(createdContext("launch-1", "ctx-one"), createdContext("launch-2", "ctx-two"));
        when(urlBuilder.build(any(), any(), any(), any()))
                .thenReturn("sso-launch-one", "sso-launch-two");

        AuthoringLaunchVO first = service.createLaunch(request());
        AuthoringLaunchVO second = service.createLaunch(request());

        ArgumentCaptor<DataRequirement> requirementCaptor = ArgumentCaptor.forClass(DataRequirement.class);
        verify(dataRequirementService, org.mockito.Mockito.times(2))
                .createDataRequirement(requirementCaptor.capture());
        assertEquals("RECORD", requirementCaptor.getAllValues().get(0).getSceneType());
        assertEquals("lesson-1", requirementCaptor.getAllValues().get(0).getTaskId());
        assertNotEquals(requirementCaptor.getAllValues().get(0).getRequirementCode(),
                requirementCaptor.getAllValues().get(1).getRequirementCode());

        ArgumentCaptor<DataPrepareFacadeService.PrepareAndExecuteRequest> prepareCaptor =
                ArgumentCaptor.forClass(DataPrepareFacadeService.PrepareAndExecuteRequest.class);
        verify(dataPrepareFacadeService, org.mockito.Mockito.times(2)).prepareAndExecute(prepareCaptor.capture());
        assertNotEquals(prepareCaptor.getAllValues().get(0).getGenerateRequest().getRequestBatchId(),
                prepareCaptor.getAllValues().get(1).getGenerateRequest().getRequestBatchId());
        assertNotEquals(prepareCaptor.getAllValues().get(0).getIdempotencyKey(),
                prepareCaptor.getAllValues().get(1).getIdempotencyKey());

        ArgumentCaptor<PlatformLaunchContext> launchCaptor = ArgumentCaptor.forClass(PlatformLaunchContext.class);
        verify(platformLaunchContextService, org.mockito.Mockito.times(2))
                .createLaunchContext(launchCaptor.capture());
        assertEquals("instance-1", launchCaptor.getAllValues().get(0).getDataInstanceId());
        assertEquals("CAPTURE", launchCaptor.getAllValues().get(0).getSdkMode());
        assertEquals("teacher-1", launchCaptor.getAllValues().get(0).getUserId());
        assertEquals("https://oa.example.com/purchase/apply", launchCaptor.getAllValues().get(0).getTargetUrl());
        assertEquals("instance-1", first.getDataInstanceId());
        assertEquals("instance-2", second.getDataInstanceId());
        verify(urlBuilder).build("https://oa.example.com/sso", "tenant-1", "ctx-one",
                "https://oa.example.com/purchase/apply");
    }

    @Test
    void shouldExposePrepareFailureReasonWhenAuthoringInstanceCannotBeCreated() {
        stubRecordConfiguration();
        when(dataRequirementService.createDataRequirement(any())).thenAnswer(invocation -> invocation.getArgument(0));
        DataPrepareJob failed = new DataPrepareJob();
        failed.setId("job-failed");
        failed.setJobStatus("FAILED");
        failed.setErrorMessage("OA 返回 businessModuleCode 不存在或未启用");
        when(dataPrepareFacadeService.prepareAndExecute(any())).thenReturn(failed);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.createLaunch(request()));

        assertEquals(ApiResultCode.STATE_NOT_ALLOWED.getCode(), exception.getCode());
        assertEquals("业务数据实例创建失败：OA 返回 businessModuleCode 不存在或未启用",
                exception.getMessage());
    }

    private void stubRecordConfiguration() {
        ConnectorSystem system = new ConnectorSystem();
        system.setId("system-1");
        system.setTenantId("tenant-1");
        system.setBaseUrl("https://oa.example.com/sso");
        system.setStatus("ACTIVE");
        system.setDeleted(false);
        when(connectorSystemService.getConnectorSystemById("system-1")).thenReturn(system);

        BusinessModule module = new BusinessModule();
        module.setId("module-1");
        module.setTenantId("tenant-1");
        module.setConnectorSystemId("system-1");
        module.setModuleCode("purchase");
        module.setEntryUrl("https://oa.example.com/purchase/apply");
        module.setStatus("ACTIVE");
        module.setDeleted(false);
        when(businessModuleService.getBusinessModuleById("module-1")).thenReturn(module);

        ModuleDataStrategy strategy = new ModuleDataStrategy();
        strategy.setId("strategy-record");
        strategy.setTenantId("tenant-1");
        strategy.setConnectorSystemId("system-1");
        strategy.setBusinessModuleId("module-1");
        strategy.setModuleCode("purchase");
        strategy.setSceneType("RECORD");
        strategy.setTemplateId("template-record");
        strategy.setInitExternalStatus("DRAFT");
        strategy.setStatus("ACTIVE");
        strategy.setDeleted(false);
        when(moduleDataStrategyService.getActiveStrategyByModuleCodeAndScene(
                "tenant-1", "system-1", "purchase", "RECORD")).thenReturn(strategy);

        TeachingDataTemplate template = new TeachingDataTemplate();
        template.setId("template-record");
        template.setTenantId("tenant-1");
        template.setConnectorSystemId("system-1");
        template.setModuleCode("purchase");
        template.setSceneType("RECORD");
        template.setTemplateCode("purchase-record");
        template.setInitState("DRAFT");
        template.setStatus("ACTIVE");
        template.setDeleted(false);
        when(teachingDataTemplateService.getTeachingDataTemplateById("template-record")).thenReturn(template);

        BusinessModuleProcessStep step = new BusinessModuleProcessStep();
        step.setId("step-1");
        step.setTenantId("tenant-1");
        step.setBusinessModuleId("module-1");
        step.setStepNo(1);
        step.setStatus("ACTIVE");
        when(processChainService.listActiveProcessSteps("tenant-1", "module-1"))
                .thenReturn(Collections.singletonList(step));

        BusinessModuleProcessActor actor = new BusinessModuleProcessActor();
        actor.setId("actor-1");
        actor.setTenantId("tenant-1");
        actor.setBusinessModuleId("module-1");
        actor.setProcessStepId("step-1");
        actor.setActorNo(1);
        actor.setActorType("CREATOR");
        actor.setRequiredOrgCode("origin-purchase-dept");
        actor.setRequiredOrgName("Purchase Department");
        actor.setRequiredRoleCode("creator");
        actor.setRequiredRoleName("Creator");
        actor.setIsRequired(true);
        actor.setStatus("ACTIVE");
        when(processChainService.listActiveProcessActors("tenant-1", "step-1"))
                .thenReturn(Collections.singletonList(actor));
    }

    private CurrentUserContext.CurrentUser currentTeacher() {
        return new CurrentUserContext.CurrentUser(
                "teacher-1", "tenant-1", "teacher01", "Teacher", "TEACHER",
                null, "T001", Arrays.asList("TEACHER"), Collections.emptyList(), Collections.emptyList());
    }

    private CreateAuthoringLaunchRequest request() {
        CreateAuthoringLaunchRequest request = new CreateAuthoringLaunchRequest();
        request.setLessonId("lesson-1");
        request.setConnectorSystemId("system-1");
        request.setBusinessModuleId("module-1");
        return request;
    }

    private DataPrepareJob successJob(String id, String requestBatchId) {
        DataPrepareJob job = new DataPrepareJob();
        job.setId(id);
        job.setTenantId("tenant-1");
        job.setRequestBatchId(requestBatchId);
        job.setJobStatus("SUCCESS");
        return job;
    }

    private TeachingDataInstance readyInstance(String id) {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId(id);
        instance.setTenantId("tenant-1");
        instance.setConnectorSystemId("system-1");
        instance.setOwnerUserId("teacher-1");
        instance.setSceneType("RECORD");
        instance.setValidationStatus("PASSED");
        instance.setInstanceStatus("READY");
        instance.setExternalBusinessId("business-1");
        instance.setExternalBusinessNo("PUR-001");
        return instance;
    }

    private PlatformLaunchContextService.CreatedLaunchContext createdContext(String id, String token) {
        PlatformLaunchContext context = new PlatformLaunchContext();
        context.setId(id);
        context.setTenantId("tenant-1");
        context.setExpireTime(LocalDateTime.of(2026, 8, 6, 20, 0));
        return new PlatformLaunchContextService.CreatedLaunchContext(context, token);
    }
}
