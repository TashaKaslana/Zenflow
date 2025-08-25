ALTER TABLE workflow_triggers
    ADD COLUMN trigger_key TEXT; -- e.g. "discord_message"

COMMENT ON COLUMN workflow_triggers.trigger_key IS 'It store key of the plugin trigger';

ALTER TABLE plugin_nodes
    DROP COLUMN IF EXISTS key;

ALTER TABLE plugin_nodes
    ADD COLUMN composite_key TEXT UNIQUE;

CREATE INDEX idx_plugin_node_composite_key ON plugin_nodes(composite_key)