-- 数据准备证据链验证脚本
-- 使用方式：
--   psql -h 127.0.0.1 -p 5432 -U postgres -d sxpt_dev -f sxpt_api/src/main/resources/db/seed/data_prepare_evidence_chain_verify.sql
--
-- 参数说明：
--   tenant_id 必填。
--   requirement_id 可选，填空时按 request_batch_id 或最新创建的需求批次自动定位。
--   request_batch_id 可选，用于验证页面本次“创建批次/触发准备”生成的数据。
\set tenant_id 'demo-tenant'
\set requirement_id ''
\set request_batch_id ''

-- 00. 定位本次验证目标。
WITH target_requirement AS (
    SELECT r.*
    FROM data_requirement r
    WHERE r.tenant_id = :'tenant_id'
      AND r.deleted = false
      AND (
          NULLIF(:'requirement_id', '') IS NOT NULL
          AND r.id = NULLIF(:'requirement_id', '')
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM data_requirement_item ri
                  WHERE ri.requirement_id = r.id
                    AND ri.request_batch_id = NULLIF(:'request_batch_id', '')
                    AND ri.deleted = false
              )
          )
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NULL
          )
      )
    ORDER BY r.create_time DESC
    LIMIT 1
)
SELECT
    '00_TARGET_REQUIREMENT' AS check_name,
    id AS requirement_id,
    requirement_code,
    tenant_id,
    connector_system_id,
    business_module_id,
    module_code,
    strategy_id,
    template_id,
    task_id,
    class_id,
    scene_type,
    requirement_status,
    expected_count,
    success_count,
    failed_count,
    create_time,
    update_time
FROM target_requirement;

-- 01. 验证平台、业务模块、策略、模板是否构成一致链路。
WITH target_requirement AS (
    SELECT r.*
    FROM data_requirement r
    WHERE r.tenant_id = :'tenant_id'
      AND r.deleted = false
      AND (
          NULLIF(:'requirement_id', '') IS NOT NULL
          AND r.id = NULLIF(:'requirement_id', '')
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM data_requirement_item ri
                  WHERE ri.requirement_id = r.id
                    AND ri.request_batch_id = NULLIF(:'request_batch_id', '')
                    AND ri.deleted = false
              )
          )
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NULL
          )
      )
    ORDER BY r.create_time DESC
    LIMIT 1
)
SELECT
    '01_ARCHITECTURE_CHAIN' AS check_name,
    r.id AS requirement_id,
    cs.system_code,
    cs.system_name,
    cs.system_type,
    bm.module_code AS business_module_code,
    bm.module_name AS business_module_name,
    mds.id AS strategy_id,
    mds.scene_type AS strategy_scene_type,
    mds.template_id AS strategy_template_id,
    tdt.template_code,
    tdt.template_name,
    CASE WHEN cs.id IS NOT NULL THEN 'PASS' ELSE 'FAIL' END AS connector_exists,
    CASE WHEN bm.id IS NOT NULL THEN 'PASS' ELSE 'FAIL' END AS business_module_exists,
    CASE WHEN mds.id IS NOT NULL THEN 'PASS' ELSE 'FAIL' END AS strategy_exists,
    CASE WHEN tdt.id IS NOT NULL THEN 'PASS' ELSE 'FAIL' END AS template_exists,
    CASE WHEN r.business_module_id = bm.id AND r.module_code = bm.module_code THEN 'PASS' ELSE 'FAIL' END AS requirement_module_match,
    CASE WHEN r.strategy_id = mds.id AND r.template_id = mds.template_id THEN 'PASS' ELSE 'FAIL' END AS requirement_strategy_match,
    CASE WHEN r.template_id = tdt.id THEN 'PASS' ELSE 'FAIL' END AS requirement_template_match
FROM target_requirement r
LEFT JOIN connector_system cs
       ON cs.id = r.connector_system_id
      AND cs.tenant_id = r.tenant_id
      AND cs.deleted = false
LEFT JOIN business_module bm
       ON bm.id = r.business_module_id
      AND bm.tenant_id = r.tenant_id
      AND bm.connector_system_id = r.connector_system_id
      AND bm.deleted = false
