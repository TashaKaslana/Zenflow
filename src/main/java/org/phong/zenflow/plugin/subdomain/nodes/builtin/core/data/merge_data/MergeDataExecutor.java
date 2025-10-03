package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.data.merge_data;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
public class MergeDataExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logPublisher = context.getLogPublisher();
        logPublisher.info("Starting data merge operation");
        log.debug("Executing MergeDataExecutor with config: {}", config);

        Map<String, Object> input = config.input();
        if (input == null || input.isEmpty()) {
            logPublisher.error("Input configuration is missing or empty");
            return ExecutionResult.error("Input configuration is required");
        }

        // Extract and validate sources
        List<Map<String, Object>> sources = extractSources(input, logPublisher);
        if (sources.isEmpty()) {
            return ExecutionResult.error("No valid source data provided");
        }

        // Extract and validate strategy
        MergeStrategy strategy = extractStrategy(input, logPublisher);

        // Additional merge options
        MergeOptions options = extractMergeOptions(input);

        logPublisher.info("Merging {} sources using {} strategy", sources.size(), strategy);

        // Perform the merge operation
        Map<String, Object> result = performMerge(sources, strategy, options, logPublisher);

        logPublisher.success("Data merge completed successfully. Result contains {} items",
                    result.containsKey("data") ? getDataSize(result.get("data")) : 0);

        return ExecutionResult.success(result);
    }

    private List<Map<String, Object>> extractSources(Map<String, Object> input, NodeLogPublisher logPublisher) {
        Object sourcesObj = input.get("sources");
        if (sourcesObj == null) {
            logPublisher.warning("No 'sources' field found in input, checking for direct data fields");
            // Support alternative input format where data is provided directly
            return extractDirectSources(input, logPublisher);
        }

        List<Map<String, Object>> sources = ObjectConversion.safeConvert(sourcesObj, new TypeReference<>() {});
        if (sources == null) {
            logPublisher.error("Sources field is not in the expected format (List<Map<String, Object>>)");
            throw new IllegalArgumentException("Invalid sources format");
        }

        // Validate each source
        List<Map<String, Object>> validSources = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            Map<String, Object> source = sources.get(i);
            if (source != null && source.containsKey("data")) {
                validSources.add(source);
            } else {
                logPublisher.warning("Source at index {} is invalid or missing 'data' field, skipping", i);
            }
        }

        return validSources;
    }

    private List<Map<String, Object>> extractDirectSources(Map<String, Object> input, NodeLogPublisher logPublisher) {
        List<Map<String, Object>> sources = new ArrayList<>();

        // Look for common data field names
        String[] possibleDataFields = {"data", "items", "values", "content", "payload"};

        for (String field : possibleDataFields) {
            if (input.containsKey(field)) {
                Map<String, Object> source = new HashMap<>();
                source.put("data", input.get(field));
                source.put("source_field", field);
                sources.add(source);
                logPublisher.info("Found data in field: {}", field);
            }
        }

        return sources;
    }

    private MergeStrategy extractStrategy(Map<String, Object> input, NodeLogPublisher logPublisher) {
        Object strategyObj = input.get("strategy");

        switch (strategyObj) {
            case null -> {
                logPublisher.info("No strategy specified, using default: DEEP_MERGE");
                return MergeStrategy.DEEP_MERGE;
            }
            case MergeStrategy mergeStrategy -> {
                return mergeStrategy;
            }
            case String strategyStr -> {
                try {
                    return MergeStrategy.fromString(strategyStr);
                } catch (IllegalArgumentException e) {
                    logPublisher.error("Invalid merge strategy: {}. Available strategies: {}",
                            strategyStr, Arrays.toString(MergeStrategy.values()));
                    throw e;
                }
            }
            default -> log.debug("Unsupported strategy type: {}", strategyObj.getClass());
        }

        throw new IllegalArgumentException("Strategy must be a string or MergeStrategy enum but was: "
                + strategyObj.getClass().getName());
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
            Object conflictObj = input.get("conflict_resolution");
            try {
                if (conflictObj instanceof ConflictResolution cr) {
                    options.conflictResolution = cr;
                } else {
                    String resolution = ObjectConversion.safeConvert(conflictObj, String.class);
                    if (resolution == null) {
                        resolution = conflictObj.toString();
                    }
                    options.conflictResolution = ConflictResolution.fromString(resolution);
                }
            } catch (Exception e) {
                log.error("Invalid conflict_resolution value: {}", conflictObj, e);
                throw new IllegalArgumentException("Unable to parse conflict_resolution: " + conflictObj);
            }
        }

        if (input.containsKey("max_depth")) {
            Object maxDepthObj = input.get("max_depth");
            try {
                Integer maxDepth = ObjectConversion.safeConvert(maxDepthObj, Integer.class);
                if (maxDepth == null) {
                    maxDepth = Integer.parseInt(maxDepthObj.toString());
                }
                options.maxDepth = maxDepth;
            } catch (Exception e) {
                log.error("Invalid max_depth value: {}", maxDepthObj, e);
                throw new IllegalArgumentException("Unable to parse max_depth: " + maxDepthObj);
            }
        }

        return options;
    }

    private Map<String, Object> performMerge(List<Map<String, Object>> sources,
                                            MergeStrategy strategy,
                                            MergeOptions options,
                                            NodeLogPublisher logPublisher) {

        List<Object> dataList = sources.stream()
                .map(source -> source.get("data"))
                .filter(data -> {
                    if (strategy == MergeStrategy.DEEP_MERGE) {
                        return data != null; // deep merge cannot handle null sources
                    }
                    return data != null || !options.ignoreNulls;
                })
                .collect(Collectors.toList());

        Object mergedData = switch (strategy) {
            case CONCAT -> concatData(dataList, logPublisher);
            case DEEP_MERGE -> deepMergeData(dataList, options, logPublisher);
            case COLLECT -> collectData(dataList, options, logPublisher);
            case OVERWRITE -> overwriteData(dataList, options, logPublisher);
            case SHALLOW_MERGE -> shallowMergeData(dataList, options, logPublisher);
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

    private Object concatData(List<Object> dataList, NodeLogPublisher logPublisher) {
        List<Object> result = new ArrayList<>();

        for (Object data : dataList) {
            if (data instanceof List<?> list) {
                result.addAll(list);
            } else if (data != null && data.getClass().isArray()) {
                int length = Array.getLength(data);
                for (int i = 0; i < length; i++) {
                    result.add(Array.get(data, i));
                }
            } else {
                result.add(data);
            }
        }

        logPublisher.info("Concatenated {} items into a single list", result.size());
        return result;
    }

    private Object deepMergeData(List<Object> dataList, MergeOptions options, NodeLogPublisher logPublisher) {
        Map<String, Object> result = options.preserveOrder ? new LinkedHashMap<>() : new HashMap<>();
        int mergedFields = 0;

        for (Object data : dataList) {
            if (data instanceof Map<?, ?> map) {
                mergedFields += mergeMapIntoResult(result, map, options, 0);
            } else if (data == null) {
                logPublisher.warning("Cannot deep merge null data source");
            } else {
                logPublisher.warning("Cannot deep merge non-map data: {}", data.getClass().getSimpleName());
            }
        }

        logPublisher.info("Deep merged {} fields across {} maps", mergedFields, dataList.size());
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

    private Object shallowMergeData(List<Object> dataList, MergeOptions options, NodeLogPublisher logPublisher) {
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

        logPublisher.info("Shallow merged {} maps into {} fields", dataList.size(), result.size());
        return result;
    }

    private Object collectData(List<Object> dataList, MergeOptions options, NodeLogPublisher logPublisher) {
        List<Object> filtered = dataList.stream()
                .filter(data -> data != null || !options.ignoreNulls)
                .collect(Collectors.toList());

        logPublisher.info("Collected {} data items", filtered.size());
        return filtered;
    }

    private Object overwriteData(List<Object> dataList, MergeOptions options, NodeLogPublisher logPublisher) {
        if (dataList.isEmpty()) {
            return null;
        }

        // Find the last non-null value if ignoring nulls
        if (options.ignoreNulls) {
            for (int i = dataList.size() - 1; i >= 0; i--) {
                Object data = dataList.get(i);
                if (data != null) {
                    logPublisher.info("Using data from source {} (last non-null)", i);
                    return data;
                }
            }
        }

        Object result = dataList.getLast();
        logPublisher.info("Using data from last source (index {})", dataList.size() - 1);
        return result;
    }

    private int getDataSize(Object data) {
        if (data instanceof Collection<?> collection) {
            return collection.size();
        } else if (data instanceof Map<?, ?> map) {
            return map.size();
        } else if (data != null && data.getClass().isArray()) {
            return Array.getLength(data);
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
