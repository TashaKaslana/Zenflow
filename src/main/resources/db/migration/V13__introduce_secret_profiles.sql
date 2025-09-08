CREATE TABLE IF NOT EXISTS secret_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    scope secret_scope_enum NOT NULL,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    project_id UUID REFERENCES projects (id) ON DELETE CASCADE,
    workflow_id UUID REFERENCES workflows (id) ON DELETE CASCADE,
    plugin_id UUID NOT NULL REFERENCES plugins (id) ON DELETE CASCADE,
    plugin_node_id UUID REFERENCES plugin_nodes (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT secret_profiles_scope_project_workflow_plugin_name_key UNIQUE (scope, project_id, workflow_id, plugin_id, name)
);

-- Populate profiles from existing secrets
INSERT INTO secret_profiles (name, scope, user_id, project_id, workflow_id, plugin_id, plugin_node_id)
SELECT DISTINCT s.group_name, s.scope, s.user_id, s.project_id, s.workflow_id, pn.plugin_id, s.plugin_node_id
FROM secrets s
LEFT JOIN plugin_nodes pn ON pn.id = s.plugin_node_id;

-- Clear node-specific links to favor plugin-level association
UPDATE secret_profiles SET plugin_node_id = NULL;

-- Add profile reference to secrets using existing groupings
ALTER TABLE secrets
    ADD COLUMN profile_id UUID;

UPDATE secrets s SET profile_id = p.id
FROM secret_profiles p
WHERE p.name = s.group_name AND p.scope = s.scope AND
      (p.project_id IS NOT DISTINCT FROM s.project_id) AND
      (p.workflow_id IS NOT DISTINCT FROM s.workflow_id) AND
      (p.user_id = s.user_id);

ALTER TABLE secrets
    DROP COLUMN group_name,
    DROP COLUMN plugin_node_id,
    ADD CONSTRAINT fk_secret_profile FOREIGN KEY (profile_id) REFERENCES secret_profiles(id) ON DELETE CASCADE;

ALTER TABLE secrets
    ALTER COLUMN profile_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS secrets_profile_key_unique ON secrets(profile_id, key);