LEFT JOIN module_data_strategy mds
       ON mds.id = r.strategy_id
      AND mds.tenant_id = r.tenant_id
      AND mds.connector_system_id = r.connector_system_id
      AND mds.business_module_id = r.business_module_id
      AND mds.module_code = r.module_code
      AND mds.scene_type = r.scene_type
      AND mds.deleted = false
LEFT JOIN teaching_data_template tdt
       ON tdt.id = r.template_id
      AND tdt.tenant_id = r.tenant_id
      AND tdt.connector_system_id = r.connector_system_id
      AND tdt.deleted = false;

-- 02. 验证需求、明细、实例、数据池之间的计数是否一致。
WITH target_requirement AS (
    SELECT r.*
    FROM data_requirement r
    WHERE r.tenant_id = :'tenant_id'
      AND r.deleted = false
      AND (
          NULLIF(:'requirement_id', '') IS NOT NULL
          AND r.id = NULLIF(:'requirement_id', '')
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM data_requirement_item ri
                  WHERE ri.requirement_id = r.id
                    AND ri.request_batch_id = NULLIF(:'request_batch_id', '')
                    AND ri.deleted = false
              )
          )
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NULL
          )
      )
    ORDER BY r.create_time DESC
    LIMIT 1
),
item_counter AS (
    SELECT
        ri.requirement_id,
        count(*) AS item_count,
        count(*) FILTER (WHERE ri.item_status = 'READY') AS item_ready_count,
        count(*) FILTER (WHERE ri.item_status = 'FAILED') AS item_failed_count,
        count(*) FILTER (WHERE ri.validation_status = 'PASSED') AS item_validation_passed_count,
        count(DISTINCT ri.request_batch_id) AS request_batch_count
    FROM data_requirement_item ri
    JOIN target_requirement r ON r.id = ri.requirement_id
    WHERE ri.deleted = false
    GROUP BY ri.requirement_id
),
instance_counter AS (
    SELECT
        tdi.requirement_id,
        count(*) AS instance_count,
        count(*) FILTER (WHERE tdi.instance_status = 'READY' AND tdi.validation_status = 'PASSED') AS instance_ready_count,
        count(*) FILTER (WHERE tdi.instance_status = 'FAILED') AS instance_failed_count,
        count(*) FILTER (WHERE tdi.pool_id IS NOT NULL) AS instance_with_pool_count
    FROM teaching_data_instance tdi
    JOIN target_requirement r ON r.id = tdi.requirement_id
    WHERE tdi.deleted = false
    GROUP BY tdi.requirement_id
),
pool_counter AS (
    SELECT
        tdp.requirement_id,
        count(*) AS pool_count,
        coalesce(sum(tdp.total_count), 0) AS pool_total_count,
        coalesce(sum(tdp.ready_count), 0) AS pool_ready_count,
        coalesce(sum(tdp.allocated_count), 0) AS pool_allocated_count,
        coalesce(sum(tdp.failed_count), 0) AS pool_failed_count
    FROM teaching_data_pool tdp
    JOIN target_requirement r ON r.id = tdp.requirement_id
    WHERE tdp.deleted = false
    GROUP BY tdp.requirement_id
)
SELECT
    '02_COUNT_RECONCILIATION' AS check_name,
    r.id AS requirement_id,
    r.requirement_status,
    r.expected_count AS requirement_expected_count,
    r.success_count AS requirement_success_count,
    r.failed_count AS requirement_failed_count,
    coalesce(ic.item_count, 0) AS item_count,
    coalesce(ic.item_ready_count, 0) AS item_ready_count,
    coalesce(ic.item_failed_count, 0) AS item_failed_count,
    coalesce(ic.item_validation_passed_count, 0) AS item_validation_passed_count,
    coalesce(ic.request_batch_count, 0) AS request_batch_count,
    coalesce(ins.instance_count, 0) AS instance_count,
    coalesce(ins.instance_ready_count, 0) AS instance_ready_count,
    coalesce(ins.instance_failed_count, 0) AS instance_failed_count,
    coalesce(ins.instance_with_pool_count, 0) AS instance_with_pool_count,
    coalesce(pc.pool_count, 0) AS pool_count,
    coalesce(pc.pool_total_count, 0) AS pool_total_count,
    coalesce(pc.pool_ready_count, 0) AS pool_ready_count,
    coalesce(pc.pool_allocated_count, 0) AS pool_allocated_count,
    coalesce(pc.pool_failed_count, 0) AS pool_failed_count,
    CASE WHEN r.expected_count = coalesce(ic.item_count, 0) THEN 'PASS' ELSE 'FAIL' END AS expected_equals_items,
    CASE WHEN r.success_count = coalesce(ins.instance_count, 0) THEN 'PASS' ELSE 'FAIL' END AS success_equals_instances,
    CASE WHEN coalesce(ins.instance_count, 0) = coalesce(ins.instance_with_pool_count, 0) THEN 'PASS' ELSE 'FAIL' END AS instances_all_in_pool,
    CASE WHEN coalesce(pc.pool_total_count, 0) = coalesce(ins.instance_count, 0) THEN 'PASS' ELSE 'FAIL' END AS pool_total_equals_instances
