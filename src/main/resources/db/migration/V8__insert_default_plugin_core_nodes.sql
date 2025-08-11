ALTER TABLE plugin_nodes
    ADD COLUMN IF NOT EXISTS icon TEXT,
    ADD COLUMN IF NOT EXISTS key TEXT UNIQUE;

ALTER TABLE plugins
    ADD COLUMN IF NOT EXISTS icon TEXT;
ALTER TABLE plugins
    ADD COLUMN IF NOT EXISTS key TEXT UNIQUE DEFAULT gen_random_uuid()::text;

INSERT INTO plugins (id,
                     publisher_id,
                     key,
                     name,
                     version,
                     registry_url,
                     verified,
                     created_at,
                     updated_at,
                     description,
                     tags,
                     icon)
VALUES (gen_random_uuid(),
        '00000000-0000-0000-0000-000000000000',
        'core',
        'Core Plugin',
        '1.0.0',
        NULL,
        true,
        now(),
        now(),
        'Built-in core plugin containing essential nodes.',
        ARRAY ['core', 'builtin'],
        'ph:core')
ON CONFLICT (name) DO NOTHING;

-- Insert plugin nodes for core plugin
WITH core_plugin AS (
    SELECT id FROM plugins WHERE key = 'core'
)
INSERT
INTO plugin_nodes (plugin_id, key, name, type, plugin_node_version, description, tags, icon, config_schema)
VALUES
-- 1. HTTP Request
((SELECT id FROM core_plugin),
 'http.request',
 'HTTP Request',
 'action',
 '1.0.0',
 'Sends an HTTP request to a specified URL with the given method and optional body and headers.',
 ARRAY ['http', 'request', 'network'],
 'ph:globe',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "url": {
           "type": "string",
           "format": "uri",
           "description": "The URL to send the HTTP request to."
         },
         "method": {
           "type": "string",
           "enum": [
             "GET",
             "POST",
             "PUT",
             "DELETE",
             "PATCH",
             "HEAD",
             "OPTIONS"
           ],
           "description": "HTTP method to use for the request."
         },
         "body": {
           "type": "object",
           "description": "Optional body content for methods like POST or PUT.",
           "additionalProperties": true,
           "default": {}
         },
         "headers": {
           "type": "object",
           "description": "Key-value pairs of HTTP headers.",
           "additionalProperties": {
             "type": "string"
           },
           "default": {}
         }
       },
       "required": [
         "url",
         "method"
       ],
       "additionalProperties": false
     },
     "output": {
       "type": "object",
       "properties": {
         "status_code": {
           "type": "integer"
         },
         "headers": {
           "type": "object"
         },
         "body": {
           "type": "object"
         }
       }
     },
     "secrets": {
       "type": "array",
       "items": {
         "type": "object",
         "properties": {
           "key": {
             "type": "string"
           }
         },
         "required": [
           "key"
         ]
       }
     }
   },
   "required": [
     "input"
   ]
 }'::jsonb),

