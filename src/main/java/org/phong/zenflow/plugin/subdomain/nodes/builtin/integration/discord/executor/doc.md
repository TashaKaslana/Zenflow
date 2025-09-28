# Discord Send Message Node

## Overview

Sends messages to Discord channels using the Discord API. Supports rich embeds, replies, and text-to-speech messages while efficiently sharing JDA connections across multiple workflow executions.

## Node Information

- **Key**: `integration:discord.message.send`
- **Version**: `1.0.0`
- **Type**: `integration.message`
- **Icon**: `simple-icons:discord`
- **Tags**: `integration`, `discord`, `message`, `send`

## Description

The Discord Send Message node allows workflows to send messages to Discord channels with full support for Discord's rich formatting features including embeds, fields, images, and colors. It uses the same efficient JDA resource management as the Discord Message Trigger for optimal performance and resource utilization.

Perfect for building Discord bots, automated notifications, alerts, reports, and interactive workflow responses.

## Features

- **Rich Embeds**: Full Discord embed support with titles, descriptions, fields, images
- **Color Customization**: Hex colors, named colors, or decimal color values
- **Resource Efficiency**: Shares JDA connections across multiple message sends
- **Reply Support**: Reply to specific messages using message IDs
- **Text-to-Speech**: Optional TTS message delivery
- **Error Handling**: Comprehensive validation and error reporting
- **Flexible Formatting**: Support for Discord markdown and formatting

## Input Configuration

### Required Parameters
- `bot_token` (string): Discord bot token for authentication
- `channel_id` (string): Discord channel ID where message should be sent
- `message` (string): Text content of the message (1-2000 characters)

### Optional Parameters
- `embed` (object): Rich embed object with formatting options
- `reply_to` (string): Message ID to reply to
- `tts` (boolean): Send as text-to-speech message (default: false)