FROM target_requirement r
LEFT JOIN item_counter ic ON ic.requirement_id = r.id
LEFT JOIN instance_counter ins ON ins.requirement_id = r.id
LEFT JOIN pool_counter pc ON pc.requirement_id = r.id;

-- 03. 输出逐条需求明细到数据实例的证据链。
WITH target_requirement AS (
    SELECT r.*
    FROM data_requirement r
    WHERE r.tenant_id = :'tenant_id'
      AND r.deleted = false
      AND (
          NULLIF(:'requirement_id', '') IS NOT NULL
          AND r.id = NULLIF(:'requirement_id', '')
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM data_requirement_item ri
                  WHERE ri.requirement_id = r.id
                    AND ri.request_batch_id = NULLIF(:'request_batch_id', '')
                    AND ri.deleted = false
              )
          )
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NULL
          )
      )
    ORDER BY r.create_time DESC
    LIMIT 1
)
SELECT
    '03_ITEM_INSTANCE_TRACE' AS check_name,
    ri.requirement_id,
    ri.id AS requirement_item_id,
    ri.request_batch_id,
    ri.request_item_id,
    ri.student_id,
    ri.question_id,
    ri.business_module_id,
    ri.module_code,
    ri.template_id,
    ri.scene_type,
    ri.required_external_org_id,
    ri.required_external_role_id,
    ri.init_external_status,
    ri.target_external_status,
    ri.item_status,
    ri.validation_status AS item_validation_status,
    ri.external_business_id AS item_external_business_id,
    tdp.id AS pool_id,
    tdp.pool_status,
    tdi.id AS data_instance_id,
    tdi.external_business_id AS instance_external_business_id,
    tdi.external_status AS instance_external_status,
    tdi.instance_status,
    tdi.validation_status AS instance_validation_status,
    tdi.owner_user_id,
    tdi.attempt_id,
    CASE WHEN tdi.id IS NULL AND ri.item_status = 'READY' THEN 'FAIL' ELSE 'PASS' END AS ready_item_has_instance,
    CASE WHEN tdi.id IS NOT NULL AND tdi.pool_id = tdp.id THEN 'PASS' ELSE 'FAIL' END AS instance_pool_match,
    CASE WHEN tdi.id IS NULL OR tdi.module_code = ri.module_code THEN 'PASS' ELSE 'FAIL' END AS module_code_match
FROM data_requirement_item ri
JOIN target_requirement r ON r.id = ri.requirement_id
LEFT JOIN teaching_data_instance tdi
       ON tdi.requirement_item_id = ri.id
      AND tdi.tenant_id = ri.tenant_id
      AND tdi.deleted = false
LEFT JOIN teaching_data_pool tdp
       ON tdp.id = tdi.pool_id
      AND tdp.tenant_id = ri.tenant_id
      AND tdp.deleted = false