-- 2. Data Transformer
((SELECT id FROM core_plugin),
 'data.transformer',
 'Data Transformer',
 'action',
 '1.0.0',
 'Transforms data using specified parameters and optional pipeline steps.',
 ARRAY ['data', 'transformer', 'pipeline'],
 'ph:code',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "definitions": {
     "concat_params": {
       "type": "object",
       "properties": {
         "suffix": {
           "type": "string"
         }
       },
       "required": [
         "suffix"
       ]
     },
     "get_field_params": {
       "type": "object",
       "properties": {
         "field": {
           "type": "string"
         }
       },
       "required": [
         "field"
       ]
     },
     "set_field_params": {
       "type": "object",
       "properties": {
         "field": {
           "type": "string"
         },
         "value": {}
       },
       "required": [
         "field",
         "value"
       ]
     },
     "regex_split_params": {
       "type": "object",
       "properties": {
         "pattern": {
           "type": "string"
         }
       },
       "required": [
         "pattern"
       ]
     },
     "substring_params": {
       "type": "object",
       "properties": {
         "start": {
           "type": "integer"
         },
         "end": {
           "type": "integer"
         }
       }
     },
     "transform_step": {
       "type": "object",
       "properties": {
         "transformer": {
           "type": "string"
         },
         "params": {
           "type": "object",
           "default": {}
         }
       },
       "required": [
         "transformer"
       ],
       "oneOf": [
         {
           "properties": {
             "transformer": {
               "const": "concat"
             },
             "params": {
               "$ref": "#/definitions/concat_params"
             }
           }
         },
         {
           "properties": {
             "transformer": {
               "const": "get_field"
             },
             "params": {
               "$ref": "#/definitions/get_field_params"
             }
           }
         },
         {
           "properties": {
             "transformer": {
               "const": "set_field"
             },
             "params": {
               "$ref": "#/definitions/set_field_params"
             }
           }
         },
         {
           "properties": {
             "transformer": {
               "const": "regex_split"
             },
             "params": {
               "$ref": "#/definitions/regex_split_params"
             }
           }
         },
         {
           "properties": {
             "transformer": {
               "const": "substring"
             },
             "params": {
               "$ref": "#/definitions/substring_params"
             }
           }
         },
         {
           "properties": {
             "transformer": {
               "const": "lowercase"
             }
           }
         },
         {
           "properties": {
             "transformer": {
               "const": "uppercase"
             }
           }
         },
         {
           "properties": {
             "transformer": {
               "const": "trim"
             }
           }
         }
       ]
     }
   },
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "name": {
           "type": "string"
         },
         "data": {},
         "params": {
           "type": "object",
           "default": {}
         },
         "isPipeline": {
           "type": "boolean",
           "default": false
         },
         "forEach": {
           "type": "boolean",
           "default": false
         },
         "steps": {
           "type": "array",
           "items": {
             "$ref": "#/definitions/transform_step"
           },
           "default": []
         }
       },
       "required": [
         "data"
       ],
       "if": {
         "properties": {
           "isPipeline": {
             "const": false
           }
         }
       },
       "then": {
         "required": [
           "name"
         ]
       },
       "else": {
         "required": [
           "steps"
         ]
       }
     },
     "output": {
       "type": "object",
       "properties": {
         "result": {}
       }
     }
   },
   "required": [
     "input"
   ]
 }'::jsonb),

-- 3. Trigger Workflow
((SELECT id FROM core_plugin),
 'workflow.trigger',
 'Trigger Workflow',
 'action',
 '1.0.0',
 'Triggers a workflow run by its ID, optionally running it asynchronously.',
 ARRAY ['workflow', 'trigger', 'run'],
 'ph:rocket-launch',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "workflow_run_id": {
           "type": "string",
           "format": "uuid",
           "description": "ID of the workflow run to trigger."
         },
         "workflow_id": {
           "type": "string",
           "format": "uuid",
           "description": "ID of the workflow to trigger."
         },
         "is_async": {
           "type": "boolean",
           "description": "Trigger the workflow asynchronously.",
           "default": false
         }
       },
       "required": [
         "workflow_run_id",
         "workflow_id"
       ],
       "additionalProperties": false
     },
     "output": {
       "type": "object"
     },
     "secrets": {
       "type": "array",
       "items": {
         "type": "object",
         "properties": {
           "key": { "type": "string" }
         },
         "required": ["key"]
       }
     }
   },
   "required": ["input"]
 }'::jsonb),

-- 4. If Condition
((SELECT id FROM core_plugin),
 'flow.branch.if',
 'If',
 'if',
 '1.0.0',
 'Evaluates a condition and branches the workflow based on the result.',
 ARRAY ['condition', 'if', 'branch'],
 'ph:git-fork',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "condition": {
           "type": "string",
           "description": "The expression to evaluate. Should resolve to a boolean."
         },
         "next_true": {
           "type": "array",
           "description": "The next node(s) to execute if the condition is true.",
           "items": {
             "type": "string"
           }
         },
         "next_false": {
           "type": "array",
           "description": "The next node(s) to execute if the condition is false.",
           "items": {
             "type": "string"
           }
         }
       },
       "required": [
         "condition"
       ]
     },
     "output": {
       "type": "object"
     }
   },
   "required": ["input"]
 }'::jsonb),

