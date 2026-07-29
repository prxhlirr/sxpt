-- Align the practice_step_result table with PracticeStepResult.java.
--
-- The original V1 table used step_status / hint_used / rollback_used, while
-- the current practice scoring service persists result_status, hint_count,
-- retry_count and execution evidence. Keep legacy columns for compatibility
-- and add the runtime columns idempotently for existing databases.

ALTER TABLE practice_step_result
    ADD COLUMN IF NOT EXISTS execution_id varchar(64),
    ADD COLUMN IF NOT EXISTS step_code varchar(64),
    ADD COLUMN IF NOT EXISTS result_status varchar(32),
    ADD COLUMN IF NOT EXISTS pass_flag boolean,
    ADD COLUMN IF NOT EXISTS hint_count bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS retry_count bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS evidence_json jsonb,
    ADD COLUMN IF NOT EXISTS feedback text;

UPDATE practice_step_result
SET result_status = step_status
WHERE result_status IS NULL
  AND step_status IS NOT NULL;

ALTER TABLE practice_step_result
    ALTER COLUMN step_status DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_practice_step_result_execution
    ON practice_step_result (tenant_id, execution_id)
    WHERE deleted = false;

-- Entity timestamps use LocalDateTime. PostgreSQL JDBC 42.2 cannot read a
-- timestamptz column directly as LocalDateTime, so normalize legacy V1/V2
-- timestamp columns while preserving their local wall-clock values.
DO $$
DECLARE
    timestamp_column record;
BEGIN
    FOR timestamp_column IN
        SELECT table_schema, table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND data_type = 'timestamp with time zone'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN %I TYPE timestamp without time zone USING %I AT TIME ZONE current_setting(''TIMEZONE'')',
            timestamp_column.table_schema,
            timestamp_column.table_name,
            timestamp_column.column_name,
            timestamp_column.column_name
        );
    END LOOP;
END
$$;
