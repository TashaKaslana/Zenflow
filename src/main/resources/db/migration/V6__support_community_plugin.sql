CREATE TYPE executor_type AS ENUM ('builtin', 'remote', 'container');

ALTER TABLE plugin_nodes ADD COLUMN executor_type executor_type NOT NULL DEFAULT 'builtin';
-- values: 'builtin' | 'remote' | 'container' (optional future)

ALTER TABLE plugin_nodes ADD COLUMN entrypoint TEXT;
-- builtin: entrypoint = class name / executor key
-- remote: entrypoint = http://ai.myplugin.com/run

ALTER TABLE plugin_nodes ADD COLUMN description TEXT;
ALTER TABLE plugin_nodes ADD COLUMN tags TEXT[];

ALTER TABLE plugins ADD COLUMN description TEXT;
ALTER TABLE plugins ADD COLUMN tags TEXT[];