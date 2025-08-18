# Text Processor Node

## Overview

Processes and transforms text data with various string manipulation operations like trimming, case conversion, and formatting.

## Node Information

- **Key**: `core:test.text_processor`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:text-aa`
- **Tags**: `test`, `text`, `string`, `processing`

## Description

The Text Processor node provides various text manipulation capabilities for testing and development. It can perform operations like trimming whitespace, case conversion, string concatenation, and other common text transformations.

## Input/Output

### Input
- `text` (string, required): The text to process
- `operations` (array): List of operations to apply in sequence
  - `trim`: Remove leading/trailing whitespace
  - `uppercase`: Convert to uppercase
  - `lowercase`: Convert to lowercase
  - `capitalize`: Capitalize first letter
  - `reverse`: Reverse the string
  - `length`: Get string length

### Output
- `original_text` (string): The original input text
- `processed_text` (string): The text after all operations
- `operations_applied` (array): List of operations that were applied
- `character_count` (integer): Length of processed text
- `processing_time` (integer): Processing time in milliseconds

## Usage Examples

### Basic Text Processing
```json
{
  "key": "process-text",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "test.text_processor",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "text": "  hello world  ",
      "operations": ["trim", "uppercase"]
    }
  }
}
```

### Multi-step Processing
```json
{
  "input": {
    "text": "{{user_input.message}}",
    "operations": ["trim", "lowercase", "capitalize"]
  }
}
```

## Common Use Cases

- **Data Cleaning**: Sanitize user input text
- **Testing**: Test text processing workflows
- **Format Validation**: Ensure consistent text formatting
- **Development**: Prototype text manipulation logic
