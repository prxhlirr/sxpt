package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import com.sxpt.module.connector.service.impl.ConnectorSystemServiceImpl;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 原业务平台配置服务测试。
 *
 * 业务功能：
 * 1. 验证创建原平台配置时会补齐默认生命周期字段。
 * 2. 验证缺少必填字段时不会写入数据库。
 *
 * 关键流程：
 * 1. 使用 Mockito 模拟 Mapper，避免单元测试依赖真实数据库。
 * 2. 直接调用 Service，聚焦业务规则和持久化调用边界。
 */
class ConnectorSystemServiceImplTests {

    /**
     * 校验创建原平台配置时写入 Mapper 并补齐默认值。
     */
    @Test
    void createConnectorSystemShouldFillDefaultsAndInsert() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();

        ConnectorSystem saved = service.createConnectorSystem(connectorSystem);

        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertFalse(saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals("system", saved.getCreateBy());
        assertEquals("system", saved.getUpdateBy());
        verify(mapper, times(1)).insert(saved);
    }

    /**
     * 验证创建本地联调平台时会补齐平台能力审计字段，避免能力表非空约束导致平台创建失败。
     */
    @Test
    void createLocalDevConnectorSystemShouldFillCapabilityAuditFields() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        PlatformCapabilityMapper capabilityMapper = mock(PlatformCapabilityMapper.class);
        ConnectorSystemServiceImpl service = new ConnectorSystemServiceImpl(mapper, capabilityMapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        connectorSystem.setSystemType("LOCAL_DEV");
        connectorSystem.setCreateBy("admin_001");

        service.createConnectorSystem(connectorSystem);

        ArgumentCaptor<PlatformCapability> captor = ArgumentCaptor.forClass(PlatformCapability.class);
        verify(capabilityMapper, times(1)).insert(captor.capture());
        for (PlatformCapability capability : captor.getAllValues()) {
            assertEquals("admin_001", capability.getCreateBy());
            assertEquals("admin_001", capability.getUpdateBy());
            assertEquals(capability.getCapabilityCode(), capability.getCapabilityType());
            assertNotNull(capability.getCreateTime());
            assertNotNull(capability.getUpdateTime());
        }
    }

    /**
     * 校验缺少必填字段时拒绝创建。
     */
    @Test
    void createConnectorSystemShouldRejectMissingRequiredField() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        connectorSystem.setSystemCode(" ");

