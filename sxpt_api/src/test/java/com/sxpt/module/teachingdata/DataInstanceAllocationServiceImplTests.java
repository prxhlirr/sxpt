package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.AllocationStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataPoolStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import com.sxpt.module.teachingdata.mapper.DataInstanceAllocationMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.DataInstanceAllocationService;
import com.sxpt.module.teachingdata.service.impl.DataInstanceAllocationServiceImpl;
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
 * 数据实例分配服务测试。
 *
 * 业务功能：
 * 1. 验证创建分配记录时会补齐状态、领取时间、审计时间和软删除默认值。
 * 2. 验证单位和角色是分配记录的硬约束，避免学生进入原平台时缺少操作上下文。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class DataInstanceAllocationServiceImplTests {

    private final DataInstanceAllocationMapper mapper = mock(DataInstanceAllocationMapper.class);

    private final TeachingDataPoolMapper poolMapper = mock(TeachingDataPoolMapper.class);

    private final TeachingDataInstanceMapper instanceMapper = mock(TeachingDataInstanceMapper.class);

    private final DataRequirementItemMapper dataRequirementItemMapper = mock(DataRequirementItemMapper.class);

    private final DataInstanceAllocationService service = new DataInstanceAllocationServiceImpl(
            mapper, poolMapper, instanceMapper, dataRequirementItemMapper);

    /**
     * 验证创建数据实例分配记录时写入 Mapper 并补齐默认值。
     */
    @Test
    void createDataInstanceAllocationShouldInsertAndFillDefaults() {
        DataInstanceAllocation allocation = buildValidAllocation();

        DataInstanceAllocation saved = service.createDataInstanceAllocation(allocation);

        assertSame(allocation, saved);
        assertEquals(AllocationStatus.ALLOCATED.getValue(), saved.getAllocationStatus());
        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getAllocateTime());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 验证缺少所需原平台单位时拒绝创建，避免分配记录无法驱动角色切换。
     */
    @Test
    void createDataInstanceAllocationShouldRejectMissingRequiredOrg() {
        DataInstanceAllocation allocation = buildValidAllocation();
        allocation.setRequiredExternalOrgId(" ");

        assertThrows(BusinessException.class, () -> service.createDataInstanceAllocation(allocation));
        verify(mapper, times(0)).insert(allocation);
    }

    /**
     * 验证按学生和场景查询分配记录时返回 Mapper 结果。
     */
    @Test
    void listByOwnerAndSceneShouldReturnMapperResult() {
        DataInstanceAllocation allocation = buildValidAllocation();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(allocation));

        List<DataInstanceAllocation> result = service.listByOwnerAndScene(
                "tenant_001", "task_001", "PRACTICE", "student_001");

        assertEquals(1, result.size());
        assertSame(allocation, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 验证按数据实例查询分配记录时返回 Mapper 结果。
     */
    @Test
    void listByDataInstanceShouldReturnMapperResult() {
        DataInstanceAllocation allocation = buildValidAllocation();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(allocation));

        List<DataInstanceAllocation> result = service.listByDataInstance("tenant_001", "instance_001");

        assertEquals(1, result.size());
        assertSame(allocation, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 验证按数据需求批次查询时先定位数据池，再返回这些数据池下的分配记录。
     */
    @Test
    void listByRequirementShouldReturnAllocationsInRequirementPools() {
        TeachingDataPool pool = buildReadyPool();
        DataInstanceAllocation allocation = buildValidAllocation();
        when(poolMapper.selectList(any())).thenReturn(Collections.singletonList(pool));
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(allocation));

        List<DataInstanceAllocation> result = service.listByRequirement("tenant_001", "requirement_001");

        assertEquals(1, result.size());
        assertSame(allocation, result.get(0));
        verify(poolMapper).selectList(any());
        verify(mapper).selectList(any());
    }

    /**
     * 验证领取 READY 数据池时，只有实例条件更新抢占成功后才会创建分配记录。
     */
    @Test
    void acquireReadyInstanceShouldAllocateAfterInstanceClaimed() {
        TeachingDataPool pool = buildReadyPool();
        TeachingDataInstance instance = buildReadyInstance();
        when(poolMapper.selectOne(any())).thenReturn(pool);
        when(instanceMapper.selectList(any())).thenReturn(Collections.singletonList(instance));
        when(mapper.selectCount(any())).thenReturn(0);
        when(instanceMapper.update(any(), any())).thenReturn(1);
        when(instanceMapper.selectCount(any())).thenReturn(0, 1);

        DataInstanceAllocation saved = service.acquireReadyInstance(buildAcquireRequest());

        assertEquals(instance.getId(), saved.getDataInstanceId());
        assertEquals(AllocationStatus.ALLOCATED.getValue(), saved.getAllocationStatus());
        assertEquals(DataInstanceStatus.ALLOCATED.getValue(), instance.getInstanceStatus());
        verify(instanceMapper).update(any(), any());
        verify(mapper).insert(saved);
        verify(poolMapper).updateById(pool);
    }

    /**
     * 验证并发场景下实例抢占失败时不会创建分配记录，避免同一个实例被重复领取。
     */
    @Test
    void acquireReadyInstanceShouldRejectWhenConditionalClaimFailed() {
        TeachingDataPool pool = buildReadyPool();
        TeachingDataInstance instance = buildReadyInstance();
        when(poolMapper.selectOne(any())).thenReturn(pool);
        when(instanceMapper.selectList(any())).thenReturn(Collections.singletonList(instance));
        when(mapper.selectCount(any())).thenReturn(0);
        when(instanceMapper.update(any(), any())).thenReturn(0);

        assertThrows(BusinessException.class, () -> service.acquireReadyInstance(buildAcquireRequest()));

        verify(mapper, times(0)).insert(any());
    }

    /**
     * 构造最小有效数据实例分配记录。
     *
     * @return 数据实例分配实体。
     */
    private DataInstanceAllocation buildValidAllocation() {
        DataInstanceAllocation allocation = new DataInstanceAllocation();
        allocation.setId("allocation_001");
        allocation.setTenantId("tenant_001");
        allocation.setPoolId("pool_001");
        allocation.setDataInstanceId("instance_001");
        allocation.setTaskId("task_001");
        allocation.setOwnerUserId("student_001");
        allocation.setAllocationScene("PRACTICE");
        allocation.setRequiredExternalOrgId("org_operator_001");
        allocation.setRequiredExternalRoleId("role_operator_001");
        allocation.setActorType("STUDENT");
        return allocation;
    }

    /**
     * 构造可领取数据池，覆盖领取接口需要的最小池状态。
     *
     * @return READY 数据池。
     */
    private TeachingDataPool buildReadyPool() {
        TeachingDataPool pool = new TeachingDataPool();
        pool.setId("pool_001");
        pool.setTenantId("tenant_001");
        pool.setTaskId("task_001");
        pool.setPoolStatus(DataPoolStatus.READY.getValue());
        pool.setReadyCount(1L);
        return pool;
    }

    /**
     * 构造已校验通过的可用实例，覆盖分配记录需要固化的原平台上下文。
     *
     * @return READY 数据实例。
     */
    private TeachingDataInstance buildReadyInstance() {
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId("instance_001");
        instance.setTenantId("tenant_001");
        instance.setPoolId("pool_001");
        instance.setTaskId("task_001");
        instance.setInstanceStatus(DataInstanceStatus.READY.getValue());
        instance.setValidationStatus(ValidationStatus.PASSED.getValue());
        instance.setRequiredExternalOrgId("org_operator_001");
        instance.setRequiredExternalOrgName("运营组织");
        instance.setRequiredExternalRoleId("role_operator_001");
        instance.setRequiredExternalRoleName("运营角色");
        instance.setActorType("STUDENT");
        return instance;
    }

    /**
     * 构造领取请求，覆盖学生、任务、场景和审计人这些创建分配记录的硬约束。
     *
     * @return 数据领取请求。
     */
    private DataInstanceAllocationService.AcquireReadyInstanceRequest buildAcquireRequest() {
        DataInstanceAllocationService.AcquireReadyInstanceRequest request =
                new DataInstanceAllocationService.AcquireReadyInstanceRequest();
        request.setTenantId("tenant_001");
        request.setPoolId("pool_001");
        request.setOwnerUserId("student_001");
        request.setTaskId("task_001");
        request.setAllocationScene("PRACTICE");
        request.setAttemptId("attempt_001");
        request.setQuestionAttemptId("question_attempt_001");
        request.setCreateBy("tester");
        request.setUpdateBy("tester");
        return request;
    }
}
