# Merge Data Node

## Overview

Combines multiple data sources into a single unified dataset with configurable merge strategies and conflict resolution.

## Node Information

- **Key**: `core:data.merge`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:arrows-merge`
- **Tags**: `data`, `merge`, `combine`, `transformation`

## Description

The Merge Data node combines data from multiple sources using various merge strategies. It supports object merging, array concatenation, and priority-based conflict resolution, making it ideal for combining data from different workflow nodes or external sources.

## Input/Output

### Input
- `sources` (array, required): Array of data sources to merge
  - `data` (any, required): The actual data to merge (object, array, or primitive)
  - `name` (string, optional): Optional name for this data source
  - `priority` (integer, optional): Priority for conflict resolution (higher = priority, default: 0)
- `strategy` (string): Merge strategy - `deep_merge`, `shallow_merge`, `concat`, `replace` (default: "deep_merge")
- `conflict_resolution` (string): How to handle conflicts - `priority`, `first_wins`, `last_wins` (default: "priority")

### Output
- `merged_data` (any): The combined result of all input sources
- `source_count` (integer): Number of sources that were merged
- `conflicts_resolved` (integer): Number of conflicts encountered and resolved
- `merge_strategy` (string): The strategy used for merging
- `merged_at` (string): ISO timestamp when merge was performed

## Usage Examples

### Basic Object Merge
```json
{
  "key": "merge-config",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "data.merge",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "sources": [
        {
          "name": "user_data",
          "data": {"name": "John", "email": "john@example.com"},
          "priority": 1
        },
        {
          "name": "profile_data", 
          "data": {"age": 30, "city": "New York"},
          "priority": 2
        }
      ],
      "strategy": "deep_merge"
    }
  }
}
```

### Priority-Based Conflict Resolution
```json
{
  "input": {
    "sources": [
      {
        "name": "default_config",
        "data": {"timeout": 30, "retries": 3, "debug": false},
        "priority": 1
      },
      {
        "name": "user_config",
        "data": {"timeout": 60, "debug": true},
        "priority": 2
      }
    ],
    "conflict_resolution": "priority"
  }
}
```

### Array Concatenation
```json
{
  "input": {
    "sources": [
      {
        "data": ["item1", "item2"]
      },
      {
        "data": ["item3", "item4"]
      }
    ],
    "strategy": "concat"
  }
}
```

### Workflow Data Merge
```json
{
  "input": {
    "sources": [
      {
        "name": "api_response",
        "data": "{{http_call.output.body}}",
        "priority": 1
      },
      {
        "name": "database_data",
        "data": "{{db_query.output.result_set}}",
        "priority": 2
      },
      {
        "name": "static_data",
        "data": {"source": "workflow", "processed_at": "{{current_timestamp}}"},
        "priority": 0
      }
    ]
  }
}
```

## Merge Strategies

### Deep Merge
Recursively merges nested objects and arrays:
```
Source 1: {"user": {"name": "John", "age": 25}}

Source 2: {"user": {"email": "john@example.com", "age": 30}}

Result: {"user": {"name": "John", "age": 30, "email": "john@example.com"}}
```

### Shallow Merge
Only merges top-level properties:
```
Source 1: {"user": {"name": "John"}, "active": true}

Source 2: {"user": {"email": "john@example.com"}, "role": "admin"}

Result: {"user": {"email": "john@example.com"}, "active": true, "role": "admin"}
```

### Concatenation
Combines arrays by concatenating elements:
```
Source 1: ["a", "b"]

Source 2: ["c", "d"]

Result: ["a", "b", "c", "d"]
```

### Replace
Last source completely replaces previous values:
```
Source 1: {"name": "John", "age": 25}

Source 2: {"name": "Jane"}

Result: {"name": "Jane"}
```

## Conflict Resolution

- **Priority**: Higher priority sources win conflicts
- **First Wins**: First encountered value is kept
- **Last Wins**: Last encountered value overwrites previous

## Common Use Cases

- **Configuration Merging**: Combine default and user-specific settings
- **Data Aggregation**: Combine data from multiple APIs or databases
- **Profile Building**: Merge user data from different sources
- **Report Generation**: Combine data for comprehensive reports
- **Feature Flags**: Merge feature configurations with defaults
- **Multi-source Integration**: Combine data from different systems
