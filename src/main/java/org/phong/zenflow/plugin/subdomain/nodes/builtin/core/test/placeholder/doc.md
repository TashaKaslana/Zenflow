# Placeholder Node

## Overview

The Placeholder Node is a utility node designed for testing and development purposes within the Zenflow workflow system. It acts as a simple pass-through node that echoes its input as output while logging all input values, making it ideal for debugging workflows and testing data flow.

## Node Information

- **Key**: `core:placeholder`
- **Version**: `1.0.0`
- **Type**: `data`
- **Icon**: `ph:placeholder`
- **Tags**: `data`, `placeholder`

## Description

A placeholder node that echoes its input as output. This node is particularly useful during workflow development and debugging phases, allowing you to:

- Verify data flow between nodes
- Log input values for inspection
- Act as a temporary replacement for more complex nodes during development
- Test workflow configurations without side effects

## Features

- **Pass-through functionality**: All input data is directly returned as output
- **Comprehensive logging**: Every input key-value pair is logged for debugging
- **Zero side effects**: No data modification or external operations
- **Simple configuration**: No complex setup required

## Input/Output

### Input
- **Type**: `Map<String, Object>`
- **Description**: Accepts any key-value pairs as input
- **Requirements**: None - accepts any valid input structure

### Output
- **Type**: `Map<String, Object>`
- **Description**: Returns the exact same data structure that was provided as input
- **Format**: Identical to input format

## Configuration

The Placeholder node requires no specific configuration. It will process any input provided through the standard workflow configuration.

### Example Configuration

```yaml
nodes:
  - id: "placeholder-test"
    type: "core:placeholder"
    input:
      message: "Hello World"
      number: 42
      array: [1, 2, 3]
      nested:
        key: "value"
```

## Usage Examples

### Example 1: Basic Data Pass-through

**Input:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "age": 30
}
```

**Output:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "age": 30
}
```

**Logs:**
```
INFO - Input username: john_doe
INFO - Input email: john@example.com  
INFO - Input age: 30
```

### Example 2: Complex Data Structures

**Input:**
```json
{
  "user": {
    "profile": {
      "name": "Alice",
      "preferences": ["theme:dark", "lang:en"]
    }
  },
  "metadata": {
    "timestamp": "2025-08-16T10:30:00Z",
    "version": "1.2.3"
  }
}
```

**Output:**
```json
{
  "user": {
    "profile": {
      "name": "Alice",
      "preferences": ["theme:dark", "lang:en"]
    }
  },
  "metadata": {
    "timestamp": "2025-08-16T10:30:00Z",
    "version": "1.2.3"
  }
}
```

## Use Cases

### 1. Workflow Development
Use the Placeholder node as a temporary replacement while developing more complex nodes:

```yaml
workflow:
  nodes:
    - id: "data-source"
      type: "http:request"
    - id: "processing-placeholder"  # Temporary placeholder
      type: "core:placeholder"
    - id: "data-sink"  
      type: "file:write"
```

### 2. Debugging Data Flow
Insert Placeholder nodes between existing nodes to inspect data at specific points:

```yaml
workflow:
  nodes:
    - id: "transform-data"
      type: "data:transform"
    - id: "debug-checkpoint"  # Debug insertion point
      type: "core:placeholder"
    - id: "save-results"
      type: "database:save"
```

### 3. Testing Workflow Structure
Validate workflow connectivity and configuration without performing actual operations:

```yaml
workflow:
  nodes:
    - id: "mock-api"
      type: "core:placeholder"
      input:
        status: "success"
        data: ["item1", "item2"]
    - id: "process-results"
      type: "core:placeholder"
```

## Error Handling

The Placeholder node is designed to be robust and will:
- Accept any valid input structure
- Never fail during execution
- Always return a successful execution result
- Log any issues during input conversion

## Performance Considerations

- **Minimal overhead**: Simple pass-through operation
- **Memory efficient**: No data duplication or transformation
- **Fast execution**: Immediate input-to-output mapping
- **Safe for large datasets**: No processing or modification of data

## Integration

The Placeholder node integrates seamlessly with all other Zenflow nodes and can be placed anywhere in a workflow where a data transformation step is expected.

### Chaining Example

```yaml
workflow:
  nodes:
    - id: "input"
      type: "core:placeholder"
      input:
        rawData: "user input"
    - id: "validate"  
      type: "core:placeholder"  # Temporary validation placeholder
    - id: "output"
      type: "core:placeholder"
```

## Troubleshooting

### Common Issues

1. **No output visible**: Check logs for input data confirmation
2. **Unexpected data structure**: Verify input format matches expectations
3. **Missing logs**: Ensure logging level is set to INFO or DEBUG

### Log Analysis

Monitor the execution logs to see all input data:
```
INFO - Input key1: value1
INFO - Input key2: value2
```

## Technical Implementation

- **Executor**: `PlaceholderExecutor`
- **Interface**: Implements `PluginNodeExecutor`
- **Conversion**: Uses `ObjectConversion.convertObjectToMap()` for input processing
- **Logging**: Utilizes `LogCollector` for structured logging
- **Result**: Returns `ExecutionResult.success()` with original input and logs

## Version History

### 1.0.0
- Initial implementation
- Basic pass-through functionality  
- Input logging capabilities
- Integration with Zenflow plugin system

## Related Documentation

[//]: # (- [Plugin Node Development Guide]&#40;../../../../../../../docs/plugin-node-sample.md&#41;)

[//]: # (- [Data Transformer Documentation]&#40;../../../../../../../docs/data-transformer.md&#41;)

[//]: # (- [Workflow Configuration Reference]&#40;../../../../../../workflow/README.md&#41;)
