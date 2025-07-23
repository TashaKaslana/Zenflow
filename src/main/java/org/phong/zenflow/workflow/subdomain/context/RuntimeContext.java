package org.phong.zenflow.workflow.subdomain.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RuntimeContext is a singleton component that manages a shared context
 * and consumer relationships for workflow execution.
 * It allows storing, retrieving, and cleaning up context data based on consumer usage.
 */
@Slf4j
public class RuntimeContext {
    @Getter
    private final Map<String, Object> context = new ConcurrentHashMap<>();
    private final Map<String, List<String>> consumers = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    public void initialize(Map<String, Object> initialContext,
                           Map<String, List<String>> initialConsumers,
                           Map<String, String> initialAliases) {
        if (initialConsumers != null) {
            consumers.putAll(initialConsumers);
        }
        if (initialContext != null) {
            context.putAll(initialContext);
        }
        if (initialAliases != null) {
            aliases.putAll(
                    initialAliases.entrySet().stream()
                            .filter(e -> e.getValue() != null)
                            .collect(
                                    HashMap::new,
                                    (m, e) -> m.put(e.getKey(), e.getValue()),
                                    HashMap::putAll
                            )
            );
        }
    }

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public void putAll(Map<String, Object> entries) {
        if (entries != null) {
            context.putAll(entries);
        }
    }

    public Object get(String key) {
        return context.get(key);
    }

    /**
     * Process and store a node's execution output in the context.
     * Each output value will be stored with a key in the format: "nodeKey.output.propertyName"
     *
     * @param nodeKey   The key of the node that produced the output
     * @param outputMap The map of output values from the node execution
     */
    public void processOutput(String nodeKey, Map<String, Object> outputMap) {
        if (outputMap == null || outputMap.isEmpty()) {
            log.debug("No output to process for node '{}'", nodeKey);
            return;
        }

        log.debug("Processing output for node '{}' with {} values", nodeKey, outputMap.size());

        for (Map.Entry<String, Object> entry : outputMap.entrySet()) {
            String outputKey = nodeKey + ".output." + entry.getKey();
            Object outputValue = entry.getValue();

            // Only store this output if it has consumers or if consumer tracking is not enabled
            if (!consumers.isEmpty() && !hasConsumers(outputKey)) {
                log.debug("Skipping storage of output '{}' as it has no consumers", outputKey);
                continue;
            }

            context.put(outputKey, outputValue);
            log.debug("Stored output '{}' with value type '{}'", outputKey,
                    outputValue != null ? outputValue.getClass().getSimpleName() : "null");
        }
    }

    /**
     * Process output based on output declaration mapping.
     * This is more efficient than storing all outputs as it only stores values
     * that are explicitly declared in the output mapping.
     *
     * @param nodeKey           The key of the node that produced the output
     * @param outputDeclaration The output declaration mapping from node config
     * @param output            The raw output values from execution
     */
    public void processOutput(String nodeKey, Map<String, Object> outputDeclaration, Map<String, Object> output) {
        if (outputDeclaration == null || output == null || output.isEmpty()) {
            log.debug("No output to process for node '{}' with declaration", nodeKey);
            return;
        }

        log.debug("Processing selective output for node '{}' with declaration: {}", nodeKey, outputDeclaration);

        for (Map.Entry<String, Object> entry : outputDeclaration.entrySet()) {
            String outputProperty = entry.getKey();
            Object templateOrMapping = entry.getValue();

            // Determine the target key where the output should be stored
            String targetKey;
            Object value;

            if (templateOrMapping instanceof String template && TemplateEngine.isTemplate(template)) {
                // If the value is a template reference, extract the target key
                targetKey = template.substring(2, template.length() - 2).trim();
                value = output.get(outputProperty);

                // Skip storing if there are no consumers for this key
                if (!consumers.isEmpty() && !hasConsumers(targetKey)) {
                    log.debug("Skipping storage of output to '{}' as it has no consumers", targetKey);
                    continue;
                }
            } else {
                // Direct mapping: key in outputDeclaration -> standard node output key format
                targetKey = nodeKey + ".output." + outputProperty;
                value = output.get(outputProperty);

                // Skip storing if there are no consumers for this key
                if (!consumers.isEmpty() && !hasConsumers(targetKey)) {
                    log.debug("Skipping storage of output to '{}' as it has no consumers in explicit output", targetKey);
                    continue;
                }
            }

            // Only store if we have a value
            if (value != null) {
                context.put(targetKey, value);
                log.debug("Stored selective output '{}' with value type '{}'",
                        targetKey, value.getClass().getSimpleName());
            } else {
                log.warn("Output value for '{}' is null, not storing", targetKey);
            }
        }
    }

