# Email Node

## Overview

Sends emails via SMTP with support for HTML/plain text content, attachments, and various SMTP configurations including authentication and encryption.

## Node Information

- **Key**: `integration:email.send`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:envelope`
- **Tags**: `email`, `smtp`, `notification`, `integration`

## Description

The Email node enables workflows to send emails through SMTP servers. It supports both HTML and plain text emails, file attachments, custom headers, and various authentication methods. Perfect for notifications, alerts, reports, and automated communications.

## Input/Output

### Input
- `host` (string, required): SMTP server host (e.g., smtp.gmail.com, smtp.outlook.com)
- `port` (integer): SMTP server port (default: 587 for TLS, 465 for SSL, 25 for plain)
- `to` (string, required): Recipient email address (format: email)
- `from` (string): Sender email address (defaults to authenticated user)
- `subject` (string, required): Email subject line
- `body` (string, required): Email body content (HTML or plain text)
- `cc` (array): CC recipients (array of email addresses)
- `bcc` (array): BCC recipients (array of email addresses)
- `username` (string): SMTP authentication username
- `password` (string): SMTP authentication password
- `use_tls` (boolean): Enable TLS encryption (default: true)
- `use_ssl` (boolean): Enable SSL encryption (default: false)
- `content_type` (string): Content type - "text/html" or "text/plain" (default: "text/html")
- `attachments` (array): File attachments with path and content

### Output
- `sent` (boolean): Whether the email was sent successfully
- `message_id` (string): Unique message identifier from SMTP server
- `recipients` (array): List of all recipients (to, cc, bcc)
- `sent_at` (string): ISO timestamp when email was sent
- `smtp_response` (string): SMTP server response message
- `attachment_count` (integer): Number of attachments included

## Usage Examples

### Simple Email Notification
```json
{
  "key": "send-notification",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "email.send",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "host": "smtp.gmail.com",
      "port": 587,
      "username": "notifications@company.com",
      "password": "{{secrets.email_password}}",
      "to": "user@example.com",
      "subject": "Workflow Complete",
      "body": "<h1>Success!</h1><p>Your workflow has completed successfully.</p>",
      "use_tls": true
    }
  }
}
```

### Email with Multiple Recipients and Attachments
```json
{
  "input": {
    "host": "smtp.outlook.com",
    "port": 587,
    "to": "primary@example.com",
    "cc": ["manager@example.com", "team@example.com"],
    "subject": "Weekly Report - {{current_date}}",
    "body": "{{report_content}}",
    "content_type": "text/html",
    "attachments": [
      {
        "filename": "report.pdf",
        "content": "{{generated_report}}"
      }
    ]
  }
}
```

### Dynamic Email from Workflow Data
```json
{
  "input": {
    "host": "{{email_config.smtp_host}}",
    "to": "{{user_data.email}}",
    "subject": "Welcome {{user_data.name}}!",
    "body": "<p>Hello {{user_data.name}},</p><p>Welcome to our platform!</p>",
    "username": "{{secrets.smtp_username}}",
    "password": "{{secrets.smtp_password}}"
  }
}
```

## SMTP Provider Examples

### Gmail
```json
{
  "host": "smtp.gmail.com",
  "port": 587,
  "use_tls": true,
  "username": "your-email@gmail.com",
  "password": "app-specific-password"
}
```

### Outlook/Hotmail
```json
{
  "host": "smtp.outlook.com",
  "port": 587,
  "use_tls": true,
  "username": "your-email@outlook.com",
  "password": "your-password"
}
```

### Custom SMTP Server
```json
{
  "host": "mail.yourcompany.com",
  "port": 25,
  "use_tls": false,
  "username": "notifications@yourcompany.com",
  "password": "server-password"
}
```

## Common Use Cases

- **Notifications**: Send workflow completion and error notifications
- **Reports**: Email automated reports and summaries
- **Alerts**: Send system alerts and monitoring notifications  
- **User Communications**: Welcome emails, password resets, confirmations
- **Data Export**: Email generated reports, exports, and documents
- **Team Updates**: Automated status updates and progress reports

## Security Considerations

- Store SMTP credentials securely using secrets management
- Use TLS/SSL encryption for sensitive communications
- Validate email addresses to prevent injection attacks
- Be cautious with user-provided email content
- Consider rate limiting to prevent abuse
- Use app-specific passwords for Gmail and similar services

## Error Handling

Common error scenarios:
- SMTP authentication failures
- Network connectivity issues
- Invalid email addresses
- SMTP server rate limiting
- Attachment size limits exceeded
- TLS/SSL connection failures
