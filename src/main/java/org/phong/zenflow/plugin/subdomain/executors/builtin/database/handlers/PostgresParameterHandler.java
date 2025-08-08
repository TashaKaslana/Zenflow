package org.phong.zenflow.plugin.subdomain.executors.builtin.database.handlers;

import org.phong.zenflow.plugin.subdomain.executors.builtin.database.base.BaseSqlExecutor;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
public class PostgresParameterHandler {

    /**
     * PostgreSQL-specific parameter binder that handles JSONB and Array types
     * Parameters are bound based on their position index
     */
    public BaseSqlExecutor.ParameterBinder createParameterBinder() {
        return (stmt, config, logCollector, isBatch) -> {
            Map<String, Object> params = config.getParams();
            if (params == null) return;

            // Handle indexed parameters - respects the order of ? placeholders in SQL
            if (params.containsKey("parameters")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parameters = (List<Map<String, Object>>) params.get("parameters");

                if (params.containsKey("isBatch") && (Boolean) params.get("isBatch") && !parameters.isEmpty()) {
                    isBatch.set(true);
                    // For batch processing, we need to handle each batch iteration separately
                    // Each parameter in the list represents a complete parameter set for one batch
                    bindBatchParameters(stmt, parameters, logCollector);
                } else {
                    bindIndexedParameters(stmt, parameters, logCollector, false);
                }
            }
        };
    }

    /**
     * Handle batch processing where each element in parameters represents one complete batch iteration
     */
    private void bindBatchParameters(PreparedStatement stmt, List<Map<String, Object>> batchParameters, LogCollector logCollector) throws SQLException {
        for (Map<String, Object> batchParam : batchParameters) {
            // For batch processing, we expect each batch parameter to contain a "parameters" key
            // with the actual parameter list for that batch iteration
            if (batchParam.containsKey("parameters")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parameterSet = (List<Map<String, Object>>) batchParam.get("parameters");
                bindIndexedParameters(stmt, parameterSet, logCollector, true);
            } else {
                // If no nested parameters, treat the batch parameter itself as a single parameter
                // This assumes each batchParam is a parameter with index, type, value
                bindIndexedParameters(stmt, List.of(batchParam), logCollector, true);
            }
        }
    }