    /**
     * Process output according to consumer information already in the RuntimeContext.
     * This is the most efficient approach as it only stores values that are
     * actually needed by downstream nodes.
     *
     * @param nodeKey The key of the node that produced the output
     * @param output  The raw output values from execution
     */
    public void processOutputWithMetadata(String nodeKey, Map<String, Object> output) {
        if (output == null || output.isEmpty()) {
            log.debug("No output to process for node '{}'.", nodeKey);
            return;
        }

        log.debug("Processing context-guided output for node '{}' with {} values", nodeKey, output.size());

        for (Map.Entry<String, Object> entry : output.entrySet()) {
            String outputProperty = entry.getKey();
            String outputKey = nodeKey + ".output." + outputProperty;
            Object value = entry.getValue();

            // Check if this output key has any consumers using the existing consumers map
            if (!hasConsumers(outputKey)) {
                log.debug("Skipping storage of '{}' as it has no registered consumers", outputKey);
                continue;
            }

            // Store the value as it has consumers
            context.put(outputKey, value);
            log.debug("Stored context-guided output '{}' with value type '{}' for consumers: {}",
                    outputKey, value != null ? value.getClass().getSimpleName() : "null", getConsumers(outputKey));
        }
    }

    /**
     * Get a value from the context and mark it as consumed by the specified node.
     * This method also triggers garbage collection for the key if there are no more consumers.
     * If the key is a template reference, it will be resolved before returning.
     *
     * @param nodeKey The key of the node consuming this value
     * @param key     The key or template to retrieve from the context
     * @return The resolved value from the context, or null if not found
     */
    public Object getAndClean(String nodeKey, String key) {
        // If this is a template, extract the reference and resolve it
        if (TemplateEngine.isTemplate(key)) {
            String refKey = TemplateEngine.extractRefs(key).stream().findFirst().orElse(null);
            if (refKey != null) {
                // Check if it's an alias that needs resolution
                String resolvedKey = resolveAlias(refKey);
                return getAndMarkConsumed(nodeKey, resolvedKey);
            }
            return null;
        }

        // Direct key access
        return getAndMarkConsumed(nodeKey, key);
    }

    /**
     * Gets a value from the context and marks it as consumed, handling garbage collection
     *
     * @param nodeKey The consuming node
     * @param key     The key to access
     * @return The value, or null if not found
     */
    private Object getAndMarkConsumed(String nodeKey, String key) {
        if (!context.containsKey(key)) {
            return null;
        }

        Object value = context.get(key);

        // Remove the current node from the consumers list for this key
        removeConsumer(key, nodeKey);

        // Perform garbage collection - remove the key if no more consumers
        performGarbageCollection(key);

        return value;
    }

    /**
     * Resolves a possible alias to its actual reference key
     *
     * @param key The key that might be an alias
     * @return The resolved key
     */
    private String resolveAlias(String key) {
        Object resolvedKey = aliases.get(key);
        if (resolvedKey != null) {
            return resolvedKey.toString();
        } else {
            return key;
        }
    }

