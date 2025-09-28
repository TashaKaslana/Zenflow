# Group By Transform Node

## Overview

Groups array data by specified fields and performs aggregation functions on grouped data, similar to SQL GROUP BY operations.

## Node Information

- **Key**: `core:data.group_by`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:rows`
- **Tags**: `data`, `groupby`, `aggregation`, `transformation`

## Description

The Group By Transform node groups array data by one or more fields and applies aggregation functions to calculate statistics, sums, averages, and other metrics for each group. It provides SQL-like GROUP BY functionality for workflow data processing.

## Input/Output

### Input
- `data` (array, required): The array data to group
- `params` (object, required): Grouping parameters
  - `groupBy` (string|array, required): Field(s) to group by
  - `aggregations` (array, optional): Aggregation functions to apply
    - `field` (string, required): Field to aggregate
    - `function` (string, required): Aggregation function (`count`, `sum`, `avg`, `min`, `max`, `first`, `last`, `concat`, `distinct_count`, `std_dev`)
    - `alias` (string, optional): Alias name for the aggregated field

### Output
- `grouped_data` (array): Array of grouped results
- `group_count` (integer): Number of groups created
- `original_count` (integer): Number of items in original array
- `grouping_fields` (array): Fields used for grouping
- `aggregations_applied` (array): List of aggregations performed

## Usage Examples

### Group Sales by Region
```json
{
  "key": "group-sales",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "data.group_by",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "data": "{{sales_data}}",
      "params": {
        "groupBy": "region",
        "aggregations": [
          {
            "field": "amount",
            "function": "sum",
            "alias": "total_sales"
          },
          {
            "field": "amount",
            "function": "avg",
            "alias": "average_sale"
          },
          {
            "field": "id",
            "function": "count",
            "alias": "sale_count"
          }
        ]
      }
    }
  }
}
```

### Multi-field Grouping
```json
{
  "input": {
    "data": "{{orders}}",
    "params": {
      "groupBy": ["category", "status"],
      "aggregations": [
        {
          "field": "total",
          "function": "sum",
          "alias": "category_total"
        },
        {
          "field": "customer_id",
          "function": "distinct_count",
          "alias": "unique_customers"
        }
      ]
    }
  }
}
```

### User Activity Analysis
```json
{
  "input": {
    "data": "{{user_activities}}",
    "params": {
      "groupBy": "user_id",
      "aggregations": [
        {
          "field": "action",
          "function": "count",
          "alias": "total_actions"
        },
        {
          "field": "timestamp",
          "function": "first",
          "alias": "first_activity"
        },
        {
          "field": "timestamp",
          "function": "last",
          "alias": "last_activity"
        }
      ]
    }
  }
}
```

## Aggregation Functions

- **count**: Count of items in each group
- **sum**: Sum of numeric values
- **avg**: Average of numeric values
- **min**: Minimum value
- **max**: Maximum value
- **first**: First value in the group
- **last**: Last value in the group
- **concat**: Concatenate string values
- **distinct_count**: Count of unique values
- **std_dev**: Standard deviation of numeric values

## Output Example

Input data:
```json
[
  {"region": "North", "amount": 100, "salesperson": "John"},
  {"region": "North", "amount": 150, "salesperson": "Jane"},
  {"region": "South", "amount": 200, "salesperson": "Bob"}
]
```

Output:
```json
{
  "grouped_data": [
    {
      "region": "North",
      "total_sales": 250,
      "average_sale": 125,
      "sale_count": 2
    },
    {
      "region": "South", 
      "total_sales": 200,
      "average_sale": 200,
      "sale_count": 1
    }
  ]
}
```

## Common Use Cases

- **Sales Analytics**: Group sales by region, product, or time period
- **User Behavior**: Analyze user activities and patterns
- **Financial Reporting**: Aggregate financial data by categories
- **Performance Metrics**: Calculate KPIs grouped by dimensions
- **Data Summarization**: Create summary statistics from detailed data
