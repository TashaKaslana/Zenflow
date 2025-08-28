# Discord Message Trigger Node

## Overview

Listens for Discord messages in specified channels and triggers workflows when messages are received. Uses an efficient centralized hub architecture for O(1) message routing performance.

## Node Information

- **Key**: `integration:discord.message.trigger`
- **Version**: `1.0.0`
- **Type**: `trigger`
- **Trigger Type**: `event`
- **Icon**: `simple-icons:discord`
- **Tags**: `integration`, `discord`, `trigger`, `message`

## Description

The Discord Message Trigger monitors Discord channels for new messages and triggers workflows when messages match specified criteria. It uses a centralized listener hub that provides O(1) performance by routing messages directly to the appropriate workflow without iterating through all registered listeners.

Perfect for building Discord bots, automated moderation, command processing, notification systems, and community management workflows.

## Features

- **High Performance**: O(1) message routing using centralized hub architecture
- **Efficient Resource Management**: Shares JDA connections across multiple triggers
- **Flexible Filtering**: Content filters, user filters, role filters, command prefixes
- **Rich Message Data**: Complete Discord message context including attachments and mentions
- **Bot Management**: Built-in bot message filtering
- **Automatic Cleanup**: Proper resource cleanup when triggers are stopped

## Input Configuration

### Required Parameters
- `bot_token` (string): Discord bot token for authentication
- `channel_id` (string): Discord channel ID to monitor (snowflake format)

### Optional Filtering Parameters
- `content_contains` (string): Only trigger if message contains this text
- `ignore_bots` (boolean): Ignore messages from bot accounts (default: true)
- `user_filter` (array): Array of user IDs - only these users can trigger workflows
- `role_filter` (array): Array of role IDs - only users with these roles can trigger
- `command_prefix` (string): Command prefix (e.g., "!" or "/") - only prefixed messages trigger
- `case_sensitive` (boolean): Whether content filtering is case-sensitive (default: false)

## Output Data

### Standard Fields
- `trigger_type`: Always "discord_message"
- `triggered_at`: Unix timestamp when trigger fired
- `message_id`: Discord message ID (snowflake)
- `channel_id`: Channel where message was sent
- `author_id`: Message author's user ID
- `content`: Full message text content
- `guild_id`: Discord server (guild) ID
- `timestamp`: ISO timestamp when message was created

### Rich Context Data
- `author`: Complete author information (username, avatar, etc.)
- `channel`: Channel details (name, type)
- `guild`: Server information (name)
- `attachments`: Array of file attachments with URLs and metadata
- `mentions`: Users, roles, and @everyone mentions in the message

## Usage Examples

### Basic Message Monitoring
```json
{
  "key": "discord-monitor",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678"
    }
  }
}
```

### Command Bot with Prefix
```json
{
  "key": "discord-commands",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "command_prefix": "!",
      "ignore_bots": true
    }
  }
}
```

### Moderation Bot with Role Filter
```json
{
  "key": "mod-actions",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "role_filter": ["987654321098765432", "876543210987654321"],
      "content_contains": "report"
    }
  }
}
```

### Support Ticket System
```json
{
  "key": "support-tickets",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "command_prefix": "/ticket",
      "ignore_bots": true,
      "case_sensitive": false
    }
  }
}
```

### VIP User Monitoring
```json
{
  "key": "vip-messages",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "discord.message.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "bot_token": "YOUR_BOT_TOKEN",
      "channel_id": "123456789012345678",
      "user_filter": ["111111111111111111", "222222222222222222"]
    }
  }
}
```

## Bot Setup Requirements

### Creating a Discord Bot
1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create a new application
3. Go to "Bot" section and create a bot
4. Copy the bot token (keep it secure!)
5. Enable necessary intents (Message Content Intent for content filtering)

### Bot Permissions
Your bot needs these permissions:
- **Read Messages**: To receive message events
- **Read Message History**: To access message content
- **Send Messages**: If using with Discord Message Executor
- **View Channels**: To access the target channel

