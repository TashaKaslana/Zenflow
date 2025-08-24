-- Rename node_logs to node_executions
ALTER TABLE node_logs RENAME TO node_executions;

-- Remove the logs column from node_executions
ALTER TABLE node_executions DROP COLUMN IF EXISTS logs;

-- Create the new node_logs table
CREATE TABLE IF NOT EXISTS node_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    workflow_run_id UUID NOT NULL,
    node_key TEXT NOT NULL,
    "timestamp" TIMESTAMPTZ NOT NULL,
    level TEXT NOT NULL,
    message TEXT,
    error_code TEXT,
    error_message TEXT,
    meta JSONB,
    trace_id TEXT,
    hierarchy TEXT,
    user_id UUID,
    correlation_id TEXT,
    FOREIGN KEY (workflow_run_id) REFERENCES workflow_runs(id) ON DELETE CASCADE
);

CREATE INDEX idx_node_log_workflow_run_id ON node_logs(workflow_run_id);
CREATE INDEX idx_node_log_node_key ON node_logs(node_key);