    /**
     * Resolve a configuration object using the RuntimeContext.
     * Replaces all template references with their actual values in input.
     *
     * @param nodeKey The key of the node using this configuration
     * @param config  The configuration object with templates
     * @return A new configuration object with resolved values
     */
    public WorkflowConfig resolveConfig(String nodeKey, WorkflowConfig config) {
        if (config == null || config.input() == null) {
            return config;
        }

        Map<String, Object> resolvedInput = resolve(nodeKey, config.input());
        return new WorkflowConfig(resolvedInput, config.output());
    }

    private Map<String, Object> resolve(String nodeKey, Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            result.put(entry.getKey(), resolveValue(nodeKey, entry.getValue()));
        }
        return result;
    }

    private Object resolveValue(String nodeKey, Object value) {
        return switch (value) {
            case String str -> {
                if (!TemplateEngine.isTemplate(str)) {
                    yield str;
                }
                List<String> refs = TemplateEngine.extractRefs(str);
                if (refs.size() == 1) {
                    String expr = refs.getFirst();
                    if (str.trim().equals("{{" + expr + "}}")) {
                        // Single template, can be any type
                        if (expr.matches("[a-zA-Z0-9._-]+\\(.*\\)")) {
                            yield TemplateEngine.evaluateFunction(expr);
                        } else {
                            yield getAndClean(nodeKey, expr);
                        }
                    }
                }
                // For complex templates, resolve all and return as string
                yield TemplateEngine.resolveTemplate(str, this.context);
            }
            case Map<?, ?> map ->
                // To be safe, we assume the map is Map<String, Object>
                    resolve(nodeKey, ObjectConversion.convertObjectToMap(map));
            case List<?> list -> list.stream()
                    .map(item -> resolveValue(nodeKey, item))
                    .toList();
            default -> value;
        };
    }

    /**
     * Remove a specific consumer from a key's consumer list
     */
    private void removeConsumer(String key, String nodeKey) {
        List<String> keyConsumers = consumers.get(key);
        if (keyConsumers != null) {
            keyConsumers.remove(nodeKey);
            log.debug("Removed consumer '{}' from key '{}'", nodeKey, key);

            // If no more consumers, remove the entry completely
            if (keyConsumers.isEmpty()) {
                consumers.remove(key);
                log.debug("No more consumers for key '{}', removed from consumers map", key);
            }
        }
    }

    /**
     * Perform garbage collection by removing context entries that have no consumers
     */
    private void performGarbageCollection(String key) {
        List<String> keyConsumers = consumers.get(key);

        // If there are no consumers left for this key, remove it from context
        if (keyConsumers == null || keyConsumers.isEmpty()) {
            Object removedValue = context.remove(key);
            if (removedValue != null) {
                log.debug("Garbage collected key '{}' from context", key);
            }
        }
    }

    /**
     * Check if a key has any remaining consumers
     */
    public boolean hasConsumers(String key) {
        List<String> keyConsumers = consumers.get(key);
        return keyConsumers != null && !keyConsumers.isEmpty();
    }

    /**
     * Get the list of consumers for a specific key
     */
    public List<String> getConsumers(String key) {
        List<String> keyConsumers = consumers.get(key);
        return keyConsumers != null ? new ArrayList<>(keyConsumers) : new ArrayList<>();
    }

    /**
     * Manual garbage collection for all keys without consumers
     */
    public void garbageCollect() {
        List<String> keysToRemove = new ArrayList<>();

        for (String key : context.keySet()) {
            if (!hasConsumers(key)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            context.remove(key);
            log.debug("Manual garbage collection removed key '{}'", key);
        }

        if (!keysToRemove.isEmpty()) {
            log.info("Manual garbage collection removed {} unused context entries", keysToRemove.size());
        }
    }

    /**
     * Get current context size for monitoring
     */
    public int getContextSize() {
        return context.size();
    }

    /**
     * Get current consumers map size for monitoring
     */
    public int getConsumersSize() {
        return consumers.size();
    }

    /**
     * Clear all context data (useful for testing or cleanup)
     */
    public void clear() {
        context.clear();
        consumers.clear();
        log.debug("RuntimeContext cleared");
    }
}
