-- Register discrete transformer nodes and extend data.transformer schema

WITH core_plugin AS (
    SELECT id FROM plugins WHERE key = 'core'
)
INSERT INTO plugin_nodes (plugin_id, key, name, type, plugin_node_version, description, tags, icon, config_schema)
VALUES
((SELECT id FROM core_plugin),
 'data.transformer.group_by',
 'Group By',
 'action',
 '1.0.0',
 'Groups records and applies aggregations.',
 ARRAY['data','group','aggregate'],
 'ph:columns',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "definitions": {
     "group_by_params": {
       "type": "object",
       "properties": {
         "groupBy": {
           "oneOf": [
             {"type": "string"},
             {"type": "array", "items": {"type": "string"}}
           ]
         },
         "aggregations": {
           "type": "array",
           "items": {
             "type": "object",
             "properties": {
               "field": {"type": "string"},
               "function": {"type": "string", "enum": ["count","sum","avg","min","max","first","last","concat","distinct_count","std_dev"]},
               "alias": {"type": "string"}
             },
             "required": ["field","function"]
           }
         }
       },
       "required": ["groupBy"]
     }
   },
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "data": {"type": "array"},
         "params": {"$ref": "#/definitions/group_by_params"}
       },
       "required": ["data","params"],
       "additionalProperties": false
     },
     "output": {"type": "object"}
   },
   "required": ["input"]
 }'::jsonb),
((SELECT id FROM core_plugin),
 'data.transformer.aggregate',
 'Aggregate',
 'action',
 '1.0.0',
 'Aggregates a list of records without grouping.',
 ARRAY['data','aggregate'],
 'ph:sigma',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "definitions": {
     "aggregate_params": {
       "type": "object",
       "properties": {
         "aggregations": {
           "type": "array",
           "items": {
             "type": "object",
             "properties": {
               "field": {"type": "string"},
               "function": {"type": "string", "enum": ["count","sum","avg","min","max","first","last","concat","distinct_count","std_dev"]},
               "alias": {"type": "string"}
             },
             "required": ["field","function"]
           }
         }
       },
       "required": ["aggregations"]
     }
   },
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "data": {"type": "array"},
         "params": {"$ref": "#/definitions/aggregate_params"}
       },
       "required": ["data","params"],
       "additionalProperties": false
     },
     "output": {"type": "object"}
   },
   "required": ["input"]
 }'::jsonb),
((SELECT id FROM core_plugin),
 'data.transformer.filter',
 'Filter',
 'action',
 '1.0.0',
 'Filters a list of records using an expression.',
 ARRAY['data','filter'],
 'ph:funnel',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "definitions": {
     "filter_params": {
       "type": "object",
       "properties": {
         "expression": {"type": "string"},
         "mode": {"type": "string", "enum": ["include","exclude"], "default": "include"}
       },
       "required": ["expression"]
     }
   },
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "data": {"type": "array"},
         "params": {"$ref": "#/definitions/filter_params"}
       },
       "required": ["data","params"],
       "additionalProperties": false
     },
     "output": {"type": "object"}
   },
   "required": ["input"]
 }'::jsonb)
ON CONFLICT (plugin_id, key, plugin_node_version) DO NOTHING;

-- Extend data.transformer schema to support aggregate in pipelines
WITH core_plugin AS (
    SELECT id FROM plugins WHERE key = 'core'
), updated AS (
    SELECT pn.id,
           jsonb_set(
             jsonb_set(
               pn.config_schema,
               '{definitions,aggregate_params}',
               '{
                 "type": "object",
                 "properties": {
                   "aggregations": {
                     "type": "array",
                     "items": {
                       "type": "object",
                       "properties": {
                         "field": {"type": "string"},
                         "function": {"type": "string", "enum": ["count","sum","avg","min","max","first","last","concat","distinct_count","std_dev"]},
                         "alias": {"type": "string"}
                       },
                       "required": ["field","function"]
                     }
                   }
                 },
                 "required": ["aggregations"]
               }'::jsonb,
               true
             ),
             '{definitions,transform_step,oneOf}',
             (
               (pn.config_schema #> '{definitions,transform_step,oneOf}') ||
               jsonb_build_array(
                 jsonb_build_object(
                   'properties',
                   jsonb_build_object(
                     'transformer', jsonb_build_object('const','aggregate'),
                     'params', jsonb_build_object('$ref','#/definitions/aggregate_params')
                   )
                 )
               )
             )
           ) AS new_schema
    FROM plugin_nodes pn
    WHERE pn.plugin_id = (SELECT id FROM core_plugin) AND pn.key = 'data.transformer'
)
UPDATE plugin_nodes pn
SET config_schema = updated.new_schema
FROM updated
WHERE pn.id = updated.id;
