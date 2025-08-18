ALTER TABLE plugin_nodes
    ADD COLUMN IF NOT EXISTS icon TEXT,
    ADD COLUMN IF NOT EXISTS key TEXT UNIQUE;

ALTER TABLE plugins
    ADD COLUMN IF NOT EXISTS icon TEXT;
ALTER TABLE plugins
    ADD COLUMN IF NOT EXISTS key TEXT UNIQUE DEFAULT gen_random_uuid()::text;

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
    ALTER COLUMN key SET NOT NULL;
ALTER TABLE plugins
    ADD CONSTRAINT unique_plugin_version_key UNIQUE (key, version);
