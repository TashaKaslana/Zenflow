# Manual Trigger Node

## Overview

Allows workflows to be started manually by users through the UI or API, with optional payload data.

## Node Information

- **Key**: `core:manual.trigger`
- **Version**: `1.0.0`
- **Type**: `trigger`
- **Icon**: `ph:play`
- **Tags**: `trigger`, `manual`, `user`, `interactive`

## Description

The Manual Trigger node enables users to start workflows on-demand through the user interface or API calls. It's ideal for workflows that need human intervention, testing, or ad-hoc execution with custom parameters.

## Input/Output

### Input
- `payload` (object, optional): Optional payload data to pass to the workflow

### Output
- `trigger_type` (string): Always "manual" for manual triggers
- `triggered_at` (string): ISO timestamp when manually triggered
- `trigger_source` (string): Source of manual trigger (UI, API, etc.)
- `payload` (object): Any payload data provided during manual trigger
- `user_id` (string): ID of user who triggered the workflow
- `session_id` (string): Session identifier for the trigger

## Usage Examples

### Basic Manual Trigger
```json
{
  "key": "manual-start",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "manual.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "payload": {}
    }
  }
}
```

### Manual Trigger with Data
```json
{
  "config": {
    "input": {
      "payload": {
        "operation": "process_batch",
        "batch_size": 100,
        "priority": "high"
      }
    }
  }
}
```

## Common Use Cases

- **Testing**: Manual execution of workflows during development
- **Admin Operations**: Trigger maintenance or administrative tasks
- **Data Processing**: Start batch processing jobs on demand
- **Emergency Procedures**: Execute critical workflows when needed
- **Interactive Workflows**: Workflows requiring human decision points
- **One-time Tasks**: Execute workflows for specific situations