WHERE ri.deleted = false
ORDER BY ri.question_id NULLS LAST, ri.student_id NULLS LAST, ri.request_item_id;

-- 04. 验证每个数据池的落库计数与实例真实状态是否一致。
WITH target_requirement AS (
    SELECT r.*
    FROM data_requirement r
    WHERE r.tenant_id = :'tenant_id'
      AND r.deleted = false
      AND (
          NULLIF(:'requirement_id', '') IS NOT NULL
          AND r.id = NULLIF(:'requirement_id', '')
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM data_requirement_item ri
                  WHERE ri.requirement_id = r.id
                    AND ri.request_batch_id = NULLIF(:'request_batch_id', '')
                    AND ri.deleted = false
              )
          )
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NULL
          )
      )
    ORDER BY r.create_time DESC
    LIMIT 1
),
real_counter AS (
    SELECT
        tdi.pool_id,
        count(*) AS real_total_count,
        count(*) FILTER (WHERE tdi.instance_status = 'READY' AND tdi.validation_status = 'PASSED') AS real_ready_count,
        count(*) FILTER (WHERE tdi.instance_status = 'ALLOCATED') AS real_allocated_count,
        count(*) FILTER (WHERE tdi.instance_status = 'FAILED') AS real_failed_count
    FROM teaching_data_instance tdi
    JOIN target_requirement r ON r.id = tdi.requirement_id
    WHERE tdi.deleted = false
      AND tdi.pool_id IS NOT NULL
    GROUP BY tdi.pool_id
)
SELECT
    '04_POOL_RECONCILIATION' AS check_name,
    tdp.id AS pool_id,
    tdp.requirement_id,
    tdp.module_code,
    tdp.scene_type,
    tdp.question_id,
    tdp.pool_status,
    tdp.total_count,
    tdp.ready_count,
    tdp.allocated_count,
    tdp.failed_count,
    coalesce(rc.real_total_count, 0) AS real_total_count,
    coalesce(rc.real_ready_count, 0) AS real_ready_count,
    coalesce(rc.real_allocated_count, 0) AS real_allocated_count,
    coalesce(rc.real_failed_count, 0) AS real_failed_count,
    CASE WHEN tdp.total_count = coalesce(rc.real_total_count, 0) THEN 'PASS' ELSE 'FAIL' END AS total_count_match,
    CASE WHEN tdp.ready_count = coalesce(rc.real_ready_count, 0) THEN 'PASS' ELSE 'FAIL' END AS ready_count_match,
    CASE WHEN tdp.allocated_count = coalesce(rc.real_allocated_count, 0) THEN 'PASS' ELSE 'FAIL' END AS allocated_count_match,
    CASE WHEN tdp.failed_count = coalesce(rc.real_failed_count, 0) THEN 'PASS' ELSE 'FAIL' END AS failed_count_match
FROM teaching_data_pool tdp
JOIN target_requirement r ON r.id = tdp.requirement_id
LEFT JOIN real_counter rc ON rc.pool_id = tdp.id
WHERE tdp.deleted = false
ORDER BY tdp.question_id NULLS LAST, tdp.create_time;