-- 5. Switch
((SELECT id FROM core_plugin),
 'flow.branch.switch',
 'Switch',
 'switch',
 '1.0.0',
 'Directs the workflow to different branches based on the value of an expression.',
 ARRAY ['condition', 'switch', 'branch'],
 'ph:git-branch',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "expression": {
           "type": "string",
           "description": "The expression to evaluate against cases."
         },
         "cases": {
           "type": "array",
           "description": "A list of cases to match against.",
           "items": {
             "type": "object",
             "properties": {
               "value": {
                 "type": "string"
               },
               "next": {
                 "type": "array",
                 "items": {
                   "type": "string"
                 }
               }
             },
             "required": [
               "value",
               "next"
             ]
           }
         },
         "default_next": {
           "type": "array",
           "description": "The default node(s) to execute if no case matches.",
           "items": {
             "type": "string"
           }
         }
       },
       "required": [
         "expression",
         "cases"
       ]
     },
     "output": {
       "type": "object"
     },
     "secrets": {
       "type": "array",
       "items": {
         "type": "object",
         "properties": {
           "key": { "type": "string" }
         },
         "required": ["key"]
       }
     }
   },
   "required": ["input"]
 }'::jsonb),

-- 6. For Loop
((SELECT id FROM core_plugin),
 'flow.loop.for',
 'For Loop',
 'for_loop',
 '1.0.0',
 'Iterates over a collection of items or repeats a sequence of nodes a specific number of times.',
 ARRAY ['loop', 'for', 'iterator'],
 'ph:repeat',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "indexVar": {
           "type": "string",
           "description": "The variable name to assign to the current iteration index.",
           "default": "index"
         },
         "updateExpression": {
           "type": "string",
           "description": "An expression to update the index variable after each iteration.",
           "default": "index + 1"
         },
         "total": {
              "type": "integer",
              "description": "The total number of iterations to perform."
         },
         "endCondition": {
           "type": "string",
           "description": "An expression that when true will exit the loop."
         },
         "breakCondition": {
           "type": "string",
           "description": "An expression that when true will exit the loop."
         },
         "continueCondition": {
           "type": "string",
           "description": "An expression that when true will skip to the next iteration."
         },
         "loopEnd": {
           "type": "array",
           "description": "The node(s) to execute after the loop ends.",
           "items": {
             "type": "string"
           }
         },
         "next": {
           "type": "array",
           "description": "The next node(s) to execute within the loop body.",
           "items": {
             "type": "string"
           }
         }
       },
       "required": [
         "next",
         "loopEnd"
       ]
     },
     "output": {
       "type": "object"
     },
     "secrets": {
       "type": "array",
       "items": {
         "type": "object",
         "properties": {
           "key": {
             "type": "string"
           }
         },
         "required": [
           "key"
         ]
       }
     }
   },
   "required": [
     "input"
   ]
 }'::jsonb),

