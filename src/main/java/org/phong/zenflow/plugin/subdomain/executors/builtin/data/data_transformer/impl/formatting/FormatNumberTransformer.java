package org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.impl.formatting;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.LocaleUtils;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.exception.DataTransformerExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.data.data_transformer.interfaces.DataTransformer;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@Slf4j
public class FormatNumberTransformer implements DataTransformer {

    @Override
    public String getName() {
        return "format_number";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object transform(Object data, Map<String, Object> params) {
        if (params == null || !params.containsKey("field")) {
            throw new DataTransformerExecutorException("Field parameter is required for format_number transformer.");
        }

        String field = (String) params.get("field");
        String pattern = (String) params.get("pattern");
        String locale = (String) params.getOrDefault("locale", "en-US");
        String type = (String) params.getOrDefault("type", "decimal"); // decimal, currency, percent
        Integer precision = (Integer) params.get("precision");

        if (data instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>((Map<String, Object>) map);
            Object numberValue = result.get(field);

            if (numberValue != null) {
                String formattedNumber = formatNumber(numberValue, pattern, locale, type, precision);
                result.put(field, formattedNumber);
            }

            return result;
        } else {
            // For non-map input, treat it as the number value itself
            return formatNumber(data, pattern, locale, type, precision);
        }
    }

    private String formatNumber(Object numberValue, String pattern, String localeStr, String type, Integer precision) {
        try {
            double value = convertToDouble(numberValue);
            Locale loc = LocaleUtils.getLocale(localeStr);

            NumberFormat formatter;

            if (pattern != null && !pattern.isEmpty()) {
                // Use custom pattern
                formatter = new DecimalFormat(pattern);
            } else {
                // Use predefined formatters based on type
                formatter = switch (type.toLowerCase()) {
                    case "currency" -> NumberFormat.getCurrencyInstance(loc);
                    case "percent" -> NumberFormat.getPercentInstance(loc);
                    case "integer" -> NumberFormat.getIntegerInstance(loc);
                    default -> NumberFormat.getNumberInstance(loc);
                };
            }

            // Set precision if specified
            if (precision != null) {
                formatter.setMinimumFractionDigits(precision);
                formatter.setMaximumFractionDigits(precision);
            }

            return formatter.format(value);

        } catch (Exception e) {
            throw new DataTransformerExecutorException("Error formatting number: " + e.getMessage());
        }
    }

    private double convertToDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        } else if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException e) {
                throw new DataTransformerExecutorException("Cannot convert '" + str + "' to number");
            }
        } else {
            throw new DataTransformerExecutorException("Value must be a number or numeric string");
        }
    }
}
