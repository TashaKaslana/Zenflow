ALTER TABLE workflows
    ADD COLUMN IF NOT EXISTS last_validation              JSONB,
    ADD COLUMN IF NOT EXISTS last_validation_at           TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_validation_publish_attempt BOOLEAN;