-- 05. 异常清单。正常情况下本查询应该返回 0 行。
WITH target_requirement AS (
    SELECT r.*
    FROM data_requirement r
    WHERE r.tenant_id = :'tenant_id'
      AND r.deleted = false
      AND (
          NULLIF(:'requirement_id', '') IS NOT NULL
          AND r.id = NULLIF(:'requirement_id', '')
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NOT NULL
              AND EXISTS (
                  SELECT 1
                  FROM data_requirement_item ri
                  WHERE ri.requirement_id = r.id
                    AND ri.request_batch_id = NULLIF(:'request_batch_id', '')
                    AND ri.deleted = false
              )
          )
          OR (
              NULLIF(:'requirement_id', '') IS NULL
              AND NULLIF(:'request_batch_id', '') IS NULL
          )
      )
    ORDER BY r.create_time DESC
    LIMIT 1
)
SELECT 'MISSING_CONNECTOR_SYSTEM' AS issue_code, r.id AS object_id, 'data_requirement' AS object_table, r.connector_system_id AS related_id
FROM target_requirement r
WHERE NOT EXISTS (
    SELECT 1 FROM connector_system cs
    WHERE cs.id = r.connector_system_id AND cs.tenant_id = r.tenant_id AND cs.deleted = false
)
UNION ALL
SELECT 'MISSING_BUSINESS_MODULE', r.id, 'data_requirement', r.business_module_id
FROM target_requirement r
WHERE NOT EXISTS (
    SELECT 1 FROM business_module bm
    WHERE bm.id = r.business_module_id
      AND bm.tenant_id = r.tenant_id
      AND bm.connector_system_id = r.connector_system_id
      AND bm.module_code = r.module_code
      AND bm.deleted = false
)
UNION ALL
SELECT 'MISSING_OR_MISMATCHED_STRATEGY', r.id, 'data_requirement', r.strategy_id
FROM target_requirement r
WHERE NOT EXISTS (
    SELECT 1 FROM module_data_strategy mds
    WHERE mds.id = r.strategy_id
      AND mds.tenant_id = r.tenant_id
      AND mds.connector_system_id = r.connector_system_id
      AND mds.business_module_id = r.business_module_id
      AND mds.module_code = r.module_code
      AND mds.scene_type = r.scene_type
      AND mds.template_id = r.template_id
      AND mds.deleted = false
)
UNION ALL
SELECT 'MISSING_TEMPLATE', r.id, 'data_requirement', r.template_id
FROM target_requirement r
WHERE NOT EXISTS (
    SELECT 1 FROM teaching_data_template tdt
    WHERE tdt.id = r.template_id
      AND tdt.tenant_id = r.tenant_id
      AND tdt.connector_system_id = r.connector_system_id
      AND tdt.deleted = false
)
UNION ALL
SELECT 'READY_ITEM_WITHOUT_INSTANCE', ri.id, 'data_requirement_item', ri.requirement_id
FROM data_requirement_item ri
JOIN target_requirement r ON r.id = ri.requirement_id
WHERE ri.deleted = false
  AND ri.item_status = 'READY'
  AND NOT EXISTS (
      SELECT 1 FROM teaching_data_instance tdi
      WHERE tdi.requirement_item_id = ri.id
        AND tdi.tenant_id = ri.tenant_id
        AND tdi.deleted = false
  )
UNION ALL
SELECT 'INSTANCE_WITHOUT_POOL', tdi.id, 'teaching_data_instance', tdi.requirement_id
FROM teaching_data_instance tdi
JOIN target_requirement r ON r.id = tdi.requirement_id
WHERE tdi.deleted = false
  AND tdi.pool_id IS NULL
UNION ALL
SELECT 'INSTANCE_POOL_MISMATCH', tdi.id, 'teaching_data_instance', tdi.pool_id
FROM teaching_data_instance tdi
JOIN target_requirement r ON r.id = tdi.requirement_id
LEFT JOIN teaching_data_pool tdp
       ON tdp.id = tdi.pool_id
      AND tdp.tenant_id = tdi.tenant_id
      AND tdp.requirement_id = tdi.requirement_id
      AND tdp.module_code = tdi.module_code
      AND tdp.deleted = false
WHERE tdi.deleted = false
  AND tdi.pool_id IS NOT NULL
  AND tdp.id IS NULL
UNION ALL
SELECT 'DUPLICATED_ACTIVE_ALLOCATION', dia.data_instance_id, 'data_instance_allocation', dia.pool_id
FROM data_instance_allocation dia
JOIN teaching_data_instance tdi
  ON tdi.id = dia.data_instance_id
 AND tdi.tenant_id = dia.tenant_id
JOIN target_requirement r ON r.id = tdi.requirement_id
WHERE dia.deleted = false
  AND dia.allocation_status = 'ALLOCATED'
GROUP BY dia.data_instance_id, dia.pool_id
HAVING count(*) > 1
ORDER BY issue_code, object_table, object_id;
