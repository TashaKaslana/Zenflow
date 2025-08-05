package org.phong.zenflow.plugin.subdomain.executors.builtin.database.executor.sql;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.base.BaseDbConnection;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.base.BaseSqlExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto.ResolvedDbConfig;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.handlers.PostgresParameterHandler;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@AllArgsConstructor
public class PostgresSqlExecutor implements PluginNodeExecutor {
    private final BaseDbConnection baseDbConnection;
    private final BaseSqlExecutor baseSqlExecutor;
    private final PostgresParameterHandler postgresHandler;

    @Override
    public String key() {
        return "core:postgresql:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            log.info("Executing Postgres SQL node with config: {}", config);
            ResolvedDbConfig dbConfig = baseDbConnection.establishConnection(config, context, logCollector);

            // Pre-process PostgreSQL-specific syntax
            dbConfig = preprocessPostgresSyntax(dbConfig, logCollector);

            // Intelligent parameter processing - infer types automatically
            dbConfig = processParametersWithTypeInference(dbConfig, logCollector);

            // Create PostgreSQL-specific parameter binder and result processor
            BaseSqlExecutor.ParameterBinder parameterBinder = hasParameters(dbConfig) ?
                postgresHandler.createParameterBinder() : null;

            BaseSqlExecutor.ResultProcessor resultProcessor = postgresHandler.createResultProcessor();

            // Execute using BaseSqlExecutor with PostgreSQL-specific lambdas
            return baseSqlExecutor.execute(dbConfig, logCollector, parameterBinder, resultProcessor);

        } catch (Exception e) {
            log.error("Postgres SQL execution failed", e);
            return ExecutionResult.error("Postgres SQL execution failed: " + e.getMessage(), logCollector.getLogs());
        }
    }

    private boolean hasParameters(ResolvedDbConfig dbConfig) {
        Map<String, Object> params = dbConfig.getParams();
        return params != null && (
            params.containsKey("parameters") ||
            params.containsKey("jsonbParams") ||
            params.containsKey("arrayParams") ||
            params.containsKey("uuidParams")
        );
    }

    /**
     * Smart parameter processing - acts like a database compiler
     * Automatically infers PostgreSQL types from Java objects
     */
    private ResolvedDbConfig processParametersWithTypeInference(ResolvedDbConfig dbConfig, LogCollector logCollector) {
        Map<String, Object> params = dbConfig.getParams();
        if (params == null) return dbConfig;

        // If already using indexed parameters, skip inference
        if (params.containsKey("parameters")) {
            logCollector.info("Using explicit indexed parameters");
            return dbConfig;
        }

        // Check if we have a simple parameter array for inference
        if (params.containsKey("values")) {
            dbConfig = inferParameterTypes(dbConfig, logCollector);
        }

        return dbConfig;
    }

    /**
     * Database compiler-style type inference
     * Analyzes Java objects and maps them to PostgreSQL types
     */
    private ResolvedDbConfig inferParameterTypes(ResolvedDbConfig dbConfig, LogCollector logCollector) {
        Map<String, Object> params = dbConfig.getParams();
        Object valuesObj = params.get("values");

        if (!(valuesObj instanceof List)) {
            logCollector.warning("'values' parameter should be a List for type inference");
            return dbConfig;
        }

        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) valuesObj;

        List<Map<String, Object>> inferredParameters = new ArrayList<>();

        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            int index = i + 1; // SQL parameters are 1-based
            String inferredType = inferPostgresType(value, logCollector);

            Map<String, Object> param = Map.of(
                "index", index,
                "type", inferredType,
                "value", value
            );

            inferredParameters.add(param);
            logCollector.info("Parameter " + index + ": inferred type '" + inferredType + "' for value: " +
                    value.getClass().getSimpleName());
        }

        // Replace the simple values with our inferred parameter structure
        params.put("parameters", inferredParameters);
        params.remove("values"); // Clean up the original values

        logCollector.info("Successfully inferred types for " + inferredParameters.size() + " parameters");
        return dbConfig;
    }

    /**
     * PostgreSQL type inference engine - like a database compiler
     * Maps Java types to PostgreSQL types intelligently
     */
    private String inferPostgresType(Object value, LogCollector logCollector) {
        if (value == null) return "string"; // Default to string for nulls

        Class<?> clazz = value.getClass();

        // Basic types
        if (clazz == String.class) {
            String str = (String) value;
            // UUID detection
            if (isValidUUID(str)) {
                return "uuid";
            }
            return "string";
        }

        if (clazz == Integer.class) return "int";
        if (clazz == Long.class) return "long";
        if (clazz == Boolean.class) return "boolean";
        if (clazz == Double.class || clazz == Float.class) return "numeric";

        // Collection types
        if (value instanceof List || value instanceof Object[]) {
            return "array";
        }

        // Complex types - likely JSON
        if (value instanceof Map || isComplexObject(value)) {
            return "jsonb";
        }

        // Time types
        if (value instanceof java.time.LocalDateTime ||
            value instanceof java.time.ZonedDateTime ||
            value instanceof java.util.Date) {
            return "timestamp";
        }

        if (value instanceof java.time.LocalDate) {
            return "date";
        }

        // Binary data
        if (value instanceof byte[]) {
            return "bytea";
        }

        // Default fallback
        logCollector.info("Unknown type " + clazz.getSimpleName() + ", defaulting to 'string'");
        return "string";
    }

    private boolean isValidUUID(String str) {
        try {
            java.util.UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isComplexObject(Object value) {
        // Detect if this is a complex object that should be JSON
        Class<?> clazz = value.getClass();
        Package pkg = clazz.getPackage();

        // Skip Java built-in types
        if (pkg != null && (pkg.getName().startsWith("java.") || pkg.getName().startsWith("javax."))) {
            return false;
        }

        // If it has fields/properties, treat as complex object for JSONB
        return clazz.getDeclaredFields().length > 0;
    }

    private ResolvedDbConfig preprocessPostgresSyntax(ResolvedDbConfig dbConfig, LogCollector logCollector) {
        String originalQuery = dbConfig.getQuery();
        Map<String, Object> params = dbConfig.getParams();

        if (params == null) return dbConfig;

        // Handle UPSERT syntax transformation
        if (originalQuery.toLowerCase().contains("upsert_placeholder")) {
            String conflictColumns = (String) params.get("conflictColumns");
            String updateAction = (String) params.get("updateAction");
            if (conflictColumns != null && updateAction != null) {
                String newQuery = originalQuery.replace("upsert_placeholder",
                    "ON CONFLICT (" + conflictColumns + ") DO " + updateAction);
                dbConfig.setQuery(newQuery);
                logCollector.info("Converted UPSERT syntax: " + newQuery);
            }
        }

        // Handle schema-qualified table references
        if (params.containsKey("schema")) {
            String schema = (String) params.get("schema");
            String modifiedQuery = originalQuery.replaceAll("\\{schema}", schema);
            dbConfig.setQuery(modifiedQuery);
            logCollector.info("Applied schema qualification: " + schema);
        }

        return dbConfig;
    }
}
