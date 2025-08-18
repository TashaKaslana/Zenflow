# Wait Node

## Overview

Waits for one or more workflow nodes to complete before proceeding. Provides sophisticated synchronization capabilities for complex workflow orchestration.

## Node Information

- **Key**: `core:flow.wait`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:clock`
- **Tags**: `flow`, `synchronization`, `wait`, `control`

## Description

The Wait node provides advanced synchronization mechanisms for workflow execution. It can wait for all specified nodes to complete, any of them to complete, or a threshold number of nodes to complete. This is essential for coordinating parallel execution paths and ensuring proper workflow sequencing.

## Input/Output

### Input
- `mode` (string): Wait condition mode
  - `"all"`: Wait for all specified nodes to complete
  - `"any"`: Wait for at least one node to complete (default)
  - `"threshold"`: Wait for a specific number of nodes to complete
- `threshold` (integer): Number of nodes that must complete when mode is "threshold" (default: 1)
- `waitingNodes` (object, required): Map of node IDs to their completion status (boolean values)

### Output
- `completed_nodes` (array): List of node IDs that have completed
- `completion_count` (integer): Number of nodes that completed
- `wait_mode` (string): The wait mode that was used
- `threshold_met` (boolean): Whether the completion threshold was met
- `waited_at` (string): ISO timestamp when wait completed

## Usage Examples

### Wait for All Nodes
```json
{
  "key": "wait-all",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "flow.wait",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "mode": "all",
      "waitingNodes": {
        "process-1": false,
        "process-2": false,
        "process-3": false
      }
    }
  }
}
```

### Wait for Any Node (First to Complete)
```json
{
  "input": {
    "mode": "any",
    "waitingNodes": {
      "fast-process": false,
      "slow-process": false
    }
  }
}
```

### Wait for Threshold (2 out of 4 nodes)
```json
{
  "input": {
    "mode": "threshold",
    "threshold": 2,
    "waitingNodes": {
      "worker-1": false,
      "worker-2": false,
      "worker-3": false,
      "worker-4": false
    }
  }
}
```

## Common Use Cases

- **Parallel Processing**: Wait for multiple parallel processes to complete
- **Race Conditions**: Wait for the first of several alternative processes
- **Batch Processing**: Wait for a minimum number of batch jobs to finish
- **Fault Tolerance**: Continue when enough processes succeed (threshold mode)
- **Resource Synchronization**: Ensure resources are available before proceeding
