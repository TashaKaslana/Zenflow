package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.formatting;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class FormatDateTransformer implements DataTransformer {
    private final FormatNumberTransformer formatNumberTransformer;

    @Override
    public String getName() {
        return "format_date";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object transform(Object data, Map<String, Object> params) {
        if (params == null || !params.containsKey("field")) {
            throw new DataTransformerExecutorException("Field parameter is required for format_date transformer.");
        }

        String field = (String) params.get("field");
        String pattern = (String) params.getOrDefault("pattern", "yyyy-MM-dd");
        String locale = (String) params.getOrDefault("locale", "en-US");
        String inputPattern = (String) params.get("inputPattern"); // Optional input pattern for parsing

        if (data instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>((Map<String, Object>) map);
            Object dateValue = result.get(field);

            if (dateValue != null) {
                String formattedDate = formatDate(dateValue, pattern, locale, inputPattern);
                result.put(field, formattedDate);
            }

            return result;
        } else {
            // For non-map input, treat it as the date value itself
            return formatDate(data, pattern, locale, inputPattern);
        }
    }

    private String formatDate(Object dateValue, String pattern, String localeStr, String inputPattern) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, formatNumberTransformer.getLocale(localeStr));

            switch (dateValue) {
                case Date date -> {
                    return formatter.format(date.toInstant().atZone(ZoneId.systemDefault()));
                }
                case LocalDateTime localDateTime -> {
                    return formatter.format(localDateTime);
                }
                case ZonedDateTime zonedDateTime -> {
                    return formatter.format(zonedDateTime);
                }
                case String dateStr -> {
                    // Parse string date using input pattern or common patterns
                    LocalDateTime parsedDate = parseStringDate(dateStr, inputPattern);
                    return formatter.format(parsedDate);
                }
                case Long timestamp -> {
                    // Handle timestamp (milliseconds since epoch)
                    LocalDateTime dateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp),
                            ZoneId.systemDefault()
                    );
                    return formatter.format(dateTime);
                }
                default ->
                        throw new DataTransformerExecutorException("Unsupported date type: " + dateValue.getClass().getSimpleName());
            }
        } catch (Exception e) {
            throw new DataTransformerExecutorException("Error formatting date: " + e.getMessage());
        }
    }

    private LocalDateTime parseStringDate(String dateStr, String inputPattern) {
        // Try with provided input pattern first
        if (inputPattern != null && !inputPattern.isEmpty()) {
            try {
                DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputPattern);
                return LocalDateTime.parse(dateStr, inputFormatter);
            } catch (DateTimeParseException e) {
                // Fall through to common patterns
            }
        }

        // Try common date patterns
        String[] commonPatterns = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "MM-dd-yyyy",
            "dd-MM-yyyy"
        };

        for (String pattern : commonPatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(dateStr, formatter);
            } catch (DateTimeParseException e) {
                // Continue to next pattern
            }
        }

        throw new DataTransformerExecutorException("Unable to parse date string: " + dateStr);
    }
}
