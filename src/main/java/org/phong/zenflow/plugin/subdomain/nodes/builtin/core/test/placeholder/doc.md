# Placeholder Node

## Overview

A simple test node that passes input data through to output, useful for testing workflow structure and data flow patterns.

## Node Information

- **Key**: `core:test.placeholder`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:placeholder`
- **Tags**: `test`, `placeholder`, `passthrough`, `debug`

## Description

The Placeholder node is a utility node primarily used for testing and development. It accepts any input data and passes it through to the output unchanged, making it perfect for testing workflow structure, data flow patterns, and placeholder logic during development.

## Input/Output

### Input
- Any object structure (additionalProperties: true)

### Output
- Same structure as input (passes through unchanged)
- `processed_at` (string): ISO timestamp when node executed
- `node_type` (string): Always "placeholder"

## Usage Examples

### Basic Data Passthrough
```json
{
  "key": "test-placeholder",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "test.placeholder",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "test_data": "example",
      "user_id": 12345,
      "active": true
    }
  }
}
```

### Workflow Testing
```json
{
  "input": {
    "upstream_data": "{{previous_node.output}}",
    "debug_info": {
      "workflow_id": "test-123",
      "step": "validation"
    }
  }
}
```

## Common Use Cases

- **Workflow Testing**: Test workflow structure without actual processing
- **Development**: Placeholder for nodes not yet implemented
- **Debugging**: Examine data flow between nodes
- **Prototyping**: Quick workflow prototyping and validation
- **Load Testing**: Test workflow performance with minimal processing
- **Data Flow Validation**: Verify data structures between workflow steps

## Output Example

Input:
```json
{
  "name": "John",
  "age": 30
}
```

Output:
```json
{
  "name": "John",
  "age": 30
}
```
