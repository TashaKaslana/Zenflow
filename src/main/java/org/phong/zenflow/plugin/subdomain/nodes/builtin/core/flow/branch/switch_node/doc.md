# Switch Node (Multi-way Branch)

## Overview

Provides multi-way branching in workflows, executing different paths based on expression matching against multiple cases.

## Node Information

- **Key**: `core:flow.switch`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:tree-structure`
- **Tags**: `flow`, `switch`, `branch`, `multi-way`

## Description

The Switch node enables multi-way conditional logic in workflows by evaluating an expression against multiple case values and routing execution to the matching path. It includes a default case for unmatched values, similar to switch statements in programming languages.

## Input/Output

### Input
- `expression` (string, required): The expression to evaluate against cases
- `cases` (array, required): List of cases to match against
  - `value` (string, required): The value to match
  - `next` (array, required): Node(s) to execute if this case matches
- `default_next` (array, optional): Default node(s) to execute if no case matches

### Output
- `expression_value` (string): The evaluated expression value
- `matched_case` (string): The case value that matched (or "default")
- `path_taken` (array): The nodes that will be executed
- `evaluated_at` (string): ISO timestamp when switch was evaluated

## Usage Examples

### User Role-Based Routing
```json
{
  "key": "route-by-role",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "flow.switch",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "expression": "{{user.role}}",
      "cases": [
        {
          "value": "admin",
          "next": ["admin-dashboard", "log-admin-access"]
        },
        {
          "value": "manager",
          "next": ["manager-dashboard"]
        },
        {
          "value": "user",
          "next": ["user-dashboard"]
        }
      ],
      "default_next": ["access-denied"]
    }
  }
}
```

### Event Type Processing
```json
{
  "input": {
    "expression": "{{webhook.payload.event_type}}",
    "cases": [
      {
        "value": "user_created",
        "next": ["send-welcome-email", "create-profile"]
      },
      {
        "value": "payment_success",
        "next": ["update-subscription", "send-receipt"]
      },
      {
        "value": "order_cancelled",
        "next": ["refund-payment", "notify-warehouse"]
      }
    ],
    "default_next": ["log-unknown-event"]
  }
}
```

### HTTP Status Code Handling
```json
{
  "input": {
    "expression": "{{api_response.status_code}}",
    "cases": [
      {
        "value": "200",
        "next": ["process-success-response"]
      },
      {
        "value": "401",
        "next": ["refresh-auth-token", "retry-request"]
      },
      {
        "value": "404",
        "next": ["handle-not-found"]
      },
      {
        "value": "500",
        "next": ["log-server-error", "alert-ops-team"]
      }
    ],
    "default_next": ["handle-generic-error"]
  }
}
```

### Priority-Based Processing
```json
{
  "input": {
    "expression": "{{task.priority}}",
    "cases": [
      {
        "value": "critical",
        "next": ["immediate-processing", "alert-team"]
      },
      {
        "value": "high",
        "next": ["priority-queue"]
      },
      {
        "value": "medium",
        "next": ["standard-queue"]
      },
      {
        "value": "low",
        "next": ["background-queue"]
      }
    ],
    "default_next": ["standard-queue"]
  }
}
```

## Common Use Cases

- **User Role Management**: Route users based on permissions
- **Event Processing**: Handle different event types appropriately
- **API Response Handling**: Different logic for different status codes
- **Workflow States**: Route based on current workflow state
- **Content Type Processing**: Handle different file/data types
- **Priority Systems**: Route based on priority levels
- **Multi-tenant Logic**: Different processing for different tenants

## Best Practices

- Always provide a default case to handle unexpected values
- Use clear, descriptive case values
- Keep case logic simple and focused
- Consider using string constants for case values
- Document the expected expression values
