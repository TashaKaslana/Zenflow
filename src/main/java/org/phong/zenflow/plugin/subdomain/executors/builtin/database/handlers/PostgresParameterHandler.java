package org.phong.zenflow.plugin.subdomain.executors.builtin.database.handlers;

import org.phong.zenflow.plugin.subdomain.executors.builtin.database.base.BaseSqlExecutor;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.sql.Date;
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
        return (stmt, config, logCollector) -> {
            Map<String, Object> params = config.getParams();
            if (params == null) return;

            // Handle indexed parameters - respects the order of ? placeholders in SQL
            if (params.containsKey("parameters")) {
                bindIndexedParameters(stmt, params, logCollector);
            }
        };
    }

    /**
     * Bind parameters by their explicit index positions
     * Expected format:
     * "parameters": [
     *   {"index": 1, "type": "jsonb", "value": {...}},
     *   {"index": 2, "type": "array", "value": [...]},
     *   {"index": 3, "type": "string", "value": "text"}
     * ]
     */
    private void bindIndexedParameters(PreparedStatement stmt, Map<String, Object> params, LogCollector logCollector) throws SQLException {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) params.get("parameters");

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
                    if (value instanceof LocalDateTime) {
                        stmt.setTimestamp(index, Timestamp.valueOf((LocalDateTime) value));
                    } else if (value instanceof Date) {
                        stmt.setTimestamp(index, new Timestamp(((Date) value).getTime()));
                    } else {
                        stmt.setTimestamp(index, Timestamp.valueOf(value.toString()));
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
            Array sqlArray = stmt.getConnection().createArrayOf("text", (Object[]) value);
            stmt.setArray(index, sqlArray);
            logCollector.info("Applied array parameter at index " + index);
        } catch (Exception e) {
            logCollector.warning("Failed to apply array parameter at index " + index + ": " + e.getMessage());
            throw new SQLException("Array parameter binding failed at index " + index, e);
        }
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
}
