package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

@Component
public class DateTimeTools implements AiToolProvider {
    
    @Tool(description = "Get the current date and time in ISO-8601 format")
    public String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
    }
    
    @Tool(description = "Get the current date and time in a specific timezone (e.g., 'America/New_York', 'Europe/London', 'Asia/Tokyo')")
    public String getCurrentDateTimeInZone(String timezone) {
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            return LocalDateTime.now(zoneId).format(DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return "Error: Invalid timezone '" + timezone + "'. Use format like 'America/New_York' or 'UTC'";
        }
    }
    
    @Tool(description = "Get the current Unix timestamp in milliseconds")
    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    @Tool(description = "Format a Unix timestamp (in milliseconds) to human-readable ISO-8601 date-time")
    public String formatTimestamp(long timestamp) {
        try {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
            );
            return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return "Error: Invalid timestamp " + timestamp;
        }
    }
}
