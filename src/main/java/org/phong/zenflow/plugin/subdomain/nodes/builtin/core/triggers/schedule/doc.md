# Schedule Trigger Node

## Overview

Automatically triggers workflows based on CRON expressions, enabling time-based automation and recurring executions.

## Node Information

- **Key**: `core:schedule.trigger`
- **Version**: `1.0.0`
- **Type**: `trigger`
- **Icon**: `ph:calendar-check`
- **Tags**: `trigger`, `schedule`, `cron`, `automation`

## Description

The Schedule Trigger node executes workflows automatically based on CRON expressions. It supports complex scheduling patterns including daily, weekly, monthly executions, and custom intervals, making it ideal for automated reporting, maintenance tasks, and periodic data processing.

## Input/Output

### Input
- `cron_expression` (string, required): CRON expression defining when to trigger (e.g., "0 9 * * MON-FRI")
- `payload` (object, optional): Optional payload data to pass to the workflow
- `schedule_description` (string, optional): Human-readable description of the schedule
- `timezone` (string, optional): Timezone for schedule execution (default: UTC)
- `enabled` (boolean, optional): Whether the schedule is active (default: true)

### Output
- `trigger_type` (string): Always "schedule" for scheduled triggers
- `triggered_at` (string): ISO timestamp when schedule triggered
- `trigger_source` (string): Source identifier for the schedule
- `cron_expression` (string): The CRON expression that triggered execution
- `schedule_description` (string): Description of the schedule
- `payload` (object): Any payload data provided with the schedule
- `next_execution` (string): ISO timestamp of next scheduled execution

## Usage Examples

### Daily Report at 9 AM
```json
{
  "key": "daily-report",
  "type": "TRIGGER",
  "pluginNode": {
    "pluginKey": "core",
    "nodeKey": "schedule.trigger",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "cron_expression": "0 9 * * *",
      "schedule_description": "Daily report generation at 9:00 AM",
      "payload": {
        "report_type": "daily",
        "recipients": ["admin@company.com"]
      }
    }
  }
}
```

### Weekly Maintenance (Sundays at 2 AM)
```json
{
  "config": {
    "input": {
      "cron_expression": "0 2 * * SUN",
      "schedule_description": "Weekly system maintenance",
      "timezone": "America/New_York",
      "payload": {
        "maintenance_type": "cleanup",
        "notify_admins": true
      }
    }
  }
}
```

### Every 15 Minutes During Business Hours
```json
{
  "config": {
    "input": {
      "cron_expression": "*/15 9-17 * * MON-FRI",
      "schedule_description": "Monitor system health every 15 minutes",
      "payload": {
        "check_type": "health_check",
        "alert_threshold": 95
      }
    }
  }
}
```

### Monthly Reports (First Day at Midnight)
```json
{
  "config": {
    "input": {
      "cron_expression": "0 0 1 * *",
      "schedule_description": "Monthly financial reports",
      "payload": {
        "report_period": "previous_month",
        "format": "pdf"
      }
    }
  }
}
```

## CRON Expression Format

Standard 5-field CRON format: `minute hour day month day_of_week`

### Field Values
- **Minute**: 0-59
- **Hour**: 0-23 (24-hour format)
- **Day**: 1-31
- **Month**: 1-12 or JAN-DEC
- **Day of Week**: 0-7 (0 and 7 = Sunday) or SUN-SAT

### Special Characters
- `*`: Any value
- `?`: No specific value (day/day_of_week only)
- `-`: Range (e.g., 1-5)
- `,`: List (e.g., 1,3,5)
- `/`: Step values (e.g., */5 = every 5)
- `L`: Last (e.g., L = last day of month)
- `#`: Nth occurrence (e.g., 2#3 = 3rd Tuesday)

## Common CRON Patterns

### Frequently Used Schedules
```
0 0 * * *        # Daily at midnight
0 9 * * MON-FRI  # Weekdays at 9 AM
0 0 * * SUN      # Weekly on Sunday
0 0 1 * *        # Monthly on 1st
*/30 * * * *     # Every 30 minutes
0 */6 * * *      # Every 6 hours
0 9,17 * * *     # Twice daily (9 AM, 5 PM)
```

## Common Use Cases

- **Automated Reports**: Generate and distribute regular reports
- **Data Backup**: Schedule regular database and file backups
- **System Maintenance**: Clean up logs, optimize databases
- **Monitoring**: Regular health checks and status updates
- **Data Processing**: ETL jobs and batch processing
- **Notifications**: Periodic reminders and alerts
- **Cache Refresh**: Update cached data at regular intervals

## Best Practices

- Use descriptive schedule descriptions
- Consider timezone implications for global deployments
- Test CRON expressions thoroughly before deployment
- Monitor scheduled executions for failures
- Use appropriate payload data for context
- Consider system load when scheduling frequent executions

## Error Handling

- Invalid CRON expressions will prevent schedule creation
- Failed executions are logged with error details
- Missed executions (system downtime) may need manual intervention
- Long-running workflows may overlap with next scheduled execution
