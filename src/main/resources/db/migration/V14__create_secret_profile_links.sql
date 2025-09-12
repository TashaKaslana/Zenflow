-- Create tables for new secret-profile and node-profile linking

-- Decouple secrets from profiles: drop FK, index and column if present
ALTER TABLE secrets DROP CONSTRAINT IF EXISTS fk_secret_profile;
DROP INDEX IF EXISTS secrets_profile_key_unique;
ALTER TABLE secrets DROP COLUMN IF EXISTS profile_id;

CREATE TABLE IF NOT EXISTS profile_secret_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    secret_id UUID NOT NULL,
    CONSTRAINT fk_psl_profile FOREIGN KEY (profile_id) REFERENCES secret_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_psl_secret FOREIGN KEY (secret_id) REFERENCES secrets(id) ON DELETE CASCADE,
    CONSTRAINT profile_secret_links_profile_secret_unique UNIQUE (profile_id, secret_id)
);
CREATE INDEX IF NOT EXISTS idx_psl_profile_id ON profile_secret_links(profile_id);
CREATE INDEX IF NOT EXISTS idx_psl_secret_id ON profile_secret_links(secret_id);

CREATE TABLE IF NOT EXISTS secret_profile_node_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    node_key TEXT NOT NULL,
    profile_id UUID NOT NULL,
    CONSTRAINT fk_spnl_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    CONSTRAINT fk_spnl_profile FOREIGN KEY (profile_id) REFERENCES secret_profiles(id) ON DELETE CASCADE,
    CONSTRAINT secret_profile_node_links_workflow_node_unique UNIQUE (workflow_id, node_key)
);
CREATE INDEX IF NOT EXISTS idx_spnl_workflow_node ON secret_profile_node_links(workflow_id, node_key);
CREATE INDEX IF NOT EXISTS idx_spnl_profile_id ON secret_profile_node_links(profile_id);

CREATE TABLE IF NOT EXISTS secret_node_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    node_key TEXT NOT NULL,
    secret_id UUID NOT NULL,
    CONSTRAINT fk_snl_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,
    CONSTRAINT fk_snl_secret FOREIGN KEY (secret_id) REFERENCES secrets(id) ON DELETE CASCADE,
    CONSTRAINT secret_node_links_workflow_node_secret_unique UNIQUE (workflow_id, node_key, secret_id)
);
CREATE INDEX IF NOT EXISTS idx_snl_workflow_node ON secret_node_links(workflow_id, node_key);
CREATE INDEX IF NOT EXISTS idx_snl_secret_id ON secret_node_links(secret_id);
