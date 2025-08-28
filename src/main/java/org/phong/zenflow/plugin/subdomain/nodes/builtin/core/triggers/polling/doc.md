# Polling Trigger Node

## Overview

Automatically polls HTTP endpoints at regular intervals and triggers workflows when changes are detected, enabling integration with APIs that don't support webhooks or real-time events.

## Node Information

- **Key**: `core:polling.trigger`
- **Version**: `1.0.0`
- **Type**: `trigger`
- **Trigger Type**: `polling`
- **Icon**: `ph:arrow-clockwise`
- **Tags**: `trigger`, `polling`, `http`, `schedule`, `api`, `quartz`

## Description

The Polling Trigger node monitors HTTP endpoints by making periodic requests and detecting changes in responses. It uses Quartz scheduler for reliable timing and supports various change detection strategies. When changes are detected, it triggers workflows with detailed information about what changed.

Perfect for integrating with REST APIs, RSS feeds, file systems, or any HTTP-accessible data sources that don't provide real-time notifications.

## Features

- **Multiple Change Detection Strategies**: Full response comparison, hash-based, or size-based detection
- **JSONPath Support**: Extract specific fields for targeted monitoring
- **Flexible HTTP Methods**: Support for GET, POST, PUT, PATCH, and HEAD requests
- **Resource Management**: Automatic cleanup and efficient caching using generic resource management
- **Quartz Integration**: Reliable scheduling with persistence and clustering support
- **Error Handling**: Graceful handling of HTTP errors and timeouts

## Input Configuration

### Required Parameters
- `url` (string): The HTTP endpoint URL to poll for changes
- `interval_seconds` (integer): Polling interval in seconds (minimum 1)

### Optional Parameters
- `http_method` (string): HTTP method to use - GET, POST, PUT, PATCH, HEAD (default: "GET")
- `change_detection` (string): Detection strategy - "full_response", "hash_comparison", "size_change" (default: "full_response")
- `json_path` (string): JSONPath expression to extract specific data (e.g., "$.data.items")
- `headers` (object): HTTP headers to include in requests
- `request_body` (any): Request body for POST/PUT requests
- `timeout_seconds` (integer): Request timeout in seconds, 1-300 (default: 30)
- `include_response` (boolean): Include full response data in payload (default: true)

## Output Data

### Standard Fields
- `trigger_type`: Always "polling"
- `triggered_at`: ISO timestamp when change was detected
- `trigger_source`: Always "polling_change_detected"
- `scheduler_type`: Always "quartz"

### Polling-Specific Fields
- `polling_url`: The URL that was polled
- `change_type`: Type of change detected - "initial_data", "data_changed", "items_added", "items_removed", "items_modified", "data_removed"
- `current_response`: Current response data (if include_response is true)
- `previous_response`: Previous response data for comparison
- `extracted_data`: Data extracted using JSONPath (if json_path specified)
- `polling_method`: HTTP method used
- `detection_strategy`: Change detection strategy used

## Change Detection Strategies

### Full Response (`full_response`)
Compares entire response objects for exact differences. Most accurate but uses more memory.

```json
{
  "change_detection": "full_response"
}
```

### Hash Comparison (`hash_comparison`)
Compares hash codes of responses. Faster and memory-efficient but may have rare false positives.

```json
{
  "change_detection": "hash_comparison"
}
```

### Size Change (`size_change`)
Detects changes in collection sizes or string lengths. Best for monitoring lists or content length.

```json
{
  "change_detection": "size_change"
}
```

## Usage Examples

### Basic API Monitoring
```json
{
  "key": "api-monitor",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "polling.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "url": "https://api.example.com/data",
      "interval_seconds": 60,
      "change_detection": "full_response"
    }
  }
}
```

### RSS Feed Monitoring
```json
{
  "key": "rss-feed-monitor",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "polling.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "url": "https://blog.example.com/feed.xml",
      "interval_seconds": 300,
      "change_detection": "size_change",
      "timeout_seconds": 60
    }
  }
}
```

