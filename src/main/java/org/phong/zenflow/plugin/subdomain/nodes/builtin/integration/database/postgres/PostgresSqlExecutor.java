package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.postgres;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.BaseDbConnection;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.BaseSqlExecutor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.ResolvedDbConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;

@Component
@PluginNode(
        key = "integration:postgresql",
        name = "PostgreSQL",
        version = "1.0.0",
        description = "Executes SQL queries against a PostgreSQL database with advanced parameter handling and type inference.",
        type = "integration.database",
        icon = "postgresql",
        tags = { "database", "postgresql", "sql", "integration" }
)
@Slf4j
@AllArgsConstructor
public class PostgresSqlExecutor implements PluginNodeExecutor {
    private final BaseDbConnection baseDbConnection;
    private final BaseSqlExecutor baseSqlExecutor;
    private final PostgresParameterHandler postgresHandler;
    private final ObjectMapper objectMapper;
    private final PostgresSqlRuntimeValidator runtimeValidator;
    private final PostgresSqlDefinitionValidator definitionValidator;

    @Override
    public String key() {
        return "integration:postgresql:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            log.info("Executing Postgres SQL node with config: {}", config);

            config.input().put("driver", "postgresql");
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

    @Override
    public List<ValidationError> validateDefinition(WorkflowConfig config) {
        return definitionValidator.validate(config);
    }

    @Override
    public List<ValidationError> validateRuntime(WorkflowConfig config, ExecutionContext ctx) {
        return runtimeValidator.validate(config, ctx);
    }

    private boolean hasParameters(ResolvedDbConfig dbConfig) {
        Map<String, Object> params = dbConfig.getParams();
        return params != null && (
                params.containsKey("parameters") ||
                        params.containsKey("batchValues") ||
                        params.containsKey("values") ||
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

        AtomicInteger startIndex = new AtomicInteger(0);
        // Check if we have a simple parameter array for inference
        if (params.containsKey("values")) {
            dbConfig = inferParameterTypes(dbConfig, logCollector, false, startIndex);
        }

        if (params.containsKey("batchValues") && params.get("batchValues") instanceof List) {
            dbConfig = inferParameterTypes(dbConfig, logCollector, true, startIndex);
        }

        return dbConfig;
    }

    /**
     * Database compiler-style type inference
     * Analyzes Java objects and maps them to PostgreSQL types
     */
    private ResolvedDbConfig inferParameterTypes(ResolvedDbConfig dbConfig, LogCollector logCollector, boolean isBatch, AtomicInteger startIndex) {
        Map<String, Object> params = dbConfig.getParams();
        Object valuesObj = params.get(isBatch ? "batchValues" : "values");

        if (!(valuesObj instanceof List)) {
            logCollector.warning("'values' parameter should be a List for type inference");
            return dbConfig;
        }

        @SuppressWarnings("unchecked")
        List<Object> values = (List<Object>) valuesObj;

        if (isBatch) {
            if (values.stream().anyMatch(v -> !(v instanceof List))) {
                logCollector.error("'batchValues' must be a list of lists.");
                return dbConfig;
            }

            // For batch processing, create a list of parameter sets, each starting from index 1
            List<Map<String, Object>> batchParameterSets = new ArrayList<>();
            for (Object rowObj : values) {
                @SuppressWarnings("unchecked")
                List<Object> rowValues = (List<Object>) rowObj;

                // Each batch row gets its own parameter set starting from index 1
                List<Map<String, Object>> rowParameters = new ArrayList<>();
                for (int i = 0; i < rowValues.size(); i++) {
                    Object value = rowValues.get(i);
                    int index = i + 1; // Start each batch row from index 1
                    extractParamsTypes(logCollector, rowParameters, value, index, true);
                }

                // Create a parameter set for this batch row
                Map<String, Object> batchParameterSet = Map.of("parameters", rowParameters);
                batchParameterSets.add(batchParameterSet);
            }

            params.put("parameters", batchParameterSets);
            params.put("isBatch", true);
            params.remove("batchValues");
        } else { // not batch
            List<Map<String, Object>> inferredParameters = new ArrayList<>();
            if (params.containsKey("parameters") && params.get("parameters") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> parameters = (List<Map<String, Object>>) params.get("parameters");
                inferredParameters.addAll(parameters);

            }
            for (Object value : values) {
                int index = startIndex.incrementAndGet();
                extractParamsTypes(logCollector, inferredParameters, value, index, false);
            }
            params.put("parameters", inferredParameters);
            params.remove("values");
        }

        logCollector.info("Successfully processed parameter type inference for " + (isBatch ? "batch" : "single") + " operation");
        return dbConfig;
    }

    private void extractParamsTypes(LogCollector logCollector, List<Map<String, Object>> inferredParameters, Object value, int index, boolean isBatch) {
        String inferredType = inferPostgresType(value, logCollector);
        Object processedValue = preprocessValue(value, inferredType, logCollector);
        Map<String, Object> param = new HashMap<>();
        param.put("index", index);
        param.put("type", inferredType);
        param.put("value", processedValue);
        inferredParameters.add(param);
        logCollector.info(isBatch ? "Batch parameters" : "Parameter " + index + ": inferred type '" + inferredType + "' for value: " +
                (value != null ? value.getClass().getSimpleName() : "null"));
    }

    /**
     * Preprocesses values to ensure they're in the correct format for PostgreSQL binding
     */
    private Object preprocessValue(Object value, String inferredType, LogCollector logCollector) {
        if (value == null) return null;

        switch (inferredType.toLowerCase()) {
            case "array" -> {
                if (value instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) value;
                    return list.toArray();
                }
                return value; // Already an array
            }
            case "jsonb" -> {
                if (value instanceof Map || isComplexObject(value)) {
                    // Convert object to JSON string for JSONB binding
                    try {
                        return objectMapper.writeValueAsString(value);
                    } catch (Exception e) {
                        logCollector.warning("Failed to convert object to JSON: " + e.getMessage());
                        return value.toString();
                    }
                }
                return value;
            }
            default -> {
                return value;
            }
        }
    }

    /**
     * Database compiler-style type inference
     * Analyzes Java objects and maps them to PostgreSQL types
     */
    private String inferPostgresType(Object value, LogCollector logCollector) {
        if (value == null) return "string"; // Default to string for nulls

        Class<?> clazz = value.getClass();

        switch (clazz.getSimpleName()) {
            case "String" -> {
                String str = (String) value;
                if (isValidUUID(str)) {
                    return "uuid";
                }
                if (isJsonString(str)) {
                    return "jsonb";
                }
                if (isTimestampString(str)) {
                    return "timestamp";
                }
                return "string";
            }
            case "Integer" -> {
                return "int";
            }
            case "Long", "BigInteger" -> {
                return "long";
            }
            case "Boolean" -> {
                return "boolean";
            }
            case "Double", "Float", "BigDecimal" -> {
                return "numeric";
            }
            case "LocalDate" -> {
                return "date";
            }
            case "LocalDateTime", "ZonedDateTime", "Date" -> {
                return "timestamp";
            }
            case "byte[]" -> {
                return "bytea";
            }
            default -> {
                if (value instanceof List || value instanceof Object[]) {
                    return "array";
                }
                if (value instanceof Map || isComplexObject(value)) {
                    return "jsonb";
                }
                logCollector.info("Unknown type " + clazz.getSimpleName() + ", defaulting to 'string'");
                return "string";
            }
        }
    }

    private boolean isValidUUID(String str) {
        if (str.length() != 36) return false;
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isJsonString(String str) {
        if (str == null || str.trim().isEmpty()) return false;
        String trimmed = str.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private boolean isTimestampString(String str) {
        if (str == null || str.trim().isEmpty()) return false;

        // Common PostgreSQL timestamp formats
        String trimmed = str.trim();

        // ISO 8601 with timezone: 2025-07-10 10:52:38.384986 +00:00
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(\\.\\d+)? [+-]\\d{2}:\\d{2}")) {
            return true;
        }

        // ISO 8601 basic: 2025-07-10T10:52:38.384986Z
        if (trimmed.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z?")) {
            return true;
        }

        // Basic timestamp: 2025-07-10 10:52:38
        return trimmed.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
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