### Embed Configuration
When using embeds, you can include:
- `title` (string): Embed title (max 256 characters)
- `description` (string): Embed description (max 4096 characters)
- `color` (string): Color as hex (#FF0000), name (red), or decimal
- `thumbnail` (string): URL for thumbnail image
- `image` (string): URL for main image
- `footer` (string): Footer text (max 2048 characters)
- `fields` (array): Up to 25 fields with name, value, and inline properties
- `author` (object): Author info with name, URL, and icon
- `url` (string): URL that the title links to
- `timestamp` (string): ISO timestamp for the embed

## Output Data

- `message_id`: ID of the sent Discord message
- `channel_id`: Channel where message was sent
- `timestamp`: ISO timestamp when message was sent
- `content`: The message content that was sent
- `embed_sent`: Whether an embed was included
- `guild_id`: Server (guild) ID where message was sent
- `author`: Bot user information

## Usage Examples

### Simple Text Message
```json
{
  "key": "send-notification",
  "type": "ACTION",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.send",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "message": "Hello from workflow! 👋"
    }
  }
}
```

### Rich Embed with Fields
```json
{
  "key": "status-report",
  "type": "ACTION",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.send",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "message": "System Status Report",
      "embed": {
        "title": "📊 System Health Check",
        "description": "Automated system status report",
        "color": "#00ff00",
        "thumbnail": "https://example.com/status-icon.png",
        "fields": [
          {
            "name": "🚀 API Status",
            "value": "Operational",
            "inline": true
          },
          {
            "name": "💾 Database",
            "value": "Healthy",
            "inline": true
          },
          {
            "name": "⏰ Last Check",
            "value": "2 minutes ago",
            "inline": true
          }
        ],
        "footer": "Automated by Zenflow",
        "timestamp": "2025-08-28T10:30:00Z"
      }
    }
  }
}
```

### Alert with Color Coding
```json
{
  "key": "error-alert",
  "type": "ACTION",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.send",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "message": "⚠️ Critical Alert",
      "embed": {
        "title": "System Error Detected",
        "description": "An error has occurred in the payment processing system",
        "color": "red",
        "fields": [
          {
            "name": "Error Code",
            "value": "PAY_001",
            "inline": true
          },
          {
            "name": "Affected Service",
            "value": "Payment Gateway",
            "inline": true
          },
          {
            "name": "Severity",
            "value": "High",
            "inline": true
          }
        ],
        "footer": "Please investigate immediately"
      }
    }
  }
}
```

### Reply to Message
```json
{
  "key": "reply-response",
  "type": "ACTION",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.send",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "message": "Thanks for your message! I've processed your request.",
      "reply_to": "987654321098765432"
    }
  }
}
```

### Welcome Message with Author Info
```json
{
  "key": "welcome-message",
  "type": "ACTION",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.send",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "message": "Welcome to our server!",
      "embed": {
        "title": "🎉 Welcome!",
        "description": "Thanks for joining our community!",
        "color": "#7289da",
        "author": {
          "name": "Community Bot",
          "icon_url": "https://example.com/bot-avatar.png"
        },
        "image": "https://example.com/welcome-banner.png",
        "fields": [
          {
            "name": "📋 Getting Started",
            "value": "Check out #rules and #announcements",
            "inline": false
          },
          {
            "name": "💬 Need Help?",
            "value": "Ask in #support",
            "inline": true
          },
          {
            "name": "🎮 Have Fun!",
            "value": "Enjoy your stay",
            "inline": true
          }
        ]
      }
    }
  }
}
```

## Color Options

### Hex Colors
```json lines
{"color": "#ff0000"}  // Red
{"color": "#00ff00"}  // Green
{"color": "#0099ff"}  // Blue
```

### Named Colors
```json lines
{"color": "red"}      // Red
{"color": "green"}    // Green
{"color": "blue"}     // Blue
{"color": "yellow"}   // Yellow
{"color": "orange"}   // Orange
{"color": "pink"}     // Pink
{"color": "cyan"}     // Cyan
{"color": "magenta"}  // Magenta
```

### Decimal Colors
```json lines
{"color": "16711680"}  // Red (0xFF0000)
{"color": "65280"}     // Green (0x00FF00)
{"color": "255"}       // Blue (0x0000FF)
```

## Bot Setup Requirements

### Required Permissions
Your Discord bot needs these permissions:
- **Send Messages**: To send messages to channels
- **Embed Links**: To send rich embeds
- **Attach Files**: If you plan to include attachments
- **Read Message History**: To reply to messages
- **Use Text-to-Speech**: If using TTS feature

### Bot Token Security
- Store bot tokens securely (environment variables, secure vaults)
- Never expose tokens in logs or configuration files
- Rotate tokens regularly for security
- Use least-privilege principle for bot permissions

## Best Practices

### Message Design
- **Keep it concise**: Discord users prefer shorter, well-formatted messages
- **Use embeds wisely**: Rich embeds are great for structured information
- **Color coding**: Use consistent colors for different message types
- **Emojis and formatting**: Enhance readability with Discord markdown

### Performance
- **Reuse connections**: The node automatically shares JDA connections
- **Rate limiting**: Be aware of Discord's rate limits (varies by action)
- **Batch operations**: Consider grouping multiple messages when possible
- **Error handling**: Always handle cases where channels don't exist or bot lacks permissions

### Content Guidelines
- **Follow Discord ToS**: Ensure content complies with Discord's terms
- **Respect community guidelines**: Consider server rules and culture
- **Avoid spam**: Don't send excessive or repetitive messages
- **Accessibility**: Use clear, readable text and appropriate contrast

## Integration Patterns

### With Discord Message Trigger
```json lines
// Trigger listens for commands
{"content_contains": "!status"}

// Executor responds with status
{"message": "System is operational! ✅"}
```

### With Condition Nodes
```json lines
// Check workflow result before sending
{"condition": "success === true"}
// Send success message with green embed
{"embed": {"color": "green", "title": "✅ Success"}}
```

### With Transform Nodes
```json lines
// Transform data into Discord embed format
{"embed": {"fields": transformedData.fields}}
```

## Common Use Cases

1. **Bot Responses**: Automated replies to user commands
2. **Notifications**: System alerts and status updates
3. **Reports**: Automated daily/weekly reports
4. **Webhooks**: Integration with external services
5. **Moderation**: Automated moderation actions and logs
6. **Welcome Messages**: Greet new server members
7. **Event Announcements**: Scheduled event notifications
8. **Support Tickets**: Automated ticket creation responses
9. **Monitoring Alerts**: System health and error notifications
10. **Interactive Workflows**: Multi-step user interactions

## Error Handling

### Common Errors
- **Invalid bot token**: Check token validity and bot status
- **Missing permissions**: Verify bot has required channel permissions
- **Channel not found**: Ensure channel ID is correct and bot has access
- **Message too long**: Discord has 2000 character limit for message content
- **Embed too large**: Total embed size cannot exceed 6000 characters
- **Invalid embed structure**: Check field limits and required properties

### Rate Limiting
- Discord enforces rate limits on message sending
- The node handles rate limits gracefully with automatic retries
- Consider message frequency when designing workflows
- Use embeds to include more information in fewer messages

## Troubleshooting

### Bot Not Sending Messages
1. **Check bot token**: Ensure token is valid and bot is online
2. **Verify permissions**: Bot needs "Send Messages" permission in target channel
3. **Channel access**: Bot must be able to see and access the channel
4. **Server membership**: Bot must be a member of the target server

### Embeds Not Displaying
1. **Check embed structure**: Ensure all required fields are present
2. **Verify permissions**: Bot needs "Embed Links" permission
3. **Content limits**: Check character limits for embed components
4. **Image URLs**: Ensure image URLs are publicly accessible

### Message Formatting Issues
1. **Markdown syntax**: Use proper Discord markdown formatting
2. **Escape characters**: Escape special characters when needed
3. **Line breaks**: Use `\n` for line breaks in embed descriptions
4. **Field limits**: Maximum 25 fields per embed, 256 chars per field name

## Security Considerations

- **Token Management**: Secure storage and rotation of bot tokens
- **Content Validation**: Sanitize user input to prevent injection
- **Permission Scoping**: Grant minimum required permissions
- **Audit Logging**: Track bot message sends for compliance
- **Content Filtering**: Consider inappropriate content detection
