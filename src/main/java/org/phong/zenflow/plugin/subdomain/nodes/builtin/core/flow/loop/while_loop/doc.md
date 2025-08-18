# While Loop Node

## Overview

Executes a set of nodes repeatedly while a specified condition remains true, providing dynamic loop control.

## Node Information

- **Key**: `core:flow.while_loop`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:arrow-clockwise`
- **Tags**: `flow`, `loop`, `while`, `condition`

## Description

The While Loop node provides condition-based looping, executing a set of nodes repeatedly as long as a specified condition evaluates to true. It includes safety mechanisms like maximum iteration limits and supports dynamic conditions based on workflow data.

## Input/Output

### Input
- `condition` (string, required): Boolean expression that controls the loop
- `loop_body` (array): Node(s) to execute in each iteration
- `max_iterations` (integer): Safety limit for iterations (default: 100)
- `iteration_variable` (string): Variable name for iteration counter (default: "iteration")

### Output
- `total_iterations` (integer): Total number of iterations completed
- `condition_final_value` (boolean): Final condition value when loop ended
- `loop_completed` (boolean): Whether loop completed normally or hit limits
- `termination_reason` (string): Why the loop terminated
- `execution_time` (integer): Total execution time in milliseconds

## Usage Examples

### Retry Until Success
```json
{
  "key": "retry-until-success",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "flow.while_loop",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "condition": "{{api_response.status}} !== 'success'",
      "loop_body": ["call-external-api", "wait-delay"],
      "max_iterations": 5
    }
  }
}
```

### Process Until Queue Empty
```json
{
  "input": {
    "condition": "{{queue.length}} > 0",
    "loop_body": ["process-queue-item", "update-queue"],
    "max_iterations": 1000
  }
}
```

## Common Use Cases

- **Retry Logic**: Retry operations until success
- **Queue Processing**: Process items until queue is empty
- **Polling**: Poll external systems until condition is met
- **Dynamic Processing**: Continue processing based on runtime conditions
