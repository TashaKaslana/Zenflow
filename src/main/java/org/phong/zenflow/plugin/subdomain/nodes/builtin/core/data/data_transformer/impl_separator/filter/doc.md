# Filter Transform Node

## Overview

Filters array data based on expressions, supporting both include and exclude modes for flexible data filtering.

## Node Information

- **Key**: `core:data.filter`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:funnel`
- **Tags**: `data`, `filter`, `array`, `transformation`

## Description

The Filter Transform node provides powerful array filtering capabilities using JavaScript-like expressions. It can filter data to include or exclude items based on specified criteria, making it essential for data processing and refinement workflows.

## Input/Output

### Input
- `data` (array, required): The array data to filter
- `params` (object, required): Filter parameters
  - `expression` (string, required): JavaScript expression for filtering (e.g., "item.age > 18")
  - `mode` (string): Filter mode - `include` or `exclude` (default: "include")

### Output
- `filtered_data` (array): The filtered array result
- `original_count` (integer): Number of items in original array
- `filtered_count` (integer): Number of items in filtered array
- `filter_expression` (string): The expression used for filtering
- `filter_mode` (string): The mode used (include/exclude)
- `items_removed` (integer): Number of items filtered out

## Usage Examples

### Filter Active Users
```json
{
  "key": "filter-active-users",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "data.filter",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "data": "{{user_list}}",
      "params": {
        "expression": "item.active === true",
        "mode": "include"
      }
    }
  }
}
```

### Exclude Low Scores
```json
{
  "input": {
    "data": "{{test_results}}",
    "params": {
      "expression": "item.score < 60",
      "mode": "exclude"
    }
  }
}
```

### Filter by Multiple Conditions
```json
{
  "input": {
    "data": "{{orders}}",
    "params": {
      "expression": "item.total > 100 && item.status === 'paid'",
      "mode": "include"
    }
  }
}
```

### Filter by Date Range
```json
{
  "input": {
    "data": "{{transactions}}",
    "params": {
      "expression": "new Date(item.date) >= new Date('2024-01-01')",
      "mode": "include"
    }
  }
}
```

## Expression Examples

### Common Filter Expressions
```javascript
// Numeric comparisons
"item.age >= 18"
"item.price < 100"
"item.quantity > 0"

// String operations
"item.name.includes('admin')"
"item.email.endsWith('@company.com')"
"item.status === 'active'"

// Boolean checks
"item.verified === true"
"item.deleted !== true"

// Array checks
"item.tags.includes('priority')"
"item.permissions.length > 0"

// Complex conditions
"item.score > 80 && item.category === 'premium'"
"item.date > '2024-01-01' || item.priority === 'high'"
```

## Common Use Cases

- **Data Cleanup**: Remove invalid or unwanted records
- **User Segmentation**: Filter users by criteria
- **Order Processing**: Filter orders by status or amount
- **Content Filtering**: Filter content by type or category
- **Quality Control**: Remove items that don't meet standards
- **Security**: Filter out potentially harmful data