### Adding Bot to Server
1. Go to OAuth2 > URL Generator in Discord Developer Portal
2. Select "bot" scope
3. Select required permissions
4. Use generated URL to add bot to your server

## Architecture & Performance

### Centralized Hub Design
- **Single Listener**: One `DiscordMessageListenerHub` per JDA instance
- **O(1) Routing**: Direct channel ID lookup instead of iterating all listeners
- **Shared Resources**: Multiple triggers share the same JDA connection
- **Memory Efficient**: Minimal overhead per additional trigger

### Resource Management
- **Connection Sharing**: Multiple triggers with same bot token share JDA instance
- **Automatic Cleanup**: Resources cleaned up when all triggers using them are stopped
- **Health Monitoring**: Built-in JDA connection health checking
- **Reference Counting**: Tracks active triggers per resource

## Best Practices

### Security
- **Never expose bot tokens** in logs or configuration files
- **Use environment variables** or secure vaults for token storage
- **Rotate tokens regularly** and update configurations
- **Apply principle of least privilege** for bot permissions

### Performance
- **Use content filtering** to reduce unnecessary workflow executions
- **Implement role/user filters** for targeted monitoring
- **Consider message volume** when setting up triggers for busy channels
- **Use command prefixes** to filter relevant messages early

### Bot Design
- **Ignore bot messages** unless specifically needed
- **Use case-insensitive filtering** for better user experience
- **Implement proper error handling** in downstream workflow nodes
- **Consider rate limiting** for high-frequency channels

## Common Use Cases

1. **Discord Bots**: Command processing and automated responses
2. **Community Management**: Moderation, user onboarding, rule enforcement  
3. **Notification Systems**: Alert workflows based on Discord activity
4. **Support Systems**: Ticket creation and management
5. **Event Management**: RSVP tracking and event announcements
6. **Content Curation**: Message archiving and content moderation
7. **Integration Workflows**: Connect Discord with external services
8. **Gaming Communities**: Clan management and tournament coordination
9. **Educational Platforms**: Student interaction and assignment submission
10. **Business Communication**: Team coordination and project updates

## Integration with Other Nodes

Works seamlessly with other workflow nodes:

- **Discord Message Executor**: Send responses back to Discord
- **Condition Nodes**: Filter messages based on complex criteria
- **Transform Nodes**: Process and format message data
- **Database Nodes**: Store message history and user data
- **HTTP Request Nodes**: Integrate with external APIs
- **Email Nodes**: Send notifications about Discord activity

## Troubleshooting

### Common Issues
- **Bot not receiving messages**: Check bot permissions and channel access
- **Trigger not firing**: Verify channel ID format and bot token
- **Content filter not working**: Check case sensitivity settings
- **Missing message data**: Ensure Message Content Intent is enabled
- **Connection errors**: Verify bot token is valid and bot is in server

### Debugging Tips
- **Test with simple setup** first (no filters) then add complexity
- **Check Discord Developer Portal** for bot status and permissions
- **Monitor application logs** for JDA connection status
- **Verify channel IDs** using Discord's developer mode
- **Test bot permissions** by trying to send a message manually

### Performance Issues
- **High memory usage**: Check for resource leaks, ensure proper trigger cleanup
- **Slow response times**: Consider message volume and workflow complexity
- **Connection instability**: Check network connectivity and Discord API status

## Error Handling

The trigger handles various error scenarios gracefully:
- **Invalid tokens**: Logged and trigger marked as unhealthy
- **Missing permissions**: Graceful failure with informative error messages
- **Network issues**: Automatic reconnection attempts by JDA
- **Channel deletion**: Trigger continues running, resumes when channel restored
- **Bot removal**: Connection drops gracefully, resources cleaned up

## Security Considerations

- **Token Protection**: Never log or expose bot tokens
- **Permission Scoping**: Grant minimum required permissions
- **Content Filtering**: Be aware of sensitive content in message data
- **User Privacy**: Consider data retention policies for message content
- **Audit Logging**: Track bot actions for security compliance
