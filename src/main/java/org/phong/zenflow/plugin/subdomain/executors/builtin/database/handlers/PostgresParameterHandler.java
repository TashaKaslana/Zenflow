package org.phong.zenflow.plugin.subdomain.executors.builtin.database.handlers;

import org.phong.zenflow.plugin.subdomain.executors.builtin.database.base.BaseSqlExecutor;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.postgresql.util.PGobject;
import org.springframework.stereotype.Component;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
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
        java.util.List<Map<String, Object>> parameters = (java.util.List<Map<String, Object>>) params.get("parameters");

        // Sort by index to ensure correct binding order
        parameters.sort(Comparator.comparingInt(a -> (Integer) a.get("index")));

        for (Map<String, Object> param : parameters) {
            int index = (Integer) param.get("index");
            String type = (String) param.get("type");
            Object value = param.get("value");

            switch (type.toLowerCase()) {
                case "jsonb" -> bindJsonbParameter(stmt, index, value, logCollector);
                case "array" -> bindArrayParameter(stmt, index, (Object[]) value, logCollector);
                case "uuid" -> bindUuidParameter(stmt, index, (String) value, logCollector);
                case "string" -> {
                    stmt.setString(index, (String) value);
                    logCollector.info("Applied string parameter at index " + index);
                }
                case "int", "integer" -> {
                    stmt.setInt(index, (Integer) value);
                    logCollector.info("Applied integer parameter at index " + index);
                }
                case "long" -> {
                    stmt.setLong(index, (Long) value);
                    logCollector.info("Applied long parameter at index " + index);
                }
                default -> {
                    stmt.setObject(index, value);
                    logCollector.info("Applied generic parameter at index " + index + " with type " + type);
                }
            }
        }
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

    private void bindArrayParameter(PreparedStatement stmt, int index, Object[] value, LogCollector logCollector) throws SQLException {
        try {
            Array sqlArray = stmt.getConnection().createArrayOf("text", value);
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
