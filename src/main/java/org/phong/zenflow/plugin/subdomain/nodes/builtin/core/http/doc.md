# HTTP Request Node

## Overview

Executes HTTP requests to external APIs and services with full support for all HTTP methods, headers, and request bodies.

## Node Information

- **Key**: `core:http.request`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:globe`
- **Tags**: `http`, `request`, `network`, `api`

## Description

The HTTP Request node enables workflows to communicate with external APIs and web services. It supports all standard HTTP methods and provides comprehensive request/response handling with automatic JSON parsing, custom headers, and detailed response metadata.

## Input/Output

### Input
- `url` (string, required): The destination URL for the HTTP request
- `method` (string, required): HTTP method - `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, or `OPTIONS`
- `body` (object, optional): Request payload for methods like POST, PUT, PATCH
- `headers` (object, optional): Custom HTTP headers as key-value pairs
- `timeout` (integer, optional): Request timeout in milliseconds (default: 30000)
- `follow_redirects` (boolean, optional): Whether to follow HTTP redirects (default: true)
- `verify_ssl` (boolean, optional): Whether to verify SSL certificates (default: true)

### Output
- `status_code` (integer): HTTP status code from the response
- `headers` (object): Response headers as key-value pairs
- `body` (object/string): Response body, automatically parsed as JSON when possible
- `response_time` (integer): Response time in milliseconds
- `content_type` (string): Response content type
- `content_length` (integer): Response content length in bytes
- `redirected` (boolean): Whether the request was redirected
- `final_url` (string): Final URL after any redirects

## Usage Examples

### Simple GET Request
```json
{
  "key": "fetch-data",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "http.request",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "url": "https://api.example.com/users",
      "method": "GET"
    }
  }
}
```

### POST with JSON Body and Headers
```json
{
  "input": {
    "url": "https://api.example.com/users",
    "method": "POST",
    "headers": {
      "Content-Type": "application/json",
      "Authorization": "Bearer {{auth.token}}"
    },
    "body": {
      "name": "{{user.name}}",
      "email": "{{user.email}}"
    }
  }
}
```

### Advanced Configuration
```json
{
  "input": {
    "url": "https://slow-api.example.com/data",
    "method": "GET",
    "timeout": 60000,
    "verify_ssl": false,
    "headers": {
      "User-Agent": "Zenflow/1.0",
      "Accept": "application/json"
    }
  }
}
```

## Response Handling

### Success Response (200-299)
```json
{
  "status_code": 200,
  "body": {"result": "success", "data": ["..."] },
  "headers": {"content-type": "application/json"},
  "response_time": 245
}
```

### Error Response (400+)
```json
{
  "status_code": 404,
  "body": {"error": "Not found"},
  "headers": {"content-type": "application/json"},
  "response_time": 123
}
```

## Common Use Cases

- **API Integration**: Call REST APIs and GraphQL endpoints
- **Data Fetching**: Retrieve data from external services
- **Webhooks**: Send notifications to external systems
- **Authentication**: Authenticate with OAuth and token-based services
- **File Upload**: Upload files to external services
- **Health Checks**: Monitor external service availability
- **Third-party Integration**: Connect with CRM, payment, and other business systems

## Error Handling

The node handles various error scenarios:
- Network timeouts and connection failures
- Invalid SSL certificates (when verification enabled)
- DNS resolution failures
- HTTP error status codes (4xx, 5xx)
- Invalid JSON response parsing

## Security Considerations

- Always use HTTPS for sensitive data
- Validate SSL certificates in production
- Store API keys and tokens securely
- Sanitize request URLs and headers
- Be cautious with user-provided URLs
