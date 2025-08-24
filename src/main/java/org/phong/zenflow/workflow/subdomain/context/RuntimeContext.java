package org.phong.zenflow.workflow.subdomain.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final Map<String, Set<String>> consumers = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    // Loop-aware cleanup management
    private final Map<String, Map<String, Set<String>>> pendingLoopCleanup = new ConcurrentHashMap<>();
    private final Set<String> activeLoops = new HashSet<>();

    public void initialize(Map<String, Object> initialContext,
                           Map<String, Set<String>> initialConsumers,
                           Map<String, String> initialAliases) {
        if (initialConsumers != null) {
            initialConsumers.forEach((key, value) -> {
                if (value != null) {
                    consumers.put(key, new HashSet<>(value));
                }
            });
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
     * Process output according to consumer information already in the RuntimeContext.
     * This is the most efficient approach as it only stores values that are
     * actually needed by downstream nodes.
     *
     * @param outputKey The initial key of the node that produced the output
     * @param output    The raw output values from execution
     */
    public void processOutputWithMetadata(String outputKey, Map<String, Object> output) {
        if (output == null || output.isEmpty()) {
            log.debug("No output to process for node '{}'.", outputKey);
            return;
        }

        log.debug("Processing context-guided output for node '{}' with {} values", outputKey, output.size());

        for (Map.Entry<String, Object> entry : output.entrySet()) {
            String outputProperty = entry.getKey();
            String currentOutputKey = outputKey.concat(".").concat(outputProperty);
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> map) {
                processOutputWithMetadata(currentOutputKey, ObjectConversion.convertObjectToMap(map));
            }

            // Check if this output key has any consumers using the existing consumers map
            if (!hasConsumers(currentOutputKey)) {
                log.debug("Skipping storage of '{}' as it has no registered consumers", currentOutputKey);
                continue;
            }

            // Store the value as it has consumers
            context.put(currentOutputKey, value);
            log.debug("Stored context-guided output '{}' with value type '{}' for consumers: {}",
                    currentOutputKey, value != null ? value.getClass().getSimpleName() : "null", getConsumers(currentOutputKey));
        }
    }

    /**
     * Get a value from the context and mark it as consumed by the specified node.
     * This method also triggers garbage collection for the key if there are no more consumers.
     * If the key is a template reference, it will be resolved before returning.
     * Supports default values using syntax like {{ref:defaultValue}}
     *
     * @param nodeKey The key of the node consuming this value
     * @param key     The key or template to retrieve from the context
     * @return The resolved value from the context, or null if not found
     */
    public Object getAndClean(String nodeKey, String key) {
        // If this is a template, extract the reference and resolve it
        if (TemplateEngine.isTemplate(key)) {
            return resolveSingleTemplate(nodeKey, key);
        }

        // This path handles cases where a non-template key might be an aliases
        String resolvedKey = resolveAlias(key);
        return getAndMarkConsumed(nodeKey, resolvedKey);
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
     * Resolves a possible aliases to its actual reference key
     *
     * @param key The key that might be an aliases
     * @return The resolved key
     */
    private String resolveAlias(String key) {
        Object resolvedKey = aliases.get(key);
        if (resolvedKey != null) {
            resolvedKey = resolvedKey.toString().substring(2, resolvedKey.toString().length() - 2).trim();
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
        if (value == null) {
            return null;
        }

        return switch (value) {
            case String str -> {
                if (!TemplateEngine.isTemplate(str)) {
                    yield str;
                }

                // For single-reference templates like "{{ref}}", "{{ref:defaultValue}}", or "{{func()}}"
                // This ensures the resolved value maintains its original type (e.g., Integer, Boolean)
                Set<String> fullRefs = TemplateEngine.extractFullRefs(str);
                if (fullRefs.size() == 1) {
                    String fullExpr = fullRefs.iterator().next();
                    if (str.trim().equals("{{" + fullExpr + "}}")) {
                        // It's a single, standalone template expression
                        yield resolveSingleTemplate(nodeKey, str);
                    }
                }

                // For complex templates with multiple references or surrounding text, e.g., "Hello {{user.name}}"
                // This will always resolve to a String.
                yield resolveComplexTemplate(nodeKey, str);
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

    private Object resolveSingleTemplate(String nodeKey, String template) {
        String activeLoop = getActiveLoop();
        if (activeLoop != null) {
            return getAndCleanInLoop(nodeKey, template, activeLoop);
        }

        // --- Start of self-contained logic for non-looping context ---
        String resolvedKey;
        if (TemplateEngine.isTemplate(template)) {
            String fullExpr = TemplateEngine.extractFullRefs(template).stream().findFirst().orElse(null);
            if (fullExpr == null) return null; // Malformed template

            if (fullExpr.matches("^[a-zA-Z0-9_.-]+\\([^()]*\\)$")) {
                return TemplateEngine.evaluateFunction(fullExpr);
            }

            String refKey = TemplateEngine.extractRefs(template).stream().findFirst().orElse(null);
            if (refKey == null) return null; // Malformed template

            resolvedKey = resolveAlias(refKey);
        } else {
            resolvedKey = resolveAlias(template);
        }

        Object value = getAndMarkConsumed(nodeKey, resolvedKey);

        if (value == null && TemplateEngine.isTemplate(template)) {
            Object defaultValue = TemplateEngine.extractDefaultValue(template);
            if (defaultValue != null) {
                log.debug("Reference in '{}' resolved to null, using default value: {}", template, defaultValue);
                return defaultValue;
            }
        }

        return value;
    }

    private String resolveComplexTemplate(String nodeKey, String template) {
        String result = template;
        Set<String> fullRefs = TemplateEngine.extractFullRefs(template);

        for (String fullRef : fullRefs) {
            String templateRef = "{{" + fullRef + "}}";
            Object resolvedValue = resolveSingleTemplate(nodeKey, templateRef);

            if (resolvedValue != null) {
                result = result.replace(templateRef, resolvedValue.toString());
            } else {
                log.warn("Reference '{}' in complex template resolved to null with no default. Returning original template.", fullRef);
                return template;
            }
        }
        return result;
    }

    /**
     * Remove a specific consumer from a key's consumer list
     */
    private void removeConsumer(String key, String nodeKey) {
        Set<String> keyConsumers = consumers.get(key);
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
        Set<String> keyConsumers = consumers.get(key);

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
        Set<String> keyConsumers = consumers.get(key);
        return keyConsumers != null && !keyConsumers.isEmpty();
    }

    /**
     * Get the list of consumers for a specific key
     */
    public List<String> getConsumers(String key) {
        Set<String> keyConsumers = consumers.get(key);
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
     * Remove a specific key from context and consumers (useful for manual cleanup)
     *
     * @param key The key to remove
     */
    public void remove(String key) {
        context.remove(key);
        consumers.remove(key);
        log.debug("Removed key '{}' from context and consumers", key);
    }

    /**
     * Clear all context data (useful for testing or cleanup)
     */
    public void clear() {
        context.clear();
        consumers.clear();
        pendingLoopCleanup.clear();
        activeLoops.clear();
        log.debug("RuntimeContext cleared");
    }

    // ========== Loop-aware context management methods ==========

    /**
     * Start a loop context. This tells the RuntimeContext to defer cleanup operations
     * for context values accessed within this loop until the loop completes.
     *
     * @param loopNodeKey The unique identifier for the loop node
     */
    public void startLoop(String loopNodeKey) {
        activeLoops.add(loopNodeKey);
        pendingLoopCleanup.put(loopNodeKey, new ConcurrentHashMap<>());
        log.debug("Started loop context for node: {}", loopNodeKey);
    }

    /**
     * End a loop context and perform all deferred cleanup operations.
     * This will clean up all context values that were accessed during the loop
     * and have no remaining consumers.
     *
     * @param loopNodeKey The unique identifier for the loop node
     */
    public void endLoop(String loopNodeKey) {
        if (!activeLoops.contains(loopNodeKey)) {
            log.warn("Attempted to end loop '{}' that was not started", loopNodeKey);
            return;
        }

        activeLoops.remove(loopNodeKey);
        Map<String, Set<String>> pendingCleanup = pendingLoopCleanup.remove(loopNodeKey);

        if (pendingCleanup != null) {
            int cleanedCount = 0;
            for (Map.Entry<String, Set<String>> entry : pendingCleanup.entrySet()) {
                String key = entry.getKey();
                Set<String> consumersToRemove = entry.getValue();

                // Remove all pending consumers for this key
                Set<String> keyConsumers = consumers.get(key);
                if (keyConsumers != null) {
                    keyConsumers.removeAll(consumersToRemove);
                    if (keyConsumers.isEmpty()) {
                        consumers.remove(key);
                    }
                }

                // Perform garbage collection for this key
                if (!hasConsumers(key)) {
                    Object removedValue = context.remove(key);
                    if (removedValue != null) {
                        cleanedCount++;
                        log.debug("Loop cleanup removed key '{}' from context", key);
                    }
                }
            }

            if (cleanedCount > 0) {
                log.debug("Loop '{}' cleanup removed {} context entries", loopNodeKey, cleanedCount);
            }
        }

        log.debug("Ended loop context for node: {}", loopNodeKey);
    }

    /**
     * Get a value from context within a loop context. This version defers cleanup
     * until the loop completes, allowing multiple iterations to access the same values.
     * Supports default values using syntax like {{ref:defaultValue}}
     *
     * @param nodeKey The key of the node consuming this value
     * @param key The key or template to retrieve from the context
     * @param loopNodeKey The identifier of the active loop
     * @return The resolved value from the context, or null if not found
     */
    public Object getAndCleanInLoop(String nodeKey, String key, String loopNodeKey) {
        // If no active loop, fall back to regular getAndClean
        if (!activeLoops.contains(loopNodeKey)) {
            return getAndClean(nodeKey, key);
        }

        // Resolve the key similar to getAndClean
        String resolvedKey;
        if (TemplateEngine.isTemplate(key)) {
            // Extract the full expression, e.g., "user.name:Guest"
            String fullExpr = TemplateEngine.extractFullRefs(key).stream().findFirst().orElse(null);
            if (fullExpr == null) return null; // Malformed template

            // Check for function calls like uuid()
            if (fullExpr.matches("^[a-zA-Z0-9_.-]+\\([^()]*\\)$")) {
                return TemplateEngine.evaluateFunction(fullExpr);
            }

            // Extract the actual reference, e.g., "user.name"
            String refKey = TemplateEngine.extractRefs(key).stream().findFirst().orElse(null);
            if (refKey == null) return null; // Malformed template

            resolvedKey = resolveAlias(refKey);

        } else {
            resolvedKey = resolveAlias(key);
        }

        Object value = context.get(resolvedKey);

        // If value is null, we must check for a default value in the template
        if (value == null && TemplateEngine.isTemplate(key)) {
            Object defaultValue = TemplateEngine.extractDefaultValue(key);
            if (defaultValue != null) {
                log.debug("Using default value '{}' for template '{}' in loop node '{}' consumer '{}'",
                        defaultValue, key, loopNodeKey, nodeKey);
                return defaultValue;
            }
        }

        // Defer cleanup for the resolved key
        if (value != null) {
            Map<String, Set<String>> loopPendingCleanup = pendingLoopCleanup.get(loopNodeKey);
            if (loopPendingCleanup != null) {
                loopPendingCleanup.computeIfAbsent(resolvedKey, k -> new HashSet<>()).add(nodeKey);
                log.debug("Deferred cleanup for key '{}' consumer '{}' in loop '{}'", resolvedKey, nodeKey, loopNodeKey);
            }
        }

        return value;
    }

    /**
     * Check if there are any active loops currently running
     *
     * @return true if there are active loops
     */
    public boolean isInLoop() {
        return !activeLoops.isEmpty();
    }

    /**
     * Get the active loop for a node key (if any)
     *
     * @return The loop node key if in a loop, null otherwise
     */
    public String getActiveLoop() {
        return activeLoops.stream().findFirst().orElse(null);
    }

    public void endLoopIfActive() {
        String activeLoop = this.getActiveLoop();
        if (activeLoop != null) {
            this.endLoop(activeLoop);
            log.debug("Ended active loop '{}' due to error or validation failure", activeLoop);
        }
    }
}
