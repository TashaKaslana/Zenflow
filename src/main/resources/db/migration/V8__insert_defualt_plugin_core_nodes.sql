ALTER TABLE plugin_nodes
    ADD COLUMN IF NOT EXISTS icon TEXT;

INSERT INTO plugins (id,
                     publisher_id,
                     name,
                     version,
                     registry_url,
                     verified,
                     created_at,
                     updated_at,
                     description,
                     tags)
VALUES (gen_random_uuid(),
        NULL,
        'core',
        '1.0.0',
        NULL,
        true,
        now(),
        now(),
        'Built-in core plugin containing essential nodes.',
        ARRAY ['core', 'builtin'])
ON CONFLICT (name) DO NOTHING;

-- Insert plugin nodes for core plugin
WITH core_plugin AS (SELECT id
                     FROM plugins
                     WHERE name = 'core'
                     LIMIT 1)
INSERT
INTO plugin_nodes (plugin_id, name, type, plugin_node_version, description, tags, icon, config_schema)
VALUES
-- 1. HTTP Request
((SELECT id FROM core_plugin),
 'HTTP Request',
 'action',
 '1.0.0',
 'Sends an HTTP request to a specified URL with the given method and optional body and headers.',
 ARRAY ['http', 'request', 'network'],
 'ph:globe',
 '{
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
       "type": "string",
       "description": "Optional body content for methods like POST or PUT.",
       "default": ""
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
 }'::jsonb),

-- 2. Data Transformer
((SELECT id FROM core_plugin),
 'Data Transformer',
 'action',
 '1.0.0',
 'Transforms data using specified parameters and optional pipeline steps.',
 ARRAY ['data', 'transformer', 'pipeline'],
 'ph:code',
 '{
   "type": "object",
   "properties": {
     "name": {
       "type": "string"
     },
     "input": {
       "type": "string"
     },
     "params": {
       "type": "object",
       "description": "Parameters for the transformer.",
       "default": {}
     },
     "isPipeline": {
       "type": "boolean",
       "description": "Whether to run a sequence of transformations.",
       "default": false
     },
     "steps": {
       "type": "array",
       "description": "List of transformation steps if using a pipeline.",
       "items": {
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
         ]
       },
       "default": []
     }
   },
   "required": [
     "input"
   ],
   "allOf": [
     {
       "if": {
         "properties": {
           "isPipeline": {
             "const": true
           }
         }
       },
       "then": {
         "required": [
           "steps"
         ]
       },
       "else": {
         "required": [
           "name"
         ]
       }
     }
   ],
   "additionalProperties": false
 }'::jsonb),

-- 3. Trigger Workflow
((SELECT id FROM core_plugin),
 'Trigger Workflow',
 'action',
 '1.0.0',
 'Triggers a workflow run by its ID, optionally running it asynchronously.',
 ARRAY ['workflow', 'trigger', 'run'],
 'ph:rocket-launch',
 '{
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
 }'::jsonb),

-- 4. If Condition
((SELECT id FROM core_plugin),
 'If',
 'if',
 '1.0.0',
 'Evaluates a condition and branches the workflow based on the result.',
 ARRAY ['condition', 'if', 'branch'],
 'ph:git-fork',
 '{
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
 }'::jsonb),

-- 5. Switch
((SELECT id FROM core_plugin),
 'Switch',
 'switch',
 '1.0.0',
 'Directs the workflow to different branches based on the value of an expression.',
 ARRAY ['condition', 'switch', 'branch'],
 'ph:git-branch',
 '{
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
 }'::jsonb),

-- 6. For Loop
((SELECT id FROM core_plugin),
 'For Loop',
 'for_loop',
 '1.0.0',
 'Iterates over a collection of items or repeats a sequence of nodes a specific number of times.',
 ARRAY ['loop', 'for', 'iterator'],
 'ph:repeat',
 '{
   "type": "object",
   "properties": {
     "iterator": {
       "type": "string",
       "description": "The variable name containing a list to iterate over."
     },
     "times": {
       "type": "number",
       "description": "Number of times to repeat the loop (used when iterator is not provided)."
     },
     "itemVar": {
       "type": "string",
       "description": "The variable name to assign to each item in the collection.",
       "default": "item"
     },
     "indexVar": {
       "type": "string",
       "description": "The variable name to assign to the current iteration index.",
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
   "oneOf": [
     {
       "required": [
         "iterator"
       ]
     },
     {
       "required": [
         "times"
       ]
     }
   ]
 }'::jsonb),

-- 7. While Loop
((SELECT id FROM core_plugin),
 'While Loop',
 'while_loop',
 '1.0.0',
 'Executes a sequence of nodes as long as a condition remains true.',
 ARRAY ['loop', 'while'],
 'ph:infinity',
 '{
   "type": "object",
   "properties": {
     "condition": {
       "type": "string",
       "description": "The expression to evaluate before each iteration. The loop continues as long as this is true."
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
 }'::jsonb),

-- 8. Timeout
((SELECT id FROM core_plugin),
 'Timeout',
 'timeout',
 '1.0.0',
 'Pauses workflow execution for a specified duration.',
 ARRAY ['timeout', 'delay', 'wait'],
 'ph:clock',
 '{
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
 }'::jsonb),

-- 9. Condition
((SELECT id FROM core_plugin),
 'Condition',
 'condition',
 '1.0.0',
 'Evaluates a series of conditions and directs the workflow based on the first matching condition.',
 ARRAY ['condition', 'branch', 'multiple-if'],
 'ph:git-branch',
 '{
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
 }'::jsonb);
