# If Node (Conditional Branch)

## Overview

Provides conditional branching in workflows, executing different paths based on boolean expression evaluation.

## Node Information

- **Key**: `core:flow.if`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:git-branch`
- **Tags**: `flow`, `conditional`, `branch`, `if`

## Description

The If node enables conditional logic in workflows by evaluating boolean expressions and routing execution to different paths based on the result. It supports complex conditions using workflow data and provides true/false execution branches.

## Input/Output

### Input
- `condition` (string, required): The expression to evaluate (should resolve to boolean)
- `next_true` (array, optional): Node(s) to execute if condition is true
- `next_false` (array, optional): Node(s) to execute if condition is false

### Output
- `condition_result` (boolean): The result of the condition evaluation
- `condition_expression` (string): The original condition expression
- `path_taken` (string): Which path was taken ("true" or "false")
- `evaluated_at` (string): ISO timestamp when condition was evaluated

## Usage Examples

### Simple Boolean Check
```json
{
  "key": "check-user-active",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "flow.if",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "condition": "{{user.active}}",
      "next_true": ["send-welcome-email"],
      "next_false": ["log-inactive-user"]
    }
  }
}
```

### Numeric Comparison
```json
{
  "input": {
    "condition": "{{order.total}} > 100",
    "next_true": ["apply-discount", "send-vip-notification"],
    "next_false": ["standard-processing"]
  }
}
```

### Complex Condition
```json
{
  "input": {
    "condition": "{{user.subscription}} === 'premium' && {{user.credits}} > 0",
    "next_true": ["premium-processing"],
    "next_false": ["upgrade-prompt"]
  }
}
```

### String Matching
```json
{
  "input": {
    "condition": "{{webhook.payload.event_type}} === 'payment_success'",
    "next_true": ["process-payment"],
    "next_false": ["handle-other-events"]
  }
}
```

## Supported Operators

### Comparison Operators
- `==`, `===`: Equality
- `!=`, `!==`: Inequality  
- `>`, `>=`: Greater than (or equal)
- `<`, `<=`: Less than (or equal)

### Logical Operators
- `&&`: Logical AND
- `||`: Logical OR
- `!`: Logical NOT

### String Operations
- `.includes()`: String contains
- `.startsWith()`: String starts with
- `.endsWith()`: String ends with

## Common Use Cases

- **User Authentication**: Route based on login status
- **Feature Flags**: Enable/disable features conditionally
- **Data Validation**: Handle valid vs invalid data differently
- **Business Logic**: Apply different rules based on conditions
- **Error Handling**: Different paths for success/failure scenarios
- **A/B Testing**: Route users to different experience paths
