package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.ClassicCaseImportRequest;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.ClassicCaseAsset;
import com.sxpt.module.connector.entity.ClassicCaseVersion;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.mapper.BusinessModuleProcessActorMapper;
import com.sxpt.module.connector.mapper.ClassicCaseAssetMapper;
import com.sxpt.module.connector.mapper.ClassicCaseVersionMapper;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.service.impl.ClassicCaseServiceImpl;
import com.sxpt.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassicCaseModuleRelocationTests {

    @Mock
    private ClassicCaseAssetMapper assetMapper;

    @Mock
    private ClassicCaseVersionMapper versionMapper;

    @Mock
    private ConnectorSystemMapper connectorSystemMapper;

    @Mock
    private BusinessModuleMapper businessModuleMapper;

    @Mock
    private BusinessModuleProcessActorMapper processActorMapper;

    private ClassicCaseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ClassicCaseServiceImpl(
                assetMapper,
                versionMapper,
                null,
                null,
                connectorSystemMapper,
                businessModuleMapper,
                processActorMapper,
                null,
                null,
                null,
                null);
    }

    @Test
    void shouldRelocateUnchangedCaseVersionToAnotherLearningModule() {
        ConnectorSystem source = system("source", "PROD");
        ConnectorSystem oldLearning = system("old-learning", "LEARNING");
        ConnectorSystem targetLearning = system("test-0805", "LEARNING");
        BusinessModule oldModule = module("old-module", oldLearning.getId(), "doc_incoming");
        BusinessModule targetModule = module(
                "target-module",
                targetLearning.getId(),
                "record_apply_1785916779495");

        when(connectorSystemMapper.selectOne(any()))
                .thenReturn(
                        source, oldLearning,
                        source, targetLearning,
                        source, targetLearning,
                        source, targetLearning,
                        source, targetLearning);
        when(businessModuleMapper.selectOne(any()))
                .thenReturn(oldModule, targetModule, targetModule, targetModule, targetModule);

        AtomicReference<ClassicCaseAsset> storedAsset = new AtomicReference<>();
        AtomicReference<ClassicCaseVersion> storedVersion = new AtomicReference<>();
        when(assetMapper.selectOne(any())).thenAnswer(invocation -> copyAsset(storedAsset.get()));
        when(versionMapper.selectOne(any())).thenAnswer(invocation -> copyVersion(storedVersion.get()));
        doAnswer(invocation -> {
            storedAsset.set(copyAsset(invocation.getArgument(0)));
            return 1;
        }).when(assetMapper).insert(any());
        doAnswer(invocation -> {
            storedVersion.set(copyVersion(invocation.getArgument(0)));
            return 1;
        }).when(versionMapper).insert(any());
        doAnswer(invocation -> {
            storedAsset.set(copyAsset(invocation.getArgument(0)));
            return 1;
        }).when(assetMapper).updateById(any());
        doAnswer(invocation -> {
            storedVersion.set(copyVersion(invocation.getArgument(0)));
            return 1;
        }).when(versionMapper).updateById(any());

        ClassicCaseAsset original = service.importClassicCase(request(
                oldLearning.getId(), oldModule.getId(), oldModule.getModuleCode()));
        String originalAssetId = original.getId();
        String originalVersionId = storedVersion.get().getId();
        String originalContentHash = storedVersion.get().getContentHash();

        AtomicReference<ClassicCaseAsset> relocatedResult = new AtomicReference<>();
        assertThatCode(() -> relocatedResult.set(service.importClassicCase(request(
                targetLearning.getId(), targetModule.getId(), targetModule.getModuleCode()))))
                .doesNotThrowAnyException();
        ClassicCaseAsset relocated = relocatedResult.get();

        assertThat(relocated.getId()).isEqualTo(originalAssetId);
        assertThat(relocated.getLearningConnectorSystemId()).isEqualTo(targetLearning.getId());
        assertThat(relocated.getBusinessModuleId()).isEqualTo(targetModule.getId());
        assertThat(relocated.getModuleCode()).isEqualTo(targetModule.getModuleCode());
        assertThat(relocated.getCurrentVersionId()).isEqualTo(originalVersionId);
        assertThat(storedVersion.get().getCaseVersionId()).isEqualTo("ccv-001");
        assertThat(storedVersion.get().getContentHash()).isNotEqualTo(originalContentHash);

        // 兼容已经迁移过但版本哈希尚未同步的历史数据。
        storedVersion.get().setContentHash(originalContentHash);
        storedAsset.get().setTagsJson("[\"收文\"]");
        storedVersion.get().setSupportedGenerationModesJson("[\"REPLAY_CASE\", \"FORMAT_DEMO\"]");
        storedVersion.get().setIdentityBindingJson("{ }");
        storedVersion.get().setDesensitizedCasePayloadJson("{\"title\": \"示例收文\"}");
        storedVersion.get().setCaseDataFormatJson(
                "{\"fields\": [{\"required\": true, \"type\": \"string\", \"name\": \"title\"}]}");

        assertThatCode(() -> service.importClassicCase(request(
                targetLearning.getId(), targetModule.getId(), targetModule.getModuleCode())))
                .as("迁移完成后 OA 对同一目标模块再次重推仍应保持幂等")
                .doesNotThrowAnyException();

        ClassicCaseImportRequest changedContent = request(
                targetLearning.getId(), targetModule.getId(), targetModule.getModuleCode());
        changedContent.setCaseSummary("被篡改的案例摘要");
        assertThatThrownBy(() -> service.importClassicCase(changedContent))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("相同 caseVersionId 对应的案例内容不一致");

        ClassicCaseImportRequest changedPolicy = request(
                targetLearning.getId(), targetModule.getId(), targetModule.getModuleCode());
        changedPolicy.setDesensitizePolicyJson("{\"mask\":\"changed\"}");
        assertThatThrownBy(() -> service.importClassicCase(changedPolicy))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("相同 caseVersionId 对应的案例内容不一致");
    }

    private ConnectorSystem system(String id, String environmentType) {
        ConnectorSystem system = new ConnectorSystem();
        system.setId(id);
        system.setTenantId("demo-tenant");
        system.setEnvironmentType(environmentType);
        system.setEnvironmentGroupCode("oa-local");
        system.setDeleted(Boolean.FALSE);
        return system;
    }

    private BusinessModule module(String id, String connectorSystemId, String moduleCode) {
        BusinessModule module = new BusinessModule();
        module.setId(id);
        module.setTenantId("demo-tenant");
        module.setConnectorSystemId(connectorSystemId);
        module.setModuleCode(moduleCode);
        module.setDeleted(Boolean.FALSE);
        return module;
    }

    private ClassicCaseImportRequest request(
            String learningSystemId,
            String businessModuleId,
            String moduleCode) {
        ClassicCaseImportRequest request = new ClassicCaseImportRequest();
        request.setTenantId("demo-tenant");
        request.setCaseCode("CC_INCOMING_001");
        request.setCaseVersionId("ccv-001");
        request.setCaseTitle("重点工作督查收文案例");
        request.setCaseSummary("收文登记教学案例");
        request.setSourceConnectorSystemId("source");
        request.setLearningConnectorSystemId(learningSystemId);
        request.setBusinessModuleId(businessModuleId);
        request.setModuleCode(moduleCode);
        request.setSceneTypesJson("[\"TEACHING\",\"PRACTICE\"]");
        request.setTagsJson("[\"收文\"]");
        request.setSupportedGenerationModesJson("[\"REPLAY_CASE\",\"FORMAT_DEMO\"]");
        request.setPayloadSchemaVersion("1.0");
        request.setIdentityBindingJson("{}");
        request.setDesensitizedCasePayloadJson("{\"title\":\"示例收文\"}");
        request.setCaseDataFormatJson(
                "{\"fields\":[{\"name\":\"title\",\"type\":\"string\",\"required\":true}]}");
        request.setSourceUpdatedAt(LocalDateTime.of(2026, 8, 18, 11, 49));
        request.setCreateBy("source");
        return request;
    }

    private ClassicCaseAsset copyAsset(ClassicCaseAsset source) {
        if (source == null) {
            return null;
        }
        ClassicCaseAsset target = new ClassicCaseAsset();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private ClassicCaseVersion copyVersion(ClassicCaseVersion source) {
        if (source == null) {
            return null;
        }
        ClassicCaseVersion target = new ClassicCaseVersion();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
