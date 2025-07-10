CREATE TYPE system_log_type_enum AS ENUM (
    'info', 'warning', 'error', 'startup', 'schedule', 'plugin', 'other'
    );

CREATE TABLE IF NOT EXISTS system_logs (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           log_type system_log_type_enum NOT NULL,
                                           message TEXT NOT NULL,
                                           context JSONB, -- optional: details like stacktrace, plugin info, etc.
                                           created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_system_logs_type ON system_logs(log_type);