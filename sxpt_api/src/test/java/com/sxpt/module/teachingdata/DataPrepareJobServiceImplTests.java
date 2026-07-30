package com.sxpt.module.teachingdata;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.DataPrepareJob;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.PrepareJobStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.TriggerType;
import com.sxpt.module.teachingdata.mapper.DataPrepareJobMapper;
import com.sxpt.module.teachingdata.service.DataPrepareJobService;
import com.sxpt.module.teachingdata.service.impl.DataPrepareJobServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
 * 数据准备任务服务测试。
 *
 * 业务功能：
 * 1. 验证创建任务时会补齐状态、计数、重试次数、审计时间和软删除默认值。
 * 2. 验证幂等键是创建任务的硬约束，避免原平台准备接口被重复调用后无法追踪。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class DataPrepareJobServiceImplTests {

    private final DataPrepareJobMapper mapper = mock(DataPrepareJobMapper.class);

    private final DataPrepareJobService service = new DataPrepareJobServiceImpl(mapper);

    /**
     * 验证创建数据准备任务时写入 Mapper 并补齐默认值。
     */
    @Test
    void createDataPrepareJobShouldInsertAndFillDefaults() {
        DataPrepareJob job = buildValidJob();

        DataPrepareJob saved = service.createDataPrepareJob(job);

        assertSame(job, saved);
        assertEquals(PrepareJobStatus.CREATED.getValue(), saved.getJobStatus());
        assertEquals(0L, saved.getExpectedCount());
        assertEquals(0L, saved.getSuccessCount());
        assertEquals(0L, saved.getFailedCount());
        assertEquals(0L, saved.getRetryCount());
        assertEquals(RecordStatus.ACTIVE.getValue(), saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 验证缺少幂等键时拒绝创建，避免外部准备请求无法去重和审计。
     */
    @Test
    void createDataPrepareJobShouldRejectMissingIdempotencyKey() {
        DataPrepareJob job = buildValidJob();
        job.setIdempotencyKey(" ");

        assertThrows(BusinessException.class, () -> service.createDataPrepareJob(job));
        verify(mapper, times(0)).insert(job);
    }

    /**
     * 验证按幂等键查询任务时返回 Mapper 命中结果。
     */
    @Test
    void getByIdempotencyKeyShouldReturnMapperResult() {
        DataPrepareJob job = buildValidJob();
        when(mapper.selectOne(any())).thenReturn(job);

        DataPrepareJob result = service.getByIdempotencyKey("tenant_001", "prepare:tenant_001:batch_001");

        assertSame(job, result);
        verify(mapper).selectOne(any());
    }

    /**
     * 验证按租户和任务 ID 查询任务时返回 Mapper 命中结果，避免重试入口绕过租户边界。
     */
    @Test
    void getByTenantAndIdShouldReturnMapperResult() {
        DataPrepareJob job = buildValidJob();
        when(mapper.selectOne(any())).thenReturn(job);

        DataPrepareJob result = service.getByTenantAndId("tenant_001", "job_001");

        assertSame(job, result);
        verify(mapper).selectOne(any());
    }

    /**
     * 验证失败任务进入重试前会推进 retryCount 并恢复为 CREATED，保证重试行为可审计。
     */
    @Test
    void markRetryingShouldUpdateRetryFields() {
        DataPrepareJob job = buildValidJob();
        job.setJobStatus(PrepareJobStatus.FAILED.getValue());
        job.setRetryCount(2L);
        when(mapper.selectOne(any())).thenReturn(job);

        DataPrepareJob result = service.markRetrying("tenant_001", "job_001", "admin_001");

        assertSame(job, result);
        assertEquals(PrepareJobStatus.CREATED.getValue(), result.getJobStatus());
        assertEquals(3L, result.getRetryCount());
        assertEquals("admin_001", result.getUpdateBy());
        verify(mapper).updateById(job);
    }

    /**
     * 验证成功任务不能被标记为重试，避免终态成功数据被重复创建。
     */
    @Test
    void markRetryingShouldRejectNonRetryableJob() {
        DataPrepareJob job = buildValidJob();
        job.setJobStatus(PrepareJobStatus.SUCCESS.getValue());
        when(mapper.selectOne(any())).thenReturn(job);

        assertThrows(BusinessException.class,
                () -> service.markRetrying("tenant_001", "job_001", "admin_001"));
        verify(mapper, times(0)).updateById(job);
    }

    /**
     * 验证按任务和场景查询任务时返回 Mapper 结果。
     */
    @Test
    void listByTaskAndSceneShouldReturnMapperResult() {
        DataPrepareJob job = buildValidJob();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(job));

        List<DataPrepareJob> result = service.listByTaskAndScene("tenant_001", "task_001", "PRACTICE");

        assertEquals(1, result.size());
        assertSame(job, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 验证查询待重试任务时要求传入当前时间。
     */
    @Test
    void listRetryableJobsShouldRejectMissingNow() {
        assertThrows(BusinessException.class, () -> service.listRetryableJobs("tenant_001", null));
        verify(mapper, times(0)).selectList(any());
    }

    /**
     * 验证查询待重试任务时返回 Mapper 结果。
     */
    @Test
    void listRetryableJobsShouldReturnMapperResult() {
        DataPrepareJob job = buildValidJob();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(job));

        List<DataPrepareJob> result = service.listRetryableJobs("tenant_001", LocalDateTime.now());

        assertEquals(1, result.size());
        assertSame(job, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效数据准备任务。
     *
     * @return 数据准备任务实体。
     */
    private DataPrepareJob buildValidJob() {
        DataPrepareJob job = new DataPrepareJob();
        job.setId("job_001");
        job.setTenantId("tenant_001");
        job.setConnectorSystemId("connector_001");
        job.setModuleCode("BUSINESS_APPLY");
        job.setSceneType("PRACTICE");
        job.setTaskId("task_001");
        job.setJobType("PREPARE");
        job.setRequestBatchId("batch_001");
        job.setIdempotencyKey("prepare:tenant_001:batch_001");
        job.setTriggerType(TriggerType.ON_DEMAND.getValue());
        return job;
    }
}
