# Timeout Node

## Overview

Introduces a time delay in workflow execution, useful for rate limiting, waiting periods, and scheduled operations.

## Node Information

- **Key**: `core:flow.timeout`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:timer`
- **Tags**: `flow`, `timeout`, `delay`, `wait`

## Description

The Timeout node pauses workflow execution for a specified duration before proceeding to the next nodes. It supports milliseconds and seconds as time units and can be used for rate limiting, implementing waiting periods, or creating scheduled delays in workflows.

## Input/Output

### Input
- `duration` (string, required): The duration to wait (e.g., "5" for 5 units)
- `unit` (string, required): The unit of time - `milliseconds` or `seconds` (default: "seconds")
- `next` (array): The node(s) to execute after the timeout completes

### Output
- `timeout_duration` (string): The duration that was waited
- `timeout_unit` (string): The time unit used
- `started_at` (string): ISO timestamp when timeout started
- `completed_at` (string): ISO timestamp when timeout completed
- `actual_wait_time` (integer): Actual time waited in milliseconds

## Usage Examples

### Basic 5-Second Delay
```json
{
  "key": "wait-5-seconds",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "flow.timeout",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "duration": "5",
      "unit": "seconds",
      "next": ["continue-processing"]
    }
  }
}
```

### Rate Limiting Delay
```json
{
  "input": {
    "duration": "500",
    "unit": "milliseconds"
  }
}
```

### Dynamic Timeout from Configuration
```json
{
  "input": {
    "duration": "{{config.retry_delay}}",
    "unit": "seconds"
  }
}
```

## Common Use Cases

- **Rate Limiting**: Prevent overwhelming external APIs
- **Retry Delays**: Wait between retry attempts
- **Batch Processing**: Delay between batch operations
- **Cooldown Periods**: Implement system cooldown delays
- **Scheduled Operations**: Create time-based workflow steps
- **Testing**: Simulate processing delays for testing