        assertThrows(BusinessException.class, () -> service.createConnectorSystem(connectorSystem));
        verify(mapper, times(0)).insert(connectorSystem);
    }

    /**
     * 验证首期原平台接入只允许 API_KEY 认证，避免前端隐藏项绕过后形成不可执行配置。
     */
    @Test
    void createConnectorSystemShouldRejectUnsupportedAuthType() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        connectorSystem.setAuthType("TOKEN");

        assertThrows(BusinessException.class, () -> service.createConnectorSystem(connectorSystem));
        verify(mapper, times(0)).insert(connectorSystem);
    }

    /**
     * 验证 API_KEY 认证必须包含 apiKey，保证 HTTP 适配器运行时能构造认证请求头。
     */
    @Test
    void createConnectorSystemShouldRejectMissingApiKeyConfig() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        connectorSystem.setConfigJson("{\"headerName\":\"X-API-Key\"}");

        assertThrows(BusinessException.class, () -> service.createConnectorSystem(connectorSystem));
        verify(mapper, times(0)).insert(connectorSystem);
    }

    /**
     * 校验创建原平台时环境类型统一归一为 PROD/LEARNING，环境组编码去掉首尾空格。
     */
    @Test
    void createConnectorSystemShouldNormalizeEnvironmentFields() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        connectorSystem.setEnvironmentType(" learning ");
        connectorSystem.setEnvironmentGroupCode(" OA_PURCHASE ");

        ConnectorSystem saved = service.createConnectorSystem(connectorSystem);

        assertEquals("LEARNING", saved.getEnvironmentType());
        assertEquals("OA_PURCHASE", saved.getEnvironmentGroupCode());
        verify(mapper, times(1)).insert(saved);
    }

    /**
     * 校验原平台环境类型只能是正式环境或学习环境，避免经典案例复制时落入不可识别环境。
     */
    @Test
    void createConnectorSystemShouldRejectUnsupportedEnvironmentType() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        connectorSystem.setEnvironmentType("SANDBOX");

        assertThrows(BusinessException.class, () -> service.createConnectorSystem(connectorSystem));
        verify(mapper, times(0)).insert(connectorSystem);
    }

    /**
     * 校验更新原平台配置时只覆盖允许编辑字段并刷新更新时间。
     */
    @Test
    void updateConnectorSystemShouldUpdateEditableFields() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem existing = buildValidConnectorSystem();
        existing.setCreateTime(LocalDateTime.now().minusDays(1));
        when(mapper.selectOne(any())).thenReturn(existing);

        ConnectorSystem update = new ConnectorSystem();
        update.setId("connector_001");
        update.setSystemName("更新后的原业务平台");
        update.setSystemType("CUSTOM");
        update.setEnvironmentType(" prod ");
        update.setEnvironmentGroupCode(" OA_PURCHASE ");
        update.setBaseUrl("https://new-origin.example.com");
        update.setAuthType("API_KEY");
        update.setConfigJson("{\"apiKey\":\"new-key\"}");

        ConnectorSystem result = service.updateConnectorSystem(update);

        assertEquals("tenant_001", result.getTenantId());
        assertEquals("origin_platform", result.getSystemCode());
        assertEquals("更新后的原业务平台", result.getSystemName());
        assertEquals("PROD", result.getEnvironmentType());
        assertEquals("OA_PURCHASE", result.getEnvironmentGroupCode());
        assertEquals("https://new-origin.example.com", result.getBaseUrl());
        assertEquals("{\"apiKey\":\"new-key\"}", result.getConfigJson());
        assertNotNull(result.getUpdateTime());
        verify(mapper, times(1)).updateById(result);
    }

    /**
     * 校验更新原平台基础信息时，如果请求没有携带配置 JSON，应保留数据库已有配置。
     */
    @Test
    void updateConnectorSystemShouldKeepExistingConfigJsonWhenRequestConfigIsNull() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem existing = buildValidConnectorSystem();
        existing.setConfigJson("{\"apiKey\":\"local-dev-api-key\",\"adapter\":\"local\"}");
        existing.setEnvironmentType("LEARNING");
        existing.setEnvironmentGroupCode("LOCAL_DEV");
        when(mapper.selectOne(any())).thenReturn(existing);

        ConnectorSystem update = new ConnectorSystem();
        update.setId("connector_001");
        update.setSystemName("updated origin");
        update.setSystemType("LOCAL_DEV");
        update.setBaseUrl("http://127.0.0.1:8080/local-origin");
        update.setAuthType("API_KEY");

        ConnectorSystem result = service.updateConnectorSystem(update);

        assertEquals("{\"apiKey\":\"local-dev-api-key\",\"adapter\":\"local\"}", result.getConfigJson());
        assertEquals("LEARNING", result.getEnvironmentType());
        assertEquals("LOCAL_DEV", result.getEnvironmentGroupCode());
        verify(mapper, times(1)).updateById(result);
    }

    /**
     * 校验更新不存在的原平台配置时返回业务异常。
     */
    @Test
    void updateConnectorSystemShouldRejectMissingRecord() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem update = buildValidConnectorSystem();
        when(mapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.updateConnectorSystem(update));
        verify(mapper, times(0)).updateById(update);
    }

    /**
     * 校验更新缺少必填字段时拒绝写入。
     */
    @Test
    void updateConnectorSystemShouldRejectMissingRequiredField() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem update = buildValidConnectorSystem();
        update.setBaseUrl(" ");

        assertThrows(BusinessException.class, () -> service.updateConnectorSystem(update));
        verify(mapper, times(0)).updateById(update);
    }

    /**
     * 校验启用原平台配置时写入 ACTIVE 状态。
     */
    @Test
    void enableConnectorSystemShouldSetActiveStatus() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem existing = buildValidConnectorSystem();
        existing.setStatus(RecordStatus.DISABLED.getValue());
        when(mapper.selectOne(any())).thenReturn(existing);

        ConnectorSystem result = service.enableConnectorSystem("connector_001");

        assertEquals(RecordStatus.ACTIVE.getValue(), result.getStatus());
        assertNotNull(result.getUpdateTime());
        verify(mapper, times(1)).updateById(result);
    }

    /**
     * 校验禁用原平台配置时写入 DISABLED 状态。
     */
    @Test
    void disableConnectorSystemShouldSetDisabledStatus() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem existing = buildValidConnectorSystem();
        existing.setStatus(RecordStatus.ACTIVE.getValue());
        when(mapper.selectOne(any())).thenReturn(existing);

        ConnectorSystem result = service.disableConnectorSystem("connector_001");

        assertEquals(RecordStatus.DISABLED.getValue(), result.getStatus());
        assertNotNull(result.getUpdateTime());
        verify(mapper, times(1)).updateById(result);
    }

    /**
     * 校验缺少 ID 时拒绝状态切换。
     */
    @Test
    void changeStatusShouldRejectBlankId() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);

        assertThrows(BusinessException.class, () -> service.enableConnectorSystem(" "));
        verify(mapper, times(0)).selectOne(any());
        verify(mapper, times(0)).updateById(any());
    }

    /**
     * 校验根据 ID 查询原平台配置详情时会返回未删除数据。
     */
    @Test
    void getConnectorSystemByIdShouldReturnExistingRecord() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        when(mapper.selectOne(any())).thenReturn(connectorSystem);

        ConnectorSystem result = service.getConnectorSystemById("connector_001");

        assertEquals("connector_001", result.getId());
        verify(mapper, times(1)).selectOne(any());
    }

    /**
     * 校验根据 ID 查询不到原平台配置时返回业务异常。
     */
    @Test
    void getConnectorSystemByIdShouldRejectMissingRecord() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        when(mapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.getConnectorSystemById("connector_missing"));
        verify(mapper, times(1)).selectOne(any());
    }

    /**
     * 校验按租户查询原平台配置列表。
     */
    @Test
    void listConnectorSystemsByTenantIdShouldReturnTenantRecords() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);
        ConnectorSystem connectorSystem = buildValidConnectorSystem();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(connectorSystem));

        List<ConnectorSystem> result = service.listConnectorSystemsByTenantId("tenant_001");

        assertEquals(1, result.size());
        assertEquals("tenant_001", result.get(0).getTenantId());
        verify(mapper, times(1)).selectList(any());
    }

    /**
     * 校验缺少租户 ID 时拒绝列表查询。
     */
    @Test
    void listConnectorSystemsByTenantIdShouldRejectBlankTenantId() {
        ConnectorSystemMapper mapper = mock(ConnectorSystemMapper.class);
        ConnectorSystemServiceImpl service = buildService(mapper);

        assertThrows(BusinessException.class, () -> service.listConnectorSystemsByTenantId(" "));
        verify(mapper, times(0)).selectList(any());
    }

    /**
     * 构造最小有效原平台配置。
     *
     * @return 原平台配置实体。
     */
    private ConnectorSystem buildValidConnectorSystem() {
        ConnectorSystem connectorSystem = new ConnectorSystem();
        connectorSystem.setId("connector_001");
        connectorSystem.setTenantId("tenant_001");
        connectorSystem.setSystemCode("origin_platform");
        connectorSystem.setSystemName("原业务平台");
        connectorSystem.setSystemType("CUSTOM");
        connectorSystem.setBaseUrl("https://origin.example.com");
        connectorSystem.setAuthType("API_KEY");
        connectorSystem.setConfigJson("{\"apiKey\":\"key-0805\"}");
        return connectorSystem;
    }

    /**
     * 构造带能力注册依赖的服务，避免测试遗漏 LOCAL_DEV 平台自动补能力的协作者。
     *
     * @param mapper 原平台配置 Mapper。
     * @return 原平台配置服务。
     */
    private ConnectorSystemServiceImpl buildService(ConnectorSystemMapper mapper) {
        return new ConnectorSystemServiceImpl(mapper, mock(PlatformCapabilityMapper.class));
    }
}