-- 7. While Loop
((SELECT id FROM core_plugin),
 'flow.loop.while',
 'While Loop',
 'while_loop',
 '1.0.0',
 'Executes a sequence of nodes as long as a condition remains true.',
 ARRAY ['loop', 'while'],
 'ph:infinity',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "condition": {
           "type": "string",
           "description": "The expression to evaluate before each iteration. The loop continues as long as this is true."
         },
         "breakCondition": {
           "type": "string",
           "description": "An expression that when true will exit the loop."
         },
         "continueCondition": {
           "type": "string",
           "description": "An expression that when true will skip to the next iteration."
         },
         "next": {
           "type": "array",
           "description": "The node(s) to execute in each iteration if the condition is true.",
           "items": {
             "type": "string"
           }
         },
         "loopEnd": {
           "type": "array",
           "description": "The node(s) to execute when the loop condition evaluates to false.",
           "items": {
             "type": "string"
           }
         }
       },
       "required": [
         "condition",
         "next",
         "loopEnd"
       ]
     },
     "output": {
       "type": "object"
     },
     "secrets": {
       "type": "array",
       "items": {
         "type": "object",
         "properties": {
           "key": {
             "type": "string"
           }
         },
         "required": [
           "key"
         ]
       }
     }
   },
   "required": [
     "input"
   ]
 }'::jsonb),

-- 8. Timeout
((SELECT id FROM core_plugin),
 'flow.timeout',
 'Timeout',
 'timeout',
 '1.0.0',
 'Pauses workflow execution for a specified duration.',
 ARRAY ['timeout', 'delay', 'wait'],
 'ph:clock',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "duration": {
           "type": "string",
           "description": "The duration to wait (e.g., \"5\" for 5 units)."
         },
         "unit": {
           "type": "string",
           "enum": [
             "milliseconds",
             "seconds"
           ],
           "description": "The unit of time for the duration.",
           "default": "seconds"
         },
         "next": {
           "type": "array",
           "description": "The node(s) to execute after the timeout.",
           "items": {
             "type": "string"
           }
         }
       },
       "required": [
         "duration",
         "unit"
       ],
       "additionalProperties": false
     },
     "output": {
       "type": "object"
     },
     "secrets": {
       "type": "array",
       "items": {
         "type": "object",
         "properties": {
           "key": { "type": "string" }
         },
         "required": ["key"]
       }
     }
   },
   "required": ["input"]
 }'::jsonb),

-- 9. Condition
((SELECT id FROM core_plugin),
 'flow.branch.condition',
 'Condition',
 'condition',
 '1.0.0',
 'Evaluates a series of conditions and directs the workflow based on the first matching condition.',
 ARRAY ['condition', 'branch', 'multiple-if'],
 'ph:git-branch',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "cases": {
           "type": "array",
           "description": "List of condition cases to check in sequence.",
           "items": {
             "type": "object",
             "properties": {
               "when": {
                 "type": "string",
                 "description": "Boolean expression to evaluate."
               },
               "then": {
                 "type": "string",
                 "description": "Node to execute when the condition is true."
               }
             },
             "required": [
               "when",
               "then"
             ]
           }
         },
         "default_case": {
           "type": "string",
           "description": "Default node to execute when no condition matches."
         }
       },
       "required": [
         "cases",
         "default_case"
       ]
     },
     "output": {
       "type": "object"
     },
     "secrets": {
       "type": "array",
       "items": {
         "type": "object",
         "properties": {
           "key": {
             "type": "string"
           }
         },
         "required": [
           "key"
         ]
       }
     }
   },
   "required": [
     "input"
   ]
 }'::jsonb),

-- 10. For Each
((SELECT id FROM core_plugin),
 'flow.loop.foreach',
 'For Each',
 'for_each',
 '1.0.0',
 'Iterates over each item in a collection and executes the specified nodes for each item.',
 ARRAY['loop', 'for_each', 'iterator'],
 'ph:repeat',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "items": {
           "type": "array",
           "description": "The collection of items to iterate over."
         },
         "itemVar": {
           "type": "string",
           "description": "Variable name for the current item in the loop.",
           "default": "item"
         },
         "indexVar": {
           "type": "string",
           "description": "Variable name for the current index in the loop.",
           "default": "index"
         },
         "breakCondition": {
           "type": "string",
           "description": "An expression that when true will exit the loop."
         },
         "continueCondition": {
           "type": "string",
           "description": "An expression that when true will skip to the next iteration."
         },
         "loopEnd": {
           "type": "array",
           "description": "The node(s) to execute after the loop ends.",
           "items": {
             "type": "string"
           }
         },
         "next": {
           "type": "array",
           "description": "The next node(s) to execute within the loop body.",
           "items": {
             "type": "string"
           }
         }
       },
       "required": [
         "items",
         "next",
         "loopEnd"
       ]
     },
     "output": {
       "type": "object"
     }
   },
   "required": ["input"]
 }'::jsonb),

