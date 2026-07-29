package com.sxpt.module.connector;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.service.TeachingDataInstanceService;
import com.sxpt.module.connector.service.impl.TeachingDataInstanceServiceImpl;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 教学业务数据实例服务测试。
 *
 * 业务功能：
 * 1. 验证创建实例时会补齐默认生命周期字段。
 * 2. 验证实例查询始终通过 Service 入口追加租户和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class TeachingDataInstanceServiceImplTests {

    private final TeachingDataInstanceMapper mapper = mock(TeachingDataInstanceMapper.class);

    private final TeachingDataInstanceService service = new TeachingDataInstanceServiceImpl(mapper);

    /**
     * 校验创建实例时写入 Mapper 并补齐默认值。
     */
    @Test
    void createTeachingDataInstanceShouldInsertAndFillDefaults() {
        TeachingDataInstance instance = buildValidInstance();

        TeachingDataInstance saved = service.createTeachingDataInstance(instance);

        assertSame(instance, saved);
        assertEquals(DataInstanceStatus.CREATED.getValue(), saved.getInstanceStatus());
        assertEquals(0L, saved.getResetCount());
        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少原平台业务数据 ID 时拒绝创建，避免产生无法跳转回原平台的数据实例。
     */
    @Test
    void createTeachingDataInstanceShouldRejectMissingExternalBusinessId() {
        TeachingDataInstance instance = buildValidInstance();
        instance.setExternalBusinessId(" ");

        assertThrows(BusinessException.class, () -> service.createTeachingDataInstance(instance));
        verify(mapper, times(0)).insert(instance);
    }

    /**
     * 校验按原平台业务数据 ID 查询时返回 Mapper 结果。
     */
    @Test
    void getByExternalBusinessShouldReturnMapperResult() {
        TeachingDataInstance instance = buildValidInstance();
        when(mapper.selectOne(any())).thenReturn(instance);

        TeachingDataInstance result = service.getByExternalBusiness(
                "tenant_001", "connector_001", "biz_001");

        assertSame(instance, result);
        verify(mapper).selectOne(any());
    }

    /**
     * 校验按使用人和场景查询时返回 Mapper 结果。
     */
    @Test
    void listByOwnerAndSceneShouldReturnMapperResult() {
        TeachingDataInstance instance = buildValidInstance();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(instance));

        List<TeachingDataInstance> result = service.listByOwnerAndScene(
                "tenant_001", "user_001", "PRACTICE");

        assertEquals(1, result.size());
        assertSame(instance, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 校验按任务和场景查询时返回 Mapper 结果。
     */
    @Test
    void listByTaskAndSceneShouldReturnMapperResult() {
        TeachingDataInstance instance = buildValidInstance();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(instance));

        List<TeachingDataInstance> result = service.listByTaskAndScene(
                "tenant_001", "task_001", "EXAM");

        assertEquals(1, result.size());
        assertSame(instance, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效教学业务数据实例。
     *
     * @return 教学业务数据实例实体。
     */
    /**
     * 校验锁定实例时写入锁定状态和锁定时间。
     */
    @Test
    void lockTeachingDataInstanceShouldMarkInstanceLocked() {
        TeachingDataInstance instance = buildValidInstance();
        when(mapper.selectById("tdi_001")).thenReturn(instance);

        TeachingDataInstance result = service.lockTeachingDataInstance("tdi_001");

        assertSame(instance, result);
        assertEquals(DataInstanceStatus.LOCKED.getValue(), result.getInstanceStatus());
        assertNotNull(result.getLockTime());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(instance);
    }

    /**
     * 校验已锁定实例不能废弃，避免考试数据被后续流程破坏。
     */
    @Test
    void discardTeachingDataInstanceShouldRejectLockedInstance() {
        TeachingDataInstance instance = buildValidInstance();
        instance.setInstanceStatus(DataInstanceStatus.LOCKED.getValue());
        when(mapper.selectById("tdi_001")).thenReturn(instance);

        assertThrows(BusinessException.class, () -> service.discardTeachingDataInstance("tdi_001"));
        verify(mapper, times(0)).updateById(instance);
    }

    /**
     * 校验废弃实例时写入废弃状态并禁用通用状态。
     */
    @Test
    void discardTeachingDataInstanceShouldDisableInstance() {
        TeachingDataInstance instance = buildValidInstance();
        when(mapper.selectById("tdi_001")).thenReturn(instance);

        TeachingDataInstance result = service.discardTeachingDataInstance("tdi_001");

        assertSame(instance, result);
        assertEquals(DataInstanceStatus.DISCARDED.getValue(), result.getInstanceStatus());
        assertEquals(RecordStatus.DISABLED.getValue(), result.getStatus());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(instance);
    }

    /**
     * 校验重置实例时替换原平台业务数据引用并增加重置次数。
     */
    @Test
    void resetTeachingDataInstanceShouldReplaceExternalBusinessAndIncreaseResetCount() {
        TeachingDataInstance instance = buildValidInstance();
        instance.setResetCount(2L);
        when(mapper.selectById("tdi_001")).thenReturn(instance);

        TeachingDataInstance result = service.resetTeachingDataInstance(
                "tdi_001", "biz_002", "NO-002", "DRAFT", "{\"source\":\"reset\"}");

        assertSame(instance, result);
        assertEquals(DataInstanceStatus.RESET.getValue(), result.getInstanceStatus());
        assertEquals(3L, result.getResetCount());
        assertEquals("biz_002", result.getExternalBusinessId());
        assertEquals("NO-002", result.getExternalBusinessNo());
        assertEquals("DRAFT", result.getExternalStatus());
        assertEquals("{\"source\":\"reset\"}", result.getMetadataJson());
        assertNotNull(result.getUpdateTime());
        verify(mapper).updateById(instance);
    }

    /**
     * 校验实例不存在或已软删除时拒绝生命周期操作。
     */
    @Test
    void lockTeachingDataInstanceShouldRejectDeletedInstance() {
        TeachingDataInstance instance = buildValidInstance();
        instance.setDeleted(Boolean.TRUE);
        when(mapper.selectById("tdi_001")).thenReturn(instance);

        assertThrows(BusinessException.class, () -> service.lockTeachingDataInstance("tdi_001"));
        verify(mapper, times(0)).updateById(instance);
    }

    private TeachingDataInstance buildValidInstance() {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId("tdi_001");
        instance.setTenantId("tenant_001");
        instance.setTemplateId("tpl_001");
        instance.setConnectorSystemId("connector_001");
        instance.setOwnerUserId("user_001");
        instance.setTaskId("task_001");
        instance.setSceneType("PRACTICE");
        instance.setExternalBusinessId("biz_001");
        return instance;
    }
}
