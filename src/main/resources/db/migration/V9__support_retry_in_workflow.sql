ALTER TABLE workflows
    ADD COLUMN retry_policy JSONB,
    ADD COLUMN description TEXT;

ALTER TABLE workflow_runs
    ADD COLUMN retry_of UUID REFERENCES workflow_runs(id) ON DELETE SET NULL,
    ADD COLUMN retry_attempt INT DEFAULT 0,
    ADD COLUMN next_retry_at TIMESTAMPTZ;

ALTER TABLE node_logs
    ALTER COLUMN status TYPE TEXT,
    ADD COLUMN logs JSONB;
