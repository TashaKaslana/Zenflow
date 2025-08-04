package org.phong.zenflow.plugin.subdomain.executors.builtin.data.merge_data;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class MergeDataExecutor implements PluginNodeExecutor {

    @Override
    public String key() {
        return "core:merge_data:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logs = new LogCollector();
        try {
            logs.info("Starting data merge operation");
            log.debug("Executing MergeDataExecutor with config: {}", config);

            Map<String, Object> input = config.input();
            if (input == null || input.isEmpty()) {
                logs.error("Input configuration is missing or empty");
                return ExecutionResult.error("Input configuration is required", logs.getLogs());
            }

            // Extract and validate sources
            List<Map<String, Object>> sources = extractSources(input, logs);
            if (sources.isEmpty()) {
                return ExecutionResult.error("No valid source data provided", logs.getLogs());
            }

            // Extract and validate strategy
            MergeStrategy strategy = extractStrategy(input, logs);

            // Additional merge options
            MergeOptions options = extractMergeOptions(input);

            logs.info("Merging {} sources using {} strategy", sources.size(), strategy);

            // Perform the merge operation
            Map<String, Object> result = performMerge(sources, strategy, options, logs);

            logs.success("Data merge completed successfully. Result contains {} items",
                        result.containsKey("data") ? getDataSize(result.get("data")) : 0);

            return ExecutionResult.success(result, logs.getLogs());

        } catch (IllegalArgumentException e) {
            logs.error("Invalid configuration: {}", e.getMessage());
            log.debug("Configuration validation failed", e);
            return ExecutionResult.error("Configuration error: " + e.getMessage(), logs.getLogs());
        } catch (Exception e) {
            logs.error("Unexpected error during data merge: {}", e.getMessage());
            log.error("Data merge operation failed", e);
            return ExecutionResult.error("Merge operation failed: " + e.getMessage(), logs.getLogs());
        }
    }

    private List<Map<String, Object>> extractSources(Map<String, Object> input, LogCollector logs) {
        Object sourcesObj = input.get("sources");
        if (sourcesObj == null) {
            logs.warning("No 'sources' field found in input, checking for direct data fields");
            // Support alternative input format where data is provided directly
            return extractDirectSources(input, logs);
        }

        List<Map<String, Object>> sources = ObjectConversion.safeConvert(sourcesObj, new TypeReference<>() {});
        if (sources == null) {
            logs.error("Sources field is not in the expected format (List<Map<String, Object>>)");
            throw new IllegalArgumentException("Invalid sources format");
        }

        // Validate each source
        List<Map<String, Object>> validSources = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            Map<String, Object> source = sources.get(i);
            if (source != null && source.containsKey("data")) {
                validSources.add(source);
            } else {
                logs.warning("Source at index {} is invalid or missing 'data' field, skipping", i);
            }
        }

        return validSources;
    }

    private List<Map<String, Object>> extractDirectSources(Map<String, Object> input, LogCollector logs) {
        List<Map<String, Object>> sources = new ArrayList<>();

        // Look for common data field names
        String[] possibleDataFields = {"data", "items", "values", "content", "payload"};

        for (String field : possibleDataFields) {
            if (input.containsKey(field)) {
                Map<String, Object> source = new HashMap<>();
                source.put("data", input.get(field));
                source.put("source_field", field);
                sources.add(source);
                logs.info("Found data in field: {}", field);
            }
        }

        return sources;
    }

    private MergeStrategy extractStrategy(Map<String, Object> input, LogCollector logs) {
        Object strategyObj = input.get("strategy");

        switch (strategyObj) {
            case null -> {
                logs.info("No strategy specified, using default: DEEP_MERGE");
                return MergeStrategy.DEEP_MERGE;
            }
            case MergeStrategy mergeStrategy -> {
                return mergeStrategy;
            }
            case String strategyStr -> {
                try {
                    return MergeStrategy.fromString(strategyStr);
                } catch (IllegalArgumentException e) {
                    logs.error("Invalid merge strategy: {}. Available strategies: {}",
                            strategyStr, Arrays.toString(MergeStrategy.values()));
                    throw e;
                }
            }
            default -> {
            }
        }

        throw new IllegalArgumentException("Strategy must be a string or MergeStrategy enum");
    }

    private MergeOptions extractMergeOptions(Map<String, Object> input) {
        MergeOptions options = new MergeOptions();

        // Extract options from input
        if (input.containsKey("preserve_order")) {
            options.preserveOrder = Boolean.TRUE.equals(input.get("preserve_order"));
        }

        if (input.containsKey("ignore_nulls")) {
            options.ignoreNulls = Boolean.TRUE.equals(input.get("ignore_nulls"));
        }

        if (input.containsKey("conflict_resolution")) {
            String resolution = (String) input.get("conflict_resolution");
            options.conflictResolution = ConflictResolution.fromString(resolution);
        }

        if (input.containsKey("max_depth")) {
            Object maxDepthObj = input.get("max_depth");
            if (maxDepthObj instanceof Number) {
                options.maxDepth = ((Number) maxDepthObj).intValue();
            }
        }

        return options;
    }

    private Map<String, Object> performMerge(List<Map<String, Object>> sources,
                                            MergeStrategy strategy,
                                            MergeOptions options,
                                            LogCollector logs) {

        List<Object> dataList = sources.stream()
                .map(source -> source.get("data"))
                .filter(data -> data != null || !options.ignoreNulls)
                .collect(Collectors.toList());

        Object mergedData = switch (strategy) {
            case CONCAT -> concatData(dataList, logs);
            case DEEP_MERGE -> deepMergeData(dataList, options, logs);
            case COLLECT -> collectData(dataList, options, logs);
            case OVERWRITE -> overwriteData(dataList, options, logs);
            case SHALLOW_MERGE -> shallowMergeData(dataList, options, logs);
        };

        // Create comprehensive result
        Map<String, Object> result = new HashMap<>();
        result.put("data", mergedData);
        result.put("strategy_used", strategy.name());
        result.put("sources_processed", sources.size());
        result.put("merge_timestamp", System.currentTimeMillis());

        // Add metadata about the merge
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source_count", sources.size());
        metadata.put("data_size", getDataSize(mergedData));
        metadata.put("options", Map.of(
            "preserve_order", options.preserveOrder,
            "ignore_nulls", options.ignoreNulls,
            "conflict_resolution", options.conflictResolution.name(),
            "max_depth", options.maxDepth
        ));
        result.put("metadata", metadata);

        return result;
    }

    private Object concatData(List<Object> dataList, LogCollector logs) {
        List<Object> result = new ArrayList<>();

        for (Object data : dataList) {
            if (data instanceof List<?> list) {
                result.addAll(list);
            } else if (data instanceof Object[] array) {
                result.addAll(Arrays.asList(array));
            } else {
                result.add(data);
            }
        }

        logs.info("Concatenated {} items into a single list", result.size());
        return result;
    }

    private Object deepMergeData(List<Object> dataList, MergeOptions options, LogCollector logs) {
        Map<String, Object> result = options.preserveOrder ? new LinkedHashMap<>() : new HashMap<>();
        int mergedFields = 0;

        for (Object data : dataList) {
            if (data instanceof Map<?, ?> map) {
                mergedFields += mergeMapIntoResult(result, map, options, 0);
            } else {
                logs.warning("Cannot deep merge non-map data: {}", data.getClass().getSimpleName());
            }
        }

        logs.info("Deep merged {} fields across {} maps", mergedFields, dataList.size());
        return result;
    }

    private int mergeMapIntoResult(Map<String, Object> result, Map<?, ?> source, MergeOptions options, int currentDepth) {
        if (currentDepth >= options.maxDepth) {
            return 0;
        }

        int mergedCount = 0;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();

            if (value == null && options.ignoreNulls) {
                continue;
            }

            if (result.containsKey(key)) {
                // Handle conflicts
                Object existingValue = result.get(key);
                value = resolveConflict(existingValue, value, options, currentDepth);
            }

            result.put(key, value);
            mergedCount++;
        }

        return mergedCount;
    }

    private Object resolveConflict(Object existing, Object incoming, MergeOptions options, int currentDepth) {
        return switch (options.conflictResolution) {
            case KEEP_FIRST -> existing;
            case KEEP_LAST -> incoming;
            case MERGE_RECURSIVE -> {
                if (existing instanceof Map && incoming instanceof Map && currentDepth < options.maxDepth) {
                    Map<String, Object> merged = new HashMap<>(ObjectConversion.convertObjectToMap(existing));
                    mergeMapIntoResult(merged, (Map<?, ?>) incoming, options, currentDepth + 1);
                    yield merged;
                } else {
                    yield incoming; // Fallback to keeping last
                }
            }
            case COMBINE_ARRAYS -> {
                if (existing instanceof List && incoming instanceof List) {
                    List<Object> combined = new ArrayList<>(ObjectConversion.convertObjectToList(existing, Object.class));
                    combined.addAll(ObjectConversion.convertObjectToList(incoming, Object.class));
                    yield combined;
                } else {
                    yield incoming; // Fallback to keeping last
                }
            }
        };
    }

    private Object shallowMergeData(List<Object> dataList, MergeOptions options, LogCollector logs) {
        Map<String, Object> result = options.preserveOrder ? new LinkedHashMap<>() : new HashMap<>();

        for (Object data : dataList) {
            if (data instanceof Map<?, ?> map) {
                map.forEach((key, value) -> {
                    if (value != null || !options.ignoreNulls) {
                        result.put(String.valueOf(key), value);
                    }
                });
            }
        }

        logs.info("Shallow merged {} maps into {} fields", dataList.size(), result.size());
        return result;
    }

    private Object collectData(List<Object> dataList, MergeOptions options, LogCollector logs) {
        List<Object> filtered = dataList.stream()
                .filter(data -> data != null || !options.ignoreNulls)
                .collect(Collectors.toList());

        logs.info("Collected {} data items", filtered.size());
        return filtered;
    }

    private Object overwriteData(List<Object> dataList, MergeOptions options, LogCollector logs) {
        if (dataList.isEmpty()) {
            return null;
        }

        // Find the last non-null value if ignoring nulls
        if (options.ignoreNulls) {
            for (int i = dataList.size() - 1; i >= 0; i--) {
                Object data = dataList.get(i);
                if (data != null) {
                    logs.info("Using data from source {} (last non-null)", i);
                    return data;
                }
            }
        }

        Object result = dataList.getLast();
        logs.info("Using data from last source (index {})", dataList.size() - 1);
        return result;
    }

    private int getDataSize(Object data) {
        if (data instanceof Collection<?> collection) {
            return collection.size();
        } else if (data instanceof Map<?, ?> map) {
            return map.size();
        } else if (data instanceof Object[] array) {
            return array.length;
        }
        return data != null ? 1 : 0;
    }

    public enum MergeStrategy {
        CONCAT,
        DEEP_MERGE,
        SHALLOW_MERGE,
        COLLECT,
        OVERWRITE;

        public static MergeStrategy fromString(String strategy) {
            return switch (strategy.trim().toLowerCase()) {
                case "concat", "concatenate", "join" -> CONCAT;
                case "deep_merge", "deepmerge", "deep", "recursive" -> DEEP_MERGE;
                case "shallow_merge", "shallowmerge", "shallow" -> SHALLOW_MERGE;
                case "collect", "gather", "list" -> COLLECT;
                case "overwrite", "last", "override", "replace" -> OVERWRITE;
                default -> throw new IllegalArgumentException("Unknown merge strategy: " + strategy);
            };
        }
    }

    public enum ConflictResolution {
        KEEP_FIRST,
        KEEP_LAST,
        MERGE_RECURSIVE,
        COMBINE_ARRAYS;

        public static ConflictResolution fromString(String resolution) {
            return switch (resolution.trim().toLowerCase()) {
                case "keep_first", "first", "original" -> KEEP_FIRST;
                case "keep_last", "last", "override" -> KEEP_LAST;
                case "merge_recursive", "recursive", "merge" -> MERGE_RECURSIVE;
                case "combine_arrays", "combine", "concat_arrays" -> COMBINE_ARRAYS;
                default -> KEEP_LAST; // Default fallback
            };
        }
    }

    private static class MergeOptions {
        boolean preserveOrder = true;
        boolean ignoreNulls = false;
        ConflictResolution conflictResolution = ConflictResolution.KEEP_LAST;
        int maxDepth = 10;
    }
}
