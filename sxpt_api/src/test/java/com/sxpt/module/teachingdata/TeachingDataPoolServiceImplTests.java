package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.TeachingDataPool;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataPoolStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.mapper.TeachingDataPoolMapper;
import com.sxpt.module.teachingdata.service.TeachingDataPoolService;
import com.sxpt.module.teachingdata.service.impl.TeachingDataPoolServiceImpl;
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
 * 教学数据池服务测试。
 *
 * 业务功能：
 * 1. 验证创建数据池时会补齐状态、统计计数、乐观锁、审计时间和软删除默认值。
 * 2. 验证幂等键是创建数据池的硬约束，避免同一需求批次重复生成库存聚合。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class TeachingDataPoolServiceImplTests {

    private final TeachingDataPoolMapper mapper = mock(TeachingDataPoolMapper.class);

    private final TeachingDataPoolService service = new TeachingDataPoolServiceImpl(mapper);

    /**
     * 验证创建教学数据池时写入 Mapper 并补齐默认值。
     */
    @Test
    void createTeachingDataPoolShouldInsertAndFillDefaults() {
        TeachingDataPool pool = buildValidPool();

        TeachingDataPool saved = service.createTeachingDataPool(pool);

        assertSame(pool, saved);
        assertEquals(DataPoolStatus.CREATED.getValue(), saved.getPoolStatus());
        assertEquals(0L, saved.getTotalCount());
        assertEquals(0L, saved.getReadyCount());
        assertEquals(0L, saved.getAllocatedCount());
        assertEquals(0L, saved.getFailedCount());
        assertEquals(0L, saved.getLockVersion());
        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 验证缺少幂等键时拒绝创建，避免重复数据池污染库存统计。
     */
    @Test
    void createTeachingDataPoolShouldRejectMissingIdempotencyKey() {
        TeachingDataPool pool = buildValidPool();
        pool.setIdempotencyKey(" ");

        assertThrows(BusinessException.class, () -> service.createTeachingDataPool(pool));
        verify(mapper, times(0)).insert(pool);
    }

    /**
     * 验证按任务和场景查询数据池时返回 Mapper 结果。
     */
    @Test
    void listByTaskAndSceneShouldReturnMapperResult() {
        TeachingDataPool pool = buildValidPool();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(pool));

        List<TeachingDataPool> result = service.listByTaskAndScene("tenant_001", "task_001", "PRACTICE");

        assertEquals(1, result.size());
        assertSame(pool, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 验证查询就绪数据池时要求题目 ID，避免跨题目领取数据。
     */
    @Test
    void listReadyPoolsShouldRejectMissingQuestionId() {
        assertThrows(BusinessException.class,
                () -> service.listReadyPools("tenant_001", "task_001", "PRACTICE", " "));
        verify(mapper, times(0)).selectList(any());
    }

    /**
     * 验证查询就绪数据池时返回 Mapper 结果。
     */
    @Test
    void listReadyPoolsShouldReturnMapperResult() {
        TeachingDataPool pool = buildValidPool();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(pool));

        List<TeachingDataPool> result = service.listReadyPools(
                "tenant_001", "task_001", "PRACTICE", "question_001");

        assertEquals(1, result.size());
        assertSame(pool, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效教学数据池。
     *
     * @return 教学数据池实体。
     */
    private TeachingDataPool buildValidPool() {
        TeachingDataPool pool = new TeachingDataPool();
        pool.setId("pool_001");
        pool.setTenantId("tenant_001");
        pool.setConnectorSystemId("connector_001");
        pool.setModuleCode("BUSINESS_APPLY");
        pool.setTaskId("task_001");
        pool.setSceneType("PRACTICE");
        pool.setQuestionId("question_001");
        pool.setRequirementId("req_001");
        pool.setIdempotencyKey("pool:tenant_001:req_001");
        return pool;
    }
}
