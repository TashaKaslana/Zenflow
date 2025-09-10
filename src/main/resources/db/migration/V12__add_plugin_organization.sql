ALTER TABLE plugins
    ADD COLUMN organization TEXT;

COMMENT ON COLUMN plugins.organization IS 'Organization key for categorizing plugins (e.g., google, discord).';

ALTER TABLE plugins
    ADD COLUMN plugin_schema JSONB;