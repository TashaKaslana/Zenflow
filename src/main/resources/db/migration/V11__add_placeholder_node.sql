WITH core_plugin AS (
    SELECT id FROM plugins WHERE key = 'core' LIMIT 1
)
INSERT INTO plugin_nodes (plugin_id, key, name, type, plugin_node_version, description, tags, icon, config_schema)
VALUES (
    (SELECT id FROM core_plugin),
    'data.placeholder',
    'Placeholder Node',
    'data',
    '1.0.0',
    'A placeholder node that echoes its input as output.',
    ARRAY['data','placeholder'],
    'ph:placeholder',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "properties": {
        "input": { "type": "object", "additionalProperties": true },
        "output": { "type": "object", "additionalProperties": true }
      },
      "required": ["input", "output"],
      "additionalProperties": false
    }'
);
