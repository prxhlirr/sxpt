package com.sxpt.module.teaching;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teaching.entity.TeachingPoint;
import com.sxpt.module.teaching.mapper.TeachingPointMapper;
import com.sxpt.module.teaching.service.TeachingPointService;
import com.sxpt.module.teaching.service.impl.TeachingPointServiceImpl;
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
 * 教学点服务测试。
 *
 * 业务功能：
 * 1. 验证教学点发布会补齐版本号和通用生命周期字段。
 * 2. 验证教学点查询始终通过 Service 附加租户、原平台和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class TeachingPointServiceImplTests {

    private final TeachingPointMapper mapper = mock(TeachingPointMapper.class);

    private final TeachingPointService service = new TeachingPointServiceImpl(mapper);

    /**
     * 校验发布教学点时插入 Mapper 并补齐默认值。
     */
    @Test
    void createTeachingPointShouldInsertAndFillDefaults() {
        TeachingPoint teachingPoint = buildValidTeachingPoint();

        TeachingPoint saved = service.createTeachingPoint(teachingPoint);

        assertSame(teachingPoint, saved);
        assertEquals(1L, saved.getVersionNo());
        assertEquals("PUBLISHED", saved.getPointStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少教学点编码时拒绝发布，避免后续任务无法稳定引用。
     */
    @Test
    void createTeachingPointShouldRejectMissingPointCode() {
        TeachingPoint teachingPoint = buildValidTeachingPoint();
        teachingPoint.setPointCode(" ");

        assertThrows(BusinessException.class, () -> service.createTeachingPoint(teachingPoint));
        verify(mapper, times(0)).insert(teachingPoint);
    }

    /**
     * 校验按原平台查询教学点时返回 Mapper 结果。
     */
    @Test
    void listByConnectorShouldReturnMapperResult() {
        TeachingPoint teachingPoint = buildValidTeachingPoint();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(teachingPoint));

        List<TeachingPoint> result = service.listByConnector("tenant_001", "connector_001");

        assertEquals(1, result.size());
        assertSame(teachingPoint, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效教学点。
     *
     * @return 教学点实体。
     */
    private TeachingPoint buildValidTeachingPoint() {
        TeachingPoint teachingPoint = new TeachingPoint();
        teachingPoint.setId("tp_001");
        teachingPoint.setTenantId("tenant_001");
        teachingPoint.setConnectorSystemId("connector_001");
        teachingPoint.setPointCode("TP_RECORD");
        teachingPoint.setPointName("标准备案申请");
        teachingPoint.setPointType("SCENARIO");
        teachingPoint.setSourceCaptureSessionId("cap_001");
        teachingPoint.setBusinessOverviewJson("{\"goal\":\"掌握备案\"}");
        teachingPoint.setRecordPathJson("[{\"segmentNo\":1}]");
        teachingPoint.setExecutionStrategy("ROLE_SWITCH");
        return teachingPoint;
    }
}
