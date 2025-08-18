# Workflow Trigger Node

## Overview

Triggers one workflow from within another workflow, enabling workflow composition and modular automation.

## Node Information

- **Key**: `core:workflow.trigger`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:flow-arrow`
- **Tags**: `trigger`, `workflow`, `composition`, `orchestration`

## Description

The Workflow Trigger node enables one workflow to start another workflow, creating powerful workflow composition capabilities. This allows for modular design, reusable workflows, and complex orchestration patterns.

## Input/Output

### Input
- `workflow_id` (string, required): ID of the target workflow to trigger
- `payload` (object, optional): Data to pass to the triggered workflow
- `wait_for_completion` (boolean): Whether to wait for the triggered workflow to complete (default: false)
- `timeout` (integer): Timeout in seconds when waiting for completion (default: 300)

### Output
- `triggered_workflow_id` (string): ID of the workflow that was triggered
- `execution_id` (string): Unique execution ID for the triggered workflow
- `triggered_at` (string): ISO timestamp when workflow was triggered
- `status` (string): Status of the triggered workflow (pending, running, completed, failed)
- `result` (object): Result data from triggered workflow (if wait_for_completion = true)

## Usage Examples

### Fire-and-Forget Workflow
```json
{
  "key": "trigger-cleanup",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "workflow.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "workflow_id": "cleanup-workflow-123",
      "payload": {
        "cleanup_type": "logs",
        "older_than": "7d"
      }
    }
  }
}
```

### Wait for Completion
```json
{
  "input": {
    "workflow_id": "data-processing-workflow",
    "payload": {
      "dataset": "{{current_dataset}}",
      "processing_mode": "batch"
    },
    "wait_for_completion": true,
    "timeout": 600
  }
}
```

## Common Use Cases

- **Modular Workflows**: Break complex processes into reusable components
- **Parallel Processing**: Trigger multiple workflows concurrently
- **Conditional Execution**: Trigger different workflows based on conditions
- **Error Recovery**: Trigger cleanup or recovery workflows on failures
- **Multi-stage Processing**: Chain workflows for complex data pipelines