    /**
     * Bind parameters by their explicit index positions
     * Expected format:
     * "parameters": [
     * {"index": 1, "type": "jsonb", "value": {...}},
     * {"index": 2, "type": "array", "value": [...]},
     * {"index": 3, "type": "string", "value": "text"}
     * ]
     */
    private void bindIndexedParameters(PreparedStatement stmt, List<Map<String, Object>> parameters, LogCollector logCollector, boolean isBatch) throws SQLException {
        // Sort by index to ensure correct binding order - CRITICAL for SQL injection prevention
        parameters.sort(Comparator.comparingInt(a -> (Integer) a.get("index")));

        // Validate that indices are sequential and start from 1
        validateParameterIndices(parameters, logCollector);

        for (Map<String, Object> param : parameters) {
            int index = (Integer) param.get("index");
            String type = (String) param.get("type");
            Object value = param.get("value");

            logCollector.info("Binding parameter at index " + index + " with type '" + type + "'");

            switch (type.toLowerCase()) {
                case "jsonb" -> bindJsonbParameter(stmt, index, value, logCollector);
                case "array" -> bindArrayParameter(stmt, index, value, logCollector);
                case "uuid" -> bindUuidParameter(stmt, index, (String) value, logCollector);
                case "string" -> {
                    stmt.setString(index, value != null ? value.toString() : null);
                    logCollector.info("Applied string parameter at index " + index);
                }
                case "int", "integer" -> {
                    stmt.setInt(index, value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(value.toString()));
                    logCollector.info("Applied integer parameter at index " + index);
                }
                case "long" -> {
                    stmt.setLong(index, value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString()));
                    logCollector.info("Applied long parameter at index " + index);
                }
                case "boolean" -> {
                    stmt.setBoolean(index, value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(value.toString()));
                    logCollector.info("Applied boolean parameter at index " + index);
                }
                case "numeric", "double" -> {
                    stmt.setDouble(index, value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString()));
                    logCollector.info("Applied numeric parameter at index " + index);
                }
                case "timestamp" -> {
                    switch (value) {
                        case LocalDateTime localDateTime -> stmt.setTimestamp(index, Timestamp.valueOf(localDateTime));
                        case Date date -> stmt.setTimestamp(index, new Timestamp(date.getTime()));
                        case String s -> stmt.setTimestamp(index, parsePostgresTimestamp(s, logCollector));
                        default -> stmt.setTimestamp(index, Timestamp.valueOf(value.toString()));
                    }
                    logCollector.info("Applied timestamp parameter at index " + index);
                }
                case "date" -> {
                    if (value instanceof LocalDate) {
                        stmt.setDate(index, Date.valueOf((LocalDate) value));
                    } else if (value instanceof Date) {
                        stmt.setDate(index, new Date(((Date) value).getTime()));
                    } else {
                        stmt.setDate(index, Date.valueOf(value.toString()));
                    }
                    logCollector.info("Applied date parameter at index " + index);
                }
                case "bytea" -> {
                    stmt.setBytes(index, (byte[]) value);
                    logCollector.info("Applied bytea parameter at index " + index);
                }
                default -> {
                    stmt.setObject(index, value);
                    logCollector.info("Applied generic parameter at index " + index + " with type " + type);
                }
            }
        }

        if (isBatch) {
            stmt.addBatch();
        }
    }

    /**
     * Validates that parameter indices are sequential and start from 1
     * This prevents SQL injection through parameter index manipulation
     */
    private void validateParameterIndices(List<Map<String, Object>> parameters, LogCollector logCollector) throws SQLException {
        if (parameters.isEmpty()) return;

        for (int i = 0; i < parameters.size(); i++) {
            int expectedIndex = i + 1;
            int actualIndex = (Integer) parameters.get(i).get("index");

            if (actualIndex != expectedIndex) {
                String error = String.format("Parameter index validation failed. Expected index %d but found %d. " +
                        "Indices must be sequential starting from 1 to prevent SQL injection.", expectedIndex, actualIndex);
                logCollector.error(error);
                throw new SQLException(error);
            }
        }

        logCollector.info("Parameter index validation passed for " + parameters.size() + " parameters");
    }

    private void bindJsonbParameter(PreparedStatement stmt, int index, Object value, LogCollector logCollector) throws SQLException {
        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(value.toString());
            stmt.setObject(index, jsonObject);
            logCollector.info("Applied JSONB parameter at index " + index);
        } catch (Exception e) {
            logCollector.warning("Failed to apply JSONB parameter at index " + index + ": " + e.getMessage());
            throw new SQLException("JSONB parameter binding failed at index " + index, e);
        }
    }

    private void bindArrayParameter(PreparedStatement stmt, int index, Object value, LogCollector logCollector) throws SQLException {
        try {
            Object[] array;
            if (value instanceof List<?> list) {
                array = list.toArray();
            } else if (value instanceof Object[] objArray) {
                array = objArray;
            } else {
                throw new SQLException("Array parameter must be a List or Object[]");
            }

            String pgType = determineArrayType(array);

            Array sqlArray = stmt.getConnection().createArrayOf(pgType, array);
            stmt.setArray(index, sqlArray);
            logCollector.info("Applied array parameter at index " + index + " with element type '" + pgType + "'");
        } catch (Exception e) {
            logCollector.warning("Failed to apply array parameter at index " + index + ": " + e.getMessage());
            throw new SQLException("Array parameter binding failed at index " + index, e);
        }
    }

    private String determineArrayType(Object[] array) {
        if (array == null || array.length == 0 || array[0] == null) {
            return "text";
        }

        Object first = array[0];
        if (first instanceof Integer) return "int";
        if (first instanceof Long) return "bigint";
        if (first instanceof Short) return "smallint";
        if (first instanceof Boolean) return "boolean";
        if (first instanceof Double) return "float8";
        if (first instanceof Float) return "float4";
        if (first instanceof UUID) return "uuid";
        if (first instanceof LocalDate || first instanceof Date) return "date";
        if (first instanceof LocalDateTime || first instanceof Timestamp) return "timestamp";
        return "text";
    }

    private void bindUuidParameter(PreparedStatement stmt, int index, String value, LogCollector logCollector) throws SQLException {
        try {
            stmt.setObject(index, UUID.fromString(value));
            logCollector.info("Applied UUID parameter at index " + index);
        } catch (Exception e) {
            logCollector.warning("Failed to apply UUID parameter at index " + index + ": " + e.getMessage());
            throw new SQLException("UUID parameter binding failed at index " + index, e);
        }
    }

    /**
     * PostgreSQL-specific result processor that adds metadata and handles special types
     */
    public BaseSqlExecutor.ResultProcessor createResultProcessor() {
        return (result, config, logCollector) -> {
            Map<String, Object> enhancedResult = new HashMap<>(result);

            // Add PostgreSQL-specific metadata
            enhancedResult.put("driver", "postgresql");
            enhancedResult.put("postgresSpecific", true);

            // Detect PostgreSQL-specific features used
            String query = config.getQuery().toLowerCase();
            Map<String, Boolean> features = detectPostgresFeatures(query);
            enhancedResult.put("postgresFeatures", features);

            if (features.values().stream().anyMatch(Boolean::booleanValue)) {
                logCollector.info("Query used PostgreSQL-specific features: " + features);
            }

            return enhancedResult;
        };
    }

    private Map<String, Boolean> detectPostgresFeatures(String query) {
        Map<String, Boolean> features = new HashMap<>();
        features.put("usedReturning", query.contains("returning"));
        features.put("usedUpsert", query.contains("on conflict"));
        features.put("usedJsonb", query.contains("jsonb"));
        features.put("usedArrays", query.contains("array[") || query.contains("any("));
        features.put("usedCTE", query.contains("with "));
        features.put("usedWindow", query.contains("over("));
        features.put("usedLateral", query.contains("lateral"));
        return features;
    }

    /**
     * Parse PostgreSQL timestamp strings with various formats including timezone information
     * Handles formats like: 2025-07-10 10:52:38.384986 +00:00, 2025-07-10T10:52:38.384986Z, etc.
     */
    private Timestamp parsePostgresTimestamp(String timestampStr, LogCollector logCollector) throws SQLException {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            throw new SQLException("Timestamp string cannot be null or empty");
        }

        String trimmed = timestampStr.trim();

        try {
            // Remove timezone information for Timestamp.valueOf() compatibility
            // PostgreSQL format: 2025-07-10 10:52:38.384986 +00:00
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d+)? [+-]\\d{2}:\\d{2}")) {
                // Extract the timestamp part before timezone
                String timestampPart = trimmed.substring(0, trimmed.lastIndexOf(' '));
                return Timestamp.valueOf(timestampPart);
            }

            // ISO 8601 with Z: 2025-07-10T10:52:38.384986Z
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z?")) {
                // Convert ISO format to SQL timestamp format
                String sqlFormat = trimmed.replace('T', ' ');
                if (sqlFormat.endsWith("Z")) {
                    sqlFormat = sqlFormat.substring(0, sqlFormat.length() - 1);
                }
                return Timestamp.valueOf(sqlFormat);
            }

            // Basic timestamp format: 2025-07-10 10:52:38[.nnnnnnnnn]
            if (trimmed.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d+)?")) {
                return Timestamp.valueOf(trimmed);
            }

            // If none of the patterns match, try direct parsing
            logCollector.warning("Unrecognized timestamp format, attempting direct parsing: " + trimmed);
            return Timestamp.valueOf(trimmed);

        } catch (Exception e) {
            String error = "Failed to parse timestamp string: " + timestampStr + ". Expected format: yyyy-mm-dd hh:mm:ss[.fffffffff]";
            logCollector.error(error + " - " + e.getMessage());
            throw new SQLException(error, e);
        }
    }
}
