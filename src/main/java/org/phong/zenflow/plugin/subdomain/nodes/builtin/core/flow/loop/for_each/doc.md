# For Each Loop Node

## Overview

Iterates over arrays or collections, executing a set of nodes for each item with access to the current item and index.

## Node Information

- **Key**: `core:flow.for_each`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:arrows-clockwise`
- **Tags**: `flow`, `loop`, `iteration`, `foreach`

## Description

The For Each Loop node enables iteration over arrays, objects, or collections in workflows. It executes a specified set of nodes for each item, providing access to the current item, index, and loop metadata. Perfect for batch processing, data transformation, and repetitive operations.

## Input/Output

### Input
- `collection` (array/object, required): The collection to iterate over
- `item_variable` (string): Variable name for current item (default: "item")
- `index_variable` (string): Variable name for current index (default: "index") 
- `loop_body` (array): Node(s) to execute for each iteration
- `max_iterations` (integer): Maximum number of iterations (safety limit)
- `parallel` (boolean): Whether to execute iterations in parallel (default: false)

### Output
- `total_iterations` (integer): Total number of iterations completed
- `successful_iterations` (integer): Number of successful iterations
- `failed_iterations` (integer): Number of failed iterations
- `results` (array): Results from each iteration
- `execution_time` (integer): Total execution time in milliseconds
- `loop_completed_at` (string): ISO timestamp when loop completed

## Usage Examples

### Process User Array
```json
{
  "key": "process-users",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "flow.for_each",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "collection": "{{user_list.users}}",
      "item_variable": "current_user",
      "index_variable": "user_index",
      "loop_body": ["validate-user", "send-notification"]
    }
  }
}
```

### Batch Data Processing
```json
{
  "input": {
    "collection": "{{database_records}}",
    "item_variable": "record",
    "loop_body": ["transform-record", "save-to-external-system"],
    "max_iterations": 1000,
    "parallel": true
  }
}
```

### File Processing
```json
{
  "input": {
    "collection": "{{uploaded_files}}",
    "item_variable": "file",
    "index_variable": "file_number",
    "loop_body": ["validate-file", "process-file", "store-result"]
  }
}
```

## Accessing Loop Variables

Within loop body nodes, access current item and index:
```json
{
  "input": {
    "user_id": "{{current_user.id}}",
    "user_email": "{{current_user.email}}",
    "processing_order": "{{user_index}}"
  }
}
```

## Common Use Cases

- **Batch Processing**: Process arrays of data records
- **User Operations**: Send emails or notifications to multiple users
- **File Processing**: Handle multiple uploaded files
- **Data Transformation**: Transform array items individually
- **API Calls**: Make API calls for each item in a collection
- **Validation**: Validate multiple items with same logic
