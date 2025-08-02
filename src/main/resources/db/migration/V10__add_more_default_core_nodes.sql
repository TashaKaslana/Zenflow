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
 ')
ON CONFLICT DO NOTHING;