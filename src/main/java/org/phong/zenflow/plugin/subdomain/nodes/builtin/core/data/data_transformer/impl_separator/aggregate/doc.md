# Aggregate Transform Node

## Overview

Performs aggregation functions on array data without grouping, calculating overall statistics and metrics across the entire dataset.

## Node Information

- **Key**: `core:data.aggregate`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:calculator`
- **Tags**: `data`, `aggregate`, `statistics`, `transformation`

## Description

The Aggregate Transform node applies aggregation functions to array data to calculate overall statistics, sums, averages, and other metrics across the entire dataset. Unlike Group By, it produces a single result with multiple aggregated values.

## Input/Output

### Input
- `data` (array, required): The array data to aggregate
- `params` (object, required): Aggregation parameters
  - `aggregations` (array, required): Aggregation functions to apply
    - `field` (string, required): Field to aggregate
    - `function` (string, required): Aggregation function (`count`, `sum`, `avg`, `min`, `max`, `first`, `last`, `concat`, `distinct_count`, `std_dev`)
    - `alias` (string, optional): Alias name for the aggregated field

### Output
- `aggregated_data` (object): Object containing all aggregated results
- `original_count` (integer): Number of items in original array
- `aggregations_applied` (array): List of aggregations performed
- `aggregated_at` (string): ISO timestamp when aggregation was performed

## Usage Examples

### Sales Summary Statistics
```json
{
  "key": "sales-summary",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "data.aggregate",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "data": "{{sales_data}}",
      "params": {
        "aggregations": [
          {
            "field": "amount",
            "function": "sum",
            "alias": "total_revenue"
          },
          {
            "field": "amount",
            "function": "avg",
            "alias": "average_sale"
          },
          {
            "field": "amount",
            "function": "max",
            "alias": "largest_sale"
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
}
```

### Performance Metrics
```json
{
  "input": {
    "data": "{{response_times}}",
    "params": {
      "aggregations": [
        {
          "field": "duration",
          "function": "avg",
          "alias": "avg_response_time"
        },
        {
          "field": "duration",
          "function": "min",
          "alias": "fastest_response"
        },
        {
          "field": "duration",
          "function": "max",
          "alias": "slowest_response"
        },
        {
          "field": "duration",
          "function": "std_dev",
          "alias": "response_time_variance"
        }
      ]
    }
  }
}
```

## Output Example

Input data:
```json
[
  {"amount": 100, "customer_id": "A"},
  {"amount": 200, "customer_id": "B"},
  {"amount": 150, "customer_id": "A"}
]
```

Output:
```json
{
  "aggregated_data": {
    "total_revenue": 450,
    "average_sale": 150,
    "largest_sale": 200,
    "unique_customers": 2
  },
  "original_count": 3,
  "aggregations_applied": ["sum", "avg", "max", "distinct_count"]
}
```

## Common Use Cases

- **Data Summaries**: Calculate overall statistics for datasets
- **KPI Calculation**: Compute key performance indicators
- **Financial Totals**: Sum revenues, costs, and profits
- **Performance Analysis**: Analyze system or user performance metrics
- **Quality Metrics**: Calculate data quality statistics