-- 11. Schedule Trigger
((SELECT id FROM core_plugin),
 'trigger.schedule',
 'Schedule',
 'schedule_trigger',
 '1.0.0',
 'Triggers the workflow based on a CRON schedule.',
 ARRAY['trigger', 'schedule', 'cron'],
 'ph:clock',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "payload": {
           "type": "object",
           "description": "Optional payload data to pass to the workflow.",
           "additionalProperties": true
         },
         "cron_expression": {
           "type": "string",
           "description": "The CRON expression for the schedule."
         },
         "schedule_description": {
           "type": "string",
           "description": "Optional description of the schedule."
         }
       },
       "additionalProperties": true
     },
     "output": {
       "type": "object",
       "properties": {
         "trigger_type": {
           "type": "string"
         },
         "triggered_at": {
           "type": "string"
         },
         "trigger_source": {
           "type": "string"
         },
         "cron_expression": {
           "type": "string"
         },
         "schedule_description": {
           "type": "string"
         },
         "payload": {
           "type": "object"
         }
       }
     }
   }
 }'::jsonb),

-- 12. Manual Trigger
((SELECT id FROM core_plugin),
 'manual.trigger',
 'Manual Trigger',
 'manual_trigger',
 '1.0.0',
 'Manually triggers the workflow execution.',
 ARRAY['trigger', 'manual'],
 'ph:play',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "payload": {
           "type": "object",
           "description": "Optional payload data to pass to the workflow.",
           "additionalProperties": true
         }
       },
       "additionalProperties": true
     },
     "output": {
       "type": "object",
       "properties": {
         "trigger_type": {
           "type": "string"
         },
         "triggered_at": {
           "type": "string"
         },
         "trigger_source": {
           "type": "string"
         },
         "payload": {
           "type": "object"
         }
       }
     }
   }
 }'::jsonb),

-- 13. Webhook Trigger
((SELECT id FROM core_plugin),
 'webhook.trigger',
 'Webhook Trigger',
 'webhook_trigger',
 '1.0.0',
 'Triggers the workflow when a webhook endpoint is called.',
 ARRAY['trigger', 'webhook', 'http'],
 'ph:webhook',
 '{
   "$schema": "http://json-schema.org/draft-07/schema#",
   "type": "object",
   "properties": {
     "input": {
       "type": "object",
       "properties": {
         "payload": {
           "type": "object",
           "description": "The webhook request body/payload.",
           "additionalProperties": true
         },
         "headers": {
           "type": "object",
           "description": "The webhook request headers.",
           "additionalProperties": {
             "type": "string"
           }
         },
         "http_method": {
           "type": "string",
           "description": "HTTP method used for the webhook request."
         },
         "source_ip": {
           "type": "string",
           "description": "IP address of the webhook request source."
         },
         "user_agent": {
           "type": "string",
           "description": "User agent of the webhook request."
         },
         "webhook_id": {
           "type": "string",
           "description": "Unique identifier for the webhook."
         }
       },
       "additionalProperties": true
     },
     "output": {
       "type": "object",
       "properties": {
         "trigger_type": {
           "type": "string"
         },
         "triggered_at": {
           "type": "string"
         },
         "trigger_source": {
           "type": "string"
         },
         "http_method": {
           "type": "string"
         },
         "source_ip": {
           "type": "string"
         },
         "user_agent": {
           "type": "string"
         },
         "webhook_id": {
           "type": "string"
         },
         "headers": {
           "type": "object"
         },
         "payload": {
           "type": "object"
         }
       }
     }
   }
 }'::jsonb);