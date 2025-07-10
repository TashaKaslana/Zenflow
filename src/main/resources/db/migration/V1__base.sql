-- To ensure a clean slate, you can optionally drop existing objects.
-- It's recommended to run these drops in reverse order of creation to avoid dependency errors.
-- DROP TABLE IF EXISTS user_settings, audit_logs, notifications, trigger_logs, node_logs, workflow_runs, workflow_triggers, secrets, plugin_nodes, plugins, workflow_versions, workflows, projects, role_permissions, permissions, roles, users CASCADE;
-- DROP TYPE IF EXISTS workflow_status_enum, trigger_type_enum, trigger_log_status_enum, secret_scope_enum, notification_type_enum, log_status_enum, user_role_enum CASCADE;

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =================================================================
-- ENUM TYPES
-- Using ENUMs for status fields improves data integrity and performance.
-- =================================================================

CREATE TYPE workflow_status_enum AS ENUM ('RUNNING', 'SUCCESS', 'ERROR', 'WAITING');
CREATE TYPE trigger_type_enum AS ENUM ('MANUAL', 'SCHEDULE', 'WEBHOOK', 'EVENT');
CREATE TYPE trigger_log_status_enum AS ENUM ('SUCCESS', 'ERROR', 'RUNNING');
CREATE TYPE secret_scope_enum AS ENUM ('GLOBAL', 'PROJECT', 'WORKFLOW');
CREATE TYPE notification_type_enum AS ENUM ('INFO', 'SUCCESS', 'ERROR', 'WARNING');
CREATE TYPE log_status_enum AS ENUM ('RUNNING', 'SUCCESS', 'ERROR');
CREATE TYPE user_role_enum AS ENUM ('OWNER', 'ADMIN', 'USER');

-- =================================================================
-- PERMISSIONS & ROLES
-- These tables manage a flexible role-based access control (RBAC) system.
-- =================================================================

CREATE TABLE IF NOT EXISTS roles (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     name TEXT NOT NULL UNIQUE,
                                     description TEXT,
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     created_by UUID, -- Can be NULL for system-defined roles
                                     updated_by UUID  -- Can be NULL for system-defined roles
);
COMMENT ON TABLE roles IS 'Stores user roles like owner, admin, user.';

CREATE TABLE IF NOT EXISTS permissions (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           feature TEXT NOT NULL, -- e.g., 'workflow', 'project', 'user_management'
                                           action TEXT NOT NULL,  -- e.g., 'create', 'read', 'update', 'delete'
                                           description TEXT,
                                           UNIQUE (feature, action)
);
COMMENT ON TABLE permissions IS 'Defines specific actions that can be performed on features.';

CREATE TABLE IF NOT EXISTS role_permissions (
                                                role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                                permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
                                                PRIMARY KEY (role_id, permission_id)
);
COMMENT ON TABLE role_permissions IS 'Maps permissions to roles.';

-- =================================================================
-- CORE TABLES
-- =================================================================

CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     username TEXT NOT NULL UNIQUE,
                                     email TEXT NOT NULL UNIQUE,
                                     password_hash TEXT NOT NULL,
                                     role_id UUID REFERENCES roles(id),
                                     created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                     deleted_at TIMESTAMPTZ -- For soft deletes
);
COMMENT ON TABLE users IS 'Stores user account information.';
COMMENT ON COLUMN users.role_id IS 'Foreign key linking to the user''s role.';
COMMENT ON COLUMN users.deleted_at IS 'Timestamp for when the user was soft-deleted.';

CREATE TABLE IF NOT EXISTS projects (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                        name TEXT NOT NULL,
                                        description TEXT,
                                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                        updated_by UUID,
                                        deleted_at TIMESTAMPTZ -- For soft deletes
);
COMMENT ON TABLE projects IS 'A workspace to organize related workflows.';
COMMENT ON COLUMN projects.user_id IS 'The user who owns the project.';
COMMENT ON COLUMN projects.deleted_at IS 'Timestamp for when the project was soft-deleted.';

CREATE TABLE IF NOT EXISTS workflows (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                         name TEXT NOT NULL,
                                         definition JSONB, -- The graph structure of nodes and connections
                                         start_node TEXT,   -- The entry point node ID within the definition
                                         is_active BOOLEAN NOT NULL DEFAULT FALSE,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                         updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                         created_by UUID,
                                         updated_by UUID,
                                         deleted_at TIMESTAMPTZ -- For soft deletes
);
COMMENT ON TABLE workflows IS 'Stores the definition and state of a workflow.';
COMMENT ON COLUMN workflows.definition IS 'JSONB representation of the workflow''s nodes and connections.';
COMMENT ON COLUMN workflows.is_active IS 'Controls if the workflow can be triggered automatically.';
COMMENT ON COLUMN workflows.deleted_at IS 'Timestamp for when the workflow was soft-deleted.';

CREATE TABLE IF NOT EXISTS workflow_versions (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                 workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
                                                 version INT NOT NULL,
                                                 definition JSONB,
                                                 is_autosave BOOLEAN NOT NULL DEFAULT FALSE,
                                                 created_by UUID, -- Can be NULL if system-generated
                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                                 UNIQUE (workflow_id, version)
);
COMMENT ON TABLE workflow_versions IS 'Immutable snapshots of a workflow''s definition for history and rollback.';

CREATE TABLE IF NOT EXISTS secrets (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
                                       workflow_id UUID REFERENCES workflows(id) ON DELETE CASCADE,
                                       name TEXT NOT NULL,
                                       value TEXT NOT NULL, -- This should be encrypted in the application layer before storing
                                       scope secret_scope_enum NOT NULL,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_by UUID,
                                       deleted_at TIMESTAMPTZ -- For soft deletes
);
COMMENT ON TABLE secrets IS 'Stores sensitive data like API keys and tokens.';
COMMENT ON COLUMN secrets.value IS 'The secret value, which should be application-level encrypted.';
COMMENT ON COLUMN secrets.scope IS 'Defines visibility: global, project-specific, or workflow-specific.';
COMMENT ON COLUMN secrets.deleted_at IS 'Timestamp for when the secret was soft-deleted.';

-- =================================================================
-- PLUGIN SYSTEM
-- =================================================================

CREATE TABLE IF NOT EXISTS plugins (
                                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       publisher_id UUID, -- The user/organization that published the plugin
                                       name TEXT NOT NULL UNIQUE,
                                       version TEXT NOT NULL,
                                       registry_url TEXT,
                                       verified BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE plugins IS 'Manages installed plugins.';

CREATE TABLE IF NOT EXISTS plugin_nodes (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            plugin_id UUID NOT NULL REFERENCES plugins(id) ON DELETE CASCADE,
                                            name TEXT NOT NULL, -- e.g., "HTTP Request", "Read Google Sheet"
                                            type TEXT NOT NULL, -- e.g., 'trigger', 'action'
                                            plugin_node_version TEXT, -- Version of the node, can differ from the plugin version
                                            config_schema JSONB, -- JSON Schema for the node's configuration fields
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                            updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE plugin_nodes IS 'Stores information about nodes provided by each plugin.';

-- =================================================================
-- EXECUTION & LOGGING
-- =================================================================

CREATE TABLE IF NOT EXISTS workflow_triggers (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                 workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
                                                 type trigger_type_enum NOT NULL,
                                                 config JSONB, -- e.g., cron string for 'schedule', webhook URL for 'webhook'
                                                 enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                                 last_triggered_at TIMESTAMPTZ,
                                                 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                                 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                                 created_by UUID,
                                                 updated_by UUID
);
COMMENT ON TABLE workflow_triggers IS 'Defines automatic triggers for workflows.';

CREATE TABLE IF NOT EXISTS workflow_runs (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
                                             status workflow_status_enum NOT NULL,
                                             error TEXT,
                                             trigger_type trigger_type_enum,
                                             started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                             ended_at TIMESTAMPTZ
);
COMMENT ON TABLE workflow_runs IS 'Logs each execution instance of a workflow.';

CREATE TABLE IF NOT EXISTS node_logs (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         workflow_run_id UUID NOT NULL REFERENCES workflow_runs(id) ON DELETE CASCADE,
                                         node_key TEXT NOT NULL, -- The unique key of the node within the workflow definition
                                         status log_status_enum NOT NULL,
                                         error TEXT,
                                         attempts INT DEFAULT 1,
                                         output JSONB,
                                         started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                         ended_at TIMESTAMPTZ
);
COMMENT ON TABLE node_logs IS 'Logs the result of each node execution within a workflow run.';

CREATE TABLE IF NOT EXISTS trigger_logs (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            trigger_id UUID NOT NULL REFERENCES workflow_triggers(id) ON DELETE CASCADE,
                                            workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
                                            status trigger_log_status_enum NOT NULL,
                                            error TEXT,
                                            input JSONB, -- Input data from the trigger (e.g., webhook body, event context)
                                            created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE trigger_logs IS 'Logs each time a trigger is activated.';

-- =================================================================
-- USER-FACING & AUDITING
-- =================================================================

CREATE TABLE IF NOT EXISTS notifications (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             workflow_id UUID REFERENCES workflows(id) ON DELETE SET NULL,
                                             type notification_type_enum NOT NULL,
                                             message TEXT NOT NULL,
                                             is_read BOOLEAN NOT NULL DEFAULT FALSE,
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE notifications IS 'Stores system-generated notifications for users.';

CREATE TABLE IF NOT EXISTS audit_logs (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          user_id UUID, -- User who performed the action
                                          action TEXT NOT NULL, -- e.g., 'workflow.create', 'secret.update'
                                          target_type TEXT,     -- e.g., 'project', 'workflow', 'user'
                                          target_id UUID,
                                          metadata JSONB,       -- Additional details about the action (e.g., old and new values)
                                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE audit_logs IS 'Tracks significant actions within the system for security and auditing.';

CREATE TABLE IF NOT EXISTS user_settings (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                             settings JSONB, -- e.g., { "theme": "dark", "language": "en" }
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                             updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE user_settings IS 'Stores user-specific preferences.';

-- =================================================================
-- INDEXES
-- Indexes are crucial for query performance, especially on foreign keys and frequently queried columns.
-- =================================================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);
CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_workflows_project_id ON workflows(project_id);
CREATE INDEX idx_workflow_versions_workflow_id ON workflow_versions(workflow_id);
CREATE INDEX idx_secrets_user_id ON secrets(user_id);
CREATE INDEX idx_secrets_project_id ON secrets(project_id);
CREATE INDEX idx_plugin_nodes_plugin_id ON plugin_nodes(plugin_id);
CREATE INDEX idx_workflow_triggers_workflow_id ON workflow_triggers(workflow_id);
CREATE INDEX idx_workflow_runs_workflow_id ON workflow_runs(workflow_id);
CREATE INDEX idx_node_logs_workflow_run_id ON node_logs(workflow_run_id);
CREATE INDEX idx_trigger_logs_trigger_id ON trigger_logs(trigger_id);
CREATE INDEX idx_trigger_logs_workflow_id ON trigger_logs(workflow_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);