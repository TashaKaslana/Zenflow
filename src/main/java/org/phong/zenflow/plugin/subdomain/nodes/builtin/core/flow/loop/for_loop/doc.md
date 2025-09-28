# For Loop Node

## Overview

Executes a set of nodes a specified number of times with counter variable access, ideal for counted iterations.

## Node Information

- **Key**: `core:flow.for_loop`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:repeat`
- **Tags**: `flow`, `loop`, `counter`, `iteration`

## Description

The For Loop node provides traditional counted loop functionality, executing a set of nodes a specified number of times. It maintains a counter variable that can be accessed within the loop body, making it perfect for operations that need to know the current iteration number.

## Input/Output

### Input
- `start` (integer): Starting counter value (default: 0)
- `end` (integer, required): Ending counter value (exclusive)
- `step` (integer): Increment step (default: 1)
- `counter_variable` (string): Variable name for counter (default: "counter")
- `loop_body` (array): Node(s) to execute for each iteration
- `break_condition` (string): Optional condition to break early

### Output
- `total_iterations` (integer): Total number of iterations completed
- `final_counter_value` (integer): Final value of the counter
- `loop_completed` (boolean): Whether loop completed normally
- `break_reason` (string): Reason for early break (if applicable)
- `execution_time` (integer): Total execution time in milliseconds

## Usage Examples

### Simple Counter Loop
```json
{
  "key": "retry-operation",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "flow.for_loop",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "start": 1,
      "end": 4,
      "counter_variable": "attempt",
      "loop_body": ["attempt-api-call", "check-result"]
    }
  }
}
```

### Batch Processing with Steps
```json
{
  "input": {
    "start": 0,
    "end": 1000,
    "step": 50,
    "counter_variable": "batch_offset",
    "loop_body": ["fetch-batch-data", "process-batch"]
  }
}
```

## Common Use Cases

- **Retry Logic**: Attempt operations multiple times
- **Batch Processing**: Process data in numbered batches
- **Pagination**: Iterate through paginated results
- **Performance Testing**: Run operations multiple times