### Authenticated API with JSONPath
```json
{
  "key": "github-commits-monitor",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "polling.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "url": "https://api.github.com/repos/owner/repo/commits",
      "interval_seconds": 120,
      "headers": {
        "Authorization": "Bearer YOUR_TOKEN",
        "Accept": "application/vnd.github.v3+json"
      },
      "json_path": "$[0].sha",
      "change_detection": "full_response"
    }
  }
}
```

### POST Request Monitoring
```json
{
  "key": "search-results-monitor",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "polling.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "url": "https://api.example.com/search",
      "interval_seconds": 180,
      "http_method": "POST",
      "request_body": {
        "query": "trending topics",
        "limit": 10
      },
      "headers": {
        "Content-Type": "application/json",
        "X-API-Key": "your-api-key"
      },
      "json_path": "$.results",
      "change_detection": "size_change"
    }
  }
}
```

### Database API Monitoring
```json
{
  "key": "database-changes-monitor",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "polling.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "url": "https://api.database.com/v1/tables/users/count",
      "interval_seconds": 30,
      "headers": {
        "Authorization": "Bearer DB_ACCESS_TOKEN"
      },
      "json_path": "$.count",
      "change_detection": "full_response",
      "timeout_seconds": 15
    }
  }
}
```

## Best Practices

### Performance Optimization
- Use `hash_comparison` for large responses to reduce memory usage
- Use `size_change` when only monitoring collection/content size
- Set appropriate `timeout_seconds` based on API response times
- Use `json_path` to monitor only relevant data fields

### API Considerations
- Respect API rate limits when setting `interval_seconds`
- Include proper authentication headers when required
- Use appropriate HTTP methods for your use case
- Set `include_response: false` for large responses when full data isn't needed

### Change Detection
- Use `full_response` for precise change detection
- Use `json_path` to focus on specific fields and avoid false positives from timestamps
- Consider API-specific fields like `last_modified` or `etag` in your JSONPath

### Error Handling
- The trigger handles HTTP errors gracefully and continues polling
- Check logs for persistent connection issues
- Adjust `timeout_seconds` if requests frequently timeout

## Common Use Cases

1. **E-commerce Price Monitoring**: Track product prices and inventory
2. **News/RSS Feeds**: Monitor for new articles or posts
3. **API Status Monitoring**: Check service health endpoints
4. **GitHub Repository Monitoring**: Watch for new commits or releases
5. **Database Change Tracking**: Monitor record counts or recent updates
6. **Social Media Monitoring**: Track mentions, hashtags, or followers
7. **Weather Data**: Poll weather APIs for condition changes
8. **Stock Price Tracking**: Monitor financial data APIs
9. **File System APIs**: Check for new file uploads or changes
10. **IoT Sensor Data**: Poll device endpoints for sensor readings

## Integration with Other Nodes

The polling trigger works seamlessly with other workflow nodes:

- **Condition Nodes**: Filter changes based on specific criteria
- **Transform Nodes**: Process and format the detected changes
- **Notification Nodes**: Send alerts when changes occur
- **Database Nodes**: Store historical data about changes
- **HTTP Request Nodes**: Make additional API calls based on changes

## Troubleshooting

### Common Issues
- **No changes detected**: Verify the endpoint returns different data, check JSONPath syntax
- **Too many false positives**: Use JSONPath to exclude timestamp fields, consider hash_comparison
- **High memory usage**: Use hash_comparison or size_change detection strategies
- **Timeout errors**: Increase timeout_seconds or check network connectivity
- **Authentication failures**: Verify API keys and headers are correct

### Debugging Tips
- Start with a short interval for testing, then increase for production
- Use `include_response: true` during setup to see what data is being compared
- Test JSONPath expressions with sample data before deployment
- Monitor logs for HTTP errors and connection issues
