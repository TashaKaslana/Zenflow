WITH core_plugin AS (
    SELECT id FROM plugins WHERE key = 'core' LIMIT 1
)
INSERT INTO plugin_nodes (
    plugin_id,
    key,
    name,
    type,
    plugin_node_version,
    description,
    tags,
    icon,
    config_schema
) VALUES (
    (SELECT id FROM core_plugin),
    'data.generate',
    'Data Generator',
    'data_generator',
    '1.0.0',
    'Generates mock user information',
    ARRAY['data', 'test', 'generator'],
    'ph:database',
    '{
      "$schema": "http://json-schema.org/draft-07/schema#",
      "type": "object",
      "properties": {
        "input": {
          "type": "object",
          "properties": {
            "seed": {"type": "integer", "description": "Seed for deterministic output"},
            "format": {"type": "string", "description": "Optional format template"}
          },
          "additionalProperties": false
        },
        "output": {
          "type": "object",
          "properties": {
            "user_email": {"type": "string"},
            "user_age": {"type": "integer"},
            "user_active": {"type": "boolean"}
          },
          "additionalProperties": true
        }
      },
      "required": ["input", "output"],
      "additionalProperties": false
    }'::jsonb
) ON CONFLICT DO NOTHING;
