WITH core_plugin AS (SELECT id
                     FROM plugins
                     WHERE name = 'core'
                     LIMIT 1)
INSERT
INTO plugin_nodes (plugin_id, key, name, type, plugin_node_version, description, tags, icon, config_schema)
VALUES
-- Email Node
((SELECT id FROM core_plugin),
 'email_node',
 'Email Node',
 'email',
 '1.0.0',
 'A node for sending emails',
 ARRAY [
     'email',
     'notification',
     'communication'
     ],
 'email_node_icon',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "title": "Plugin Node Configuration Schema",
   "properties": {
     "input": {
       "type": "object",
       "title": "Input Schema",
       "properties": {
         "host": {
           "type": "string",
           "description": "SMTP server host (e.g., smtp.gmail.com)"
         },
         "port": {
           "type": "integer",
           "description": "SMTP server port",
           "default": 587
         },
         "to": {
           "type": "string",
           "format": "email",
           "description": "Recipient email address"
         },
         "subject": {
           "type": "string",
           "description": "Subject of the email"
         },
         "body": {
           "type": "string",
           "description": "Body content of the email"
         },
         "username": {
           "type": "string",
           "description": "SMTP username (injected from secrets)"
         },
         "password": {
           "type": "string",
           "description": "SMTP password (injected from secrets)"
         }
       },
       "required": [
         "host",
         "to",
         "subject",
         "body"
       ],
       "additionalProperties": false
     },
     "output": {
       "type": "object",
       "title": "Output Schema"
     },
     "secrets": {
       "type": "array",
       "title": "Secrets",
       "description": "Secrets to inject as input fields before execution",
       "minItems": 2,
       "items": {
         "type": "object",
         "title": "Secret Definition",
         "properties": {
           "key": {
             "type": "string",
             "title": "Secret Key",
             "description": "Environment key of the secret.",
             "pattern": "^[A-Z0-9_]+$",
             "minLength": 3,
             "maxLength": 64
           },
           "required": {
             "type": "boolean",
             "title": "Is Required",
             "default": true,
             "description": "Whether this secret must be provided."
           },
           "description": {
             "type": "string",
             "title": "Secret Description",
             "description": "Explanation of what this secret is used for."
           }
         },
         "required": [
           "key"
         ]
       },
       "uniqueItems": true
     }
   },
   "required": [
     "input",
     "output",
     "secrets"
   ],
   "additionalProperties": false,
   "allOf": [
     {
       "properties": {
         "secrets": {
           "contains": {
             "type": "object",
             "properties": {
               "key": {
                 "const": "USERNAME"
               }
             },
             "required": [
               "key"
             ]
           }
         }
       }
     },
     {
       "properties": {
         "secrets": {
           "contains": {
             "type": "object",
             "properties": {
               "key": {
                 "const": "PASSWORD"
               }
             },
             "required": [
               "key"
             ]
           }
         }
       }
     }
   ]
 }
 '),

-- Merge Data Node
((SELECT id FROM core_plugin),
 'merge_data_node',
 'Merge Data Node',
 'data_processing',
 '1.0.0',
 'A node for merging multiple data sources with various strategies and conflict resolution options',
 ARRAY [
     'data',
     'merge',
     'processing',
     'transformation',
     'aggregation'
     ],
 'merge_data_node_icon',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "title": "Merge Data Node Configuration Schema",
   "properties": {
     "input": {
       "type": "object",
       "title": "Input Schema",
       "properties": {
         "sources": {
           "type": "array",
           "title": "Data Sources",
           "description": "Array of data sources to merge",
           "items": {
             "type": "object",
             "properties": {
               "data": {
                 "title": "Source Data",
                 "description": "The actual data to merge (any type: object, array, primitive)"
               },
               "name": {
                 "type": "string",
                 "description": "Optional name for this data source"
               },
               "priority": {
                 "type": "integer",
                 "description": "Priority for conflict resolution (higher = priority)",
                 "default": 0
               }
             },
             "required": ["data"]
           },
           "minItems": 1
         },
         "strategy": {
           "type": "string",
           "title": "Merge Strategy",
           "description": "Strategy for merging the data sources",
           "enum": [
             "concat",
             "concatenate",
             "join",
             "deep_merge",
             "deepmerge",
             "deep",
             "recursive",
             "shallow_merge",
             "shallowmerge",
             "shallow",
             "collect",
             "gather",
             "list",
             "overwrite",
             "last",
             "override",
             "replace"
           ],
           "default": "deep_merge"
         },
         "preserve_order": {
           "type": "boolean",
           "title": "Preserve Order",
           "description": "Whether to preserve insertion order in merged objects",
           "default": true
         },
         "ignore_nulls": {
           "type": "boolean",
           "title": "Ignore Nulls",
           "description": "Whether to ignore null values during merge",
           "default": false
         },
         "conflict_resolution": {
           "type": "string",
           "title": "Conflict Resolution",
           "description": "How to handle key conflicts during merge",
           "enum": [
             "keep_first",
             "first",
             "original",
             "keep_last",
             "last",
             "override",
             "merge_recursive",
             "recursive",
             "merge",
             "combine_arrays",
             "combine",
             "concat_arrays"
           ],
           "default": "keep_last"
         },
         "max_depth": {
           "type": "integer",
           "title": "Maximum Depth",
           "description": "Maximum recursion depth for deep merge operations",
           "minimum": 1,
           "maximum": 50,
           "default": 10
         },
         "data": {
           "title": "Direct Data",
           "description": "Alternative: provide data directly (when not using sources array)"
         },
         "items": {
           "title": "Items Data",
           "description": "Alternative data field name"
         },
         "values": {
           "title": "Values Data",
           "description": "Alternative data field name"
         },
         "content": {
           "title": "Content Data",
           "description": "Alternative data field name"
         },
         "payload": {
           "title": "Payload Data",
           "description": "Alternative data field name"
         }
       },
       "anyOf": [
         {
           "required": ["sources"]
         },
         {
           "anyOf": [
             {"required": ["data"]},
             {"required": ["items"]},
             {"required": ["values"]},
             {"required": ["content"]},
             {"required": ["payload"]}
           ]
         }
       ],
       "additionalProperties": true
     },
     "output": {
       "type": "object",
       "title": "Output Schema",
       "properties": {
         "data": {
           "title": "Merged Data",
           "description": "The result of the merge operation"
         },
         "strategy_used": {
           "type": "string",
           "description": "The merge strategy that was applied"
         },
         "sources_processed": {
           "type": "integer",
           "description": "Number of data sources that were processed"
         },
         "merge_timestamp": {
           "type": "integer",
           "description": "Unix timestamp when merge was completed"
         },
         "metadata": {
           "type": "object",
           "properties": {
             "source_count": {
               "type": "integer",
               "description": "Total number of sources"
             },
             "data_size": {
               "type": "integer",
               "description": "Size of the merged data (count of items/fields)"
             },
             "options": {
               "type": "object",
               "description": "Merge options that were used"
             }
           }
         }
       },
       "required": ["data", "strategy_used", "sources_processed"]
     }
   },
   "required": [
     "input",
     "output"
   ],
   "additionalProperties": false
 }
 ')
ON CONFLICT DO NOTHING;