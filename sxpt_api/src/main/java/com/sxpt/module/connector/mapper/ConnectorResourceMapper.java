package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ConnectorResource;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台正式资源 Mapper。
 *
 * 业务功能：
 * 1. 绑定 connector_resource 表的基础持久化能力。
 * 2. 为资源沉淀、资源查询、教学步骤引用和评分项引用提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责资源是否值得沉淀、租户边界和默认生命周期字段。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface ConnectorResourceMapper extends BaseMapper<ConnectorResource> {

    /**
     * 按租户、原平台和资源编码原子创建或更新正式资源。
     *
     * <p>重复的备案发布、浏览器重试或并发请求会命中数据库唯一索引，
     * 保留首次创建的资源 ID，并刷新可变的页面定位、元数据和来源采集会话。</p>
     *
     * @param resource 待沉淀的正式资源。
     * @return 受影响行数。
     */
    @Insert("INSERT INTO connector_resource (" +
            "id, tenant_id, connector_system_id, resource_code, resource_name, resource_type, " +
            "page_url, locator, stable_key, metadata_json, source_capture_id, create_by, " +
            "create_time, update_by, update_time, status, deleted" +
            ") VALUES (" +
            "#{id}, #{tenantId}, #{connectorSystemId}, #{resourceCode}, #{resourceName}, #{resourceType}, " +
            "#{pageUrl}, #{locator}, #{stableKey}, #{metadataJson}, #{sourceCaptureId}, #{createBy}, " +
            "#{createTime}, #{updateBy}, #{updateTime}, #{status}, #{deleted}" +
            ") ON CONFLICT (tenant_id, connector_system_id, resource_code) WHERE deleted = false " +
            "DO UPDATE SET " +
            "resource_name = EXCLUDED.resource_name, " +
            "resource_type = EXCLUDED.resource_type, " +
            "page_url = EXCLUDED.page_url, " +
            "locator = EXCLUDED.locator, " +
            "stable_key = EXCLUDED.stable_key, " +
            "metadata_json = EXCLUDED.metadata_json, " +
            "source_capture_id = EXCLUDED.source_capture_id, " +
            "update_by = EXCLUDED.update_by, " +
            "update_time = EXCLUDED.update_time, " +
            "status = EXCLUDED.status")
    int upsertByBusinessKey(ConnectorResource resource);
}
