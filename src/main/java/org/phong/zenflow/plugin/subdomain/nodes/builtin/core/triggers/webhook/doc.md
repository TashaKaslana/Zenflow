# Webhook Trigger Node

## Overview

Triggers a workflow when an HTTP request is received at the webhook endpoint. This is the primary entry point for external systems to initiate workflow execution.

## Node Information

- **Key**: `core:webhook.trigger`
- **Version**: `1.0.0`
- **Type**: `trigger`
- **Icon**: `ph:webhook`
- **Tags**: `trigger`, `webhook`, `http`, `external`

## Description

The Webhook Trigger node listens for incoming HTTP requests and starts workflow execution when a request is received. It captures all relevant request information including payload, headers, HTTP method, and metadata for use in subsequent workflow nodes.

## Input/Output

### Input (Webhook Request)
- `payload` (object): The webhook request body/payload containing the data sent by the external system
- `headers` (object): HTTP headers from the webhook request as key-value pairs
- `http_method` (string): HTTP method used for the webhook request (GET, POST, PUT, DELETE, etc.)
- `source_ip` (string): IP address of the client making the webhook request
- `user_agent` (string): User agent string from the webhook request
- `webhook_id` (string): Unique identifier for this specific webhook instance

### Output
- `trigger_type` (string): Always "webhook" for webhook triggers
- `triggered_at` (string): ISO timestamp when the webhook was triggered
- `trigger_source` (string): Source of the trigger (webhook URL or identifier)
- `http_method` (string): HTTP method used in the request
- `payload` (object): The original request payload passed through
- `headers` (object): Request headers passed through
- `source_ip` (string): Client IP address
- `user_agent` (string): Client user agent

## Usage Examples

### Basic Webhook
```json
{
  "key": "webhook-start",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "webhook.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "payload": {"action": "process", "data": "example"},
      "user_agent": "ExternalSystem/1.0"
    }
  }
}
```

### Accessing Webhook Data in Next Nodes
```json
{
  "input": {
    "user_id": "{{webhook-start.output.payload.user_id}}",
    "timestamp": "{{webhook-start.output.triggered_at}}",
    "source": "{{webhook-start.output.source_ip}}"
  }
}
```

## Security Considerations

- Webhook endpoints are publicly accessible
- Validate and sanitize all incoming payload data
- Consider implementing webhook signature verification
- Monitor for unusual traffic patterns or abuse
- Use HTTPS for sensitive data transmission

## Common Use Cases

- **API Integration**: Receive data from external APIs and services
- **Event Processing**: Process events from third-party systems
- **Data Ingestion**: Accept incoming data for processing
- **Notification Handling**: Process notifications from external services
- **Automation Triggers**: Start workflows based on external events
