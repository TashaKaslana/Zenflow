ALTER TABLE workflows
    ADD COLUMN IF NOT EXISTS retry_policy JSONB,
    ADD COLUMN IF NOT EXISTS description  TEXT;

ALTER TABLE workflow_runs
    ADD COLUMN IF NOT EXISTS context       JSONB,
    ADD COLUMN IF NOT EXISTS retry_of      UUID REFERENCES workflow_runs (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS retry_attempt INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ;

ALTER TABLE node_logs
    ALTER COLUMN status TYPE TEXT,
    ADD COLUMN IF NOT EXISTS logs JSONB;

ALTER TABLE plugin_nodes
    ALTER COLUMN executor_type TYPE TEXT;

ALTER TABLE plugins
    ADD COLUMN IF NOT EXISTS key TEXT;

UPDATE plugins
SET key = 'core', publisher_id = '00000000-0000-0000-0000-000000000000'
WHERE id = (SELECT id
            FROM plugins
            WHERE name = 'core'
            LIMIT 1);

ALTER TABLE plugins
    ALTER COLUMN key SET NOT NULL;
ALTER TABLE plugins
    ADD CONSTRAINT unique_plugin_version_key UNIQUE (key, version);
