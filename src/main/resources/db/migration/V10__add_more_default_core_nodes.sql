WITH core_plugin AS (SELECT id
                     FROM plugins
                     WHERE name = 'core'
                     LIMIT 1)
INSERT
INTO plugin_nodes (plugin_id, key, name, type, plugin_node_version, description, tags, icon, config_schema)
VALUES
-- Email Node
((SELECT id FROM core_plugin),
 'email',
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
 'merge_data',
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
 '),

-- Wait Node
((SELECT id FROM core_plugin),
 'wait',
 'Wait Node',
 'flow_control',
 '1.0.0',
 'A node that waits for other nodes to complete based on a defined condition.',
 ARRAY['wait', 'flow', 'control', 'synchronization'],
 'wait_node_icon',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "title": "Wait Node Configuration Schema",
   "properties": {
     "input": {
       "type": "object",
       "title": "Input Schema",
       "properties": {
         "mode": {
           "type": "string",
           "enum": ["all", "any", "threshold"],
           "default": "any",
           "description": "The condition to wait for. ''all'' waits for all nodes, ''any'' waits for at least one, ''threshold'' waits for a specific number of nodes."
         },
         "threshold": {
           "type": "integer",
           "description": "The number of nodes that must be complete when mode is ''threshold''.",
           "default": 1
         },
         "waitingNodes": {
           "type": "object",
           "description": "A map where keys are node IDs and values are their completion status (boolean).",
           "additionalProperties": {
             "type": "boolean"
           }
         }
       },
       "required": ["waitingNodes"],
       "if": {
         "properties": { "mode": { "const": "threshold" } }
       },
       "then": {
         "required": ["threshold"]
       }
     },
     "output": {
       "type": "object",
       "title": "Output Schema",
       "properties": {
         "waitingNodes": {
           "type": "object",
           "additionalProperties": { "type": "boolean" }
         },
         "mode": {
           "type": "string"
         },
         "threshold": {
           "type": "integer"
         },
         "isReady": {
           "type": "boolean"
         }
       },
       "required": ["waitingNodes", "mode", "threshold", "isReady"]
     }
   },
   "required": ["input", "output"],
   "additionalProperties": false
 }'),

-- PostgreSQL Node
((SELECT id FROM core_plugin),
 'postgresql',
 'PostgreSQL Database Node',
 'database',
 '1.0.0',
 'A node for executing PostgreSQL queries with advanced parameter binding and result processing',
 ARRAY [
     'database',
     'postgresql',
     'postgres',
     'sql',
     'query'
     ],
 'postgresql_node_icon',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "title": "PostgreSQL Node Configuration Schema",
   "properties": {
     "input": {
       "type": "object",
       "title": "Input Schema",
       "properties": {
         "host": {
           "type": "string",
           "description": "PostgreSQL server hostname or IP address",
           "default": "localhost"
         },
         "port": {
           "type": "integer",
           "description": "PostgreSQL server port",
           "default": 5432,
           "minimum": 1,
           "maximum": 65535
         },
         "database": {
           "type": "string",
           "description": "Database name to connect to"
         },
         "username": {
           "type": "string",
           "description": "Database username (injected from secrets)"
         },
         "password": {
           "type": "string",
           "description": "Database password (injected from secrets)"
         },
         "query": {
           "type": "string",
           "description": "SQL query to execute (supports parameterized queries with ? placeholders)"
         },
         "parameters": {
           "type": "array",
           "description": "Indexed parameters for parameterized queries",
           "items": {
             "type": "object",
             "properties": {
               "index": {
                 "type": "integer",
                 "description": "1-based parameter index",
                 "minimum": 1
               },
               "type": {
                 "type": "string",
                 "description": "PostgreSQL parameter type",
                 "enum": [
                   "string",
                   "int",
                   "long",
                   "boolean",
                   "numeric",
                   "date",
                   "timestamp",
                   "uuid",
                   "jsonb",
                   "array",
                   "bytea"
                 ]
               },
               "value": {
                 "description": "Parameter value (type depends on the type field)"
               }
             },
             "required": [
               "index",
               "type",
               "value"
             ]
           }
         },
         "values": {
           "type": "array",
           "description": "Simple parameter values array (types will be auto-inferred)",
           "items": {}
         },
         "batchValues": {
              "type": "array",
              "description": "Batch parameter values for multi-row inserts/updates",
              "items": {
                 "type": "array",
                 "description": "Array of values for each row",
                 "items": {}
              }
         },
         "schema": {
           "type": "string",
           "description": "Database schema name for table qualification"
         },
         "conflictColumns": {
           "type": "string",
           "description": "Column names for UPSERT conflict resolution"
         },
         "updateAction": {
           "type": "string",
           "description": "Action to take on conflict (e.g., UPDATE SET column = EXCLUDED.column)"
         },
         "timeout": {
           "type": "integer",
           "description": "Query timeout in seconds",
           "default": 30,
           "minimum": 1,
           "maximum": 3600
         },
         "maxRows": {
           "type": "integer",
           "description": "Maximum number of rows to return",
           "default": 1000,
           "minimum": 1
         }
       },
       "required": [
         "host",
         "database",
         "username",
         "password",
         "query"
       ],
       "additionalProperties": false
     },
     "output": {
       "type": "object",
       "title": "Output Schema",
       "properties": {
         "rows": {
           "type": "array",
           "description": "Query result rows",
           "items": {
             "type": "object"
           }
         },
         "rowCount": {
           "type": "integer",
           "description": "Number of rows returned or affected"
         },
         "columns": {
           "type": "array",
           "description": "Column metadata",
           "items": {
             "type": "object",
             "properties": {
               "name": {
                 "type": "string"
               },
               "type": {
                 "type": "string"
               },
               "nullable": {
                 "type": "boolean"
               }
             }
           }
         },
         "executionTime": {
           "type": "integer",
           "description": "Query execution time in milliseconds"
         },
         "queryType": {
           "type": "string",
           "description": "Type of query executed (SELECT, INSERT, UPDATE, DELETE, etc.)"
         }
       },
       "required": [
         "rows",
         "rowCount",
         "executionTime",
         "queryType"
       ]
     },
     "secrets": {
       "type": "array",
       "title": "Secrets",
       "description": "Database credentials to inject as input fields",
       "minItems": 2,
       "items": {
         "type": "object",
         "title": "Secret Definition",
         "properties": {
           "key": {
             "type": "string",
             "title": "Secret Key",
             "description": "Environment key of the secret",
             "pattern": "^[A-Z0-9_]+$",
             "minLength": 3,
             "maxLength": 64
           },
           "required": {
             "type": "boolean",
             "title": "Is Required",
             "default": true,
             "description": "Whether this secret must be provided"
           },
           "description": {
             "type": "string",
             "title": "Secret Description",
             "description": "Explanation of what this secret is used for"
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
                 "const": "DB_USERNAME"
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
                 "const": "DB_PASSWORD"
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
 }'
)
ON CONFLICT DO NOTHING;