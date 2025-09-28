# Number Validator Node

## Overview

Validates numeric values against specified thresholds and criteria, useful for testing numeric data validation workflows.

## Node Information

- **Key**: `core:test.number_validator`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:hash`
- **Tags**: `test`, `validation`, `number`, `threshold`

## Description

The Number Validator node validates integer values against configurable thresholds and criteria. It's designed for testing numeric validation logic and ensuring data quality in workflows that process numeric data.

## Input/Output

### Input
- `number` (integer, required): The number to validate
- `threshold` (integer, required): The validation threshold value
- `validation_type` (string, optional): Type of validation - `greater_than`, `less_than`, `equal`, `range` (default: "greater_than")
- `strict` (boolean, optional): Whether to use strict comparison (default: false)

### Output
- `valid` (boolean): Whether the number passed validation
- `input_number` (integer): The original number that was validated
- `threshold` (integer): The threshold used for validation
- `validation_type` (string): The type of validation performed
- `validation_message` (string): Human-readable validation result message
- `validated_at` (string): ISO timestamp when validation occurred

## Usage Examples

### Basic Threshold Validation
```json
{
  "key": "validate-score",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "test.number_validator",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "number": 85,
      "threshold": 70,
      "validation_type": "greater_than"
    }
  }
}
```

### Dynamic Validation from Workflow Data
```json
{
  "input": {
    "number": "{{user_score.value}}",
    "threshold": "{{system_config.min_passing_score}}",
    "validation_type": "greater_than",
    "strict": true
  }
}
```

### Range Validation
```json
{
  "input": {
    "number": "{{temperature_reading}}",
    "threshold": 100,
    "validation_type": "less_than"
  }
}
```

## Validation Types

- **greater_than**: Number must be greater than threshold
- **less_than**: Number must be less than threshold
- **equal**: Number must equal threshold exactly
- **not_equal**: Number must not equal threshold
- **range**: Number must be within acceptable range

## Output Examples

### Successful Validation
```json
{
  "valid": true,
  "input_number": 85,
  "threshold": 70,
  "validation_type": "greater_than",
  "validation_message": "Number 85 is greater than threshold 70",
  "validated_at": "2024-01-15T10:30:00Z"
}
```

### Failed Validation
```json
{
  "valid": false,
  "input_number": 45,
  "threshold": 70,
  "validation_type": "greater_than",
  "validation_message": "Number 45 is not greater than threshold 70",
  "validated_at": "2024-01-15T10:30:00Z"
}
```

## Common Use Cases

- **Score Validation**: Validate test scores, ratings, or performance metrics
- **Threshold Monitoring**: Check if values exceed operational thresholds
- **Data Quality**: Ensure numeric data meets business requirements
- **Testing**: Test numeric validation logic in workflows
- **Range Checking**: Validate that values fall within acceptable ranges
