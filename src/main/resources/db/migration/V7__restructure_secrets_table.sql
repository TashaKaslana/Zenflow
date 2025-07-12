DROP TABLE IF EXISTS secrets;

CREATE TABLE IF NOT EXISTS secrets
(
    id              UUID PRIMARY KEY           DEFAULT gen_random_uuid(),

    user_id         UUID              NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_id      UUID REFERENCES projects (id) ON DELETE CASCADE,
    workflow_id     UUID REFERENCES workflows (id) ON DELETE CASCADE,

    scope           secret_scope_enum NOT NULL, -- 'global', 'project', 'workflow'

    group_name      TEXT              NOT NULL, -- e.g., 'google'
    key             TEXT              NOT NULL, -- e.g., 'api_key'
    encrypted_value TEXT              NOT NULL,

    description     TEXT,
    tags            TEXT[],

    version         INT                        DEFAULT 1,
    is_active       BOOLEAN                    DEFAULT TRUE,

    created_at      TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ       NOT NULL DEFAULT NOW(),
    updated_by      UUID,
    deleted_at      TIMESTAMPTZ,

    UNIQUE (scope, project_id, workflow_id, group_name, key)
);
COMMENT ON TABLE secrets IS 'Stores sensitive data like API keys and tokens.';
COMMENT ON COLUMN secrets.encrypted_value IS 'The secret value, which should be application-level encrypted.';
COMMENT ON COLUMN secrets.scope IS 'Defines visibility: global, project-specific, or workflow-specific.';
COMMENT ON COLUMN secrets.deleted_at IS 'Timestamp for when the secret was soft-deleted.';