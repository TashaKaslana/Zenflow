# Data Generator Node

## Overview

Generates test data in various formats using a random seed for reproducible results. Useful for testing, development, and demonstration purposes.

## Node Information

- **Key**: `core:data.generate`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:database`
- **Tags**: `data`, `generator`, `test`, `mock`

## Description

The Data Generator node creates structured test data in JSON, XML, or CSV format. It uses a seed value to ensure reproducible data generation, making it ideal for testing scenarios where consistent data is needed across multiple runs.

## Input/Output

### Input
- `seed` (integer, required): Random seed number for deterministic data generation
- `format` (string, required): Output format - one of `json`, `xml`, or `csv`

### Output
- `generated_data` (object/string): The generated data in the specified format
- `format` (string): The format used for generation
- `seed` (integer): The seed value used
- `record_count` (integer): Number of records generated
- `generated_at` (string): ISO timestamp when data was generated

## Usage Examples

### Basic JSON Data Generation
```json
{
  "key": "data-generator",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "data.generate",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "seed": 1234,
      "format": "json"
    }
  }
}
```

### Using Dynamic Seed from Previous Node
```json
{
  "input": {
    "seed": "{{webhook-start.output.payload.seed}}",
    "format": "json"
  }
}
```

## Generated Data Structure

The generator creates realistic test data including:
- User profiles (names, emails, addresses)
- Transaction records
- Product information
- Timestamps and IDs
- Numerical data and statistics

## Common Use Cases

- **Testing**: Generate consistent test data for unit and integration tests
- **Development**: Populate development databases with sample data
- **Demos**: Create realistic data for demonstrations
- **Load Testing**: Generate bulk data for performance testing
- **Data Pipeline Testing**: Test data transformation workflows
