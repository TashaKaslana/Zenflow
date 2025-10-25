package org.phong.zenflow.workflow.subdomain.context;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.context.refvalue.RefValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RuntimeContext manages shared context and consumer relationships for workflow execution.
 * 
 * <p><b>RefValue Integration</b>: Internally uses {@link RefValue} for efficient storage
 * of large payloads (JSON, files, streams) without exhausting heap memory. Legacy APIs
 * transparently convert between Object and RefValue for backward compatibility.
 * 
 * <p>Storage is automatically optimized:
 * <ul>
 *   <li>Small values (< 1MB) → Memory</li>
 *   <li>Medium JSON (1-2MB) → Parsed tree with JsonPointer queries</li>
 *   <li>Large payloads (> 1MB) → Temp files on disk</li>
 * </ul>
 * 
 * @see RefValue
 * @see RuntimeContextRefValueSupport
 */
@Slf4j
public class RuntimeContext {
    
    // Core storage: values are now RefValue instances for efficient memory management
    private final Map<String, RefValue> context = new ConcurrentHashMap<>();
    
    // Consumer tracking and aliases remain unchanged
    private final Map<String, AtomicInteger> consumers = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    // Loop-aware cleanup management
    private final Map<String, Map<String, Set<String>>> pendingLoopCleanup = new ConcurrentHashMap<>();
    private final Set<String> activeLoops = new HashSet<>();
    
    private final RuntimeContextRefValueSupport refValueSupport;
    
    /**
     * Constructor with optional RefValue support injection.
     * 
     * @param refValueSupport support layer for RefValue operations (null = create default)
     */
    public RuntimeContext(RuntimeContextRefValueSupport refValueSupport) {
        this.refValueSupport = refValueSupport != null ? refValueSupport : new RuntimeContextRefValueSupport();
    }
    
    /**
     * Default constructor.
     */
    public RuntimeContext() {
        this(null);
    }

    public void initialize(Map<String, Object> initialContext,
                           Map<String, Set<String>> initialConsumers,
                           Map<String, String> initialAliases) {
        if (initialConsumers != null) {
            initialConsumers.forEach((key, value) -> {
                if (value != null) {
                    consumers.put(key, new AtomicInteger(value.size()));
                }
            });
        }
        if (initialContext != null) {
            for (Map.Entry<String, Object> entry : initialContext.entrySet()) {
                context.put(entry.getKey(), refValueSupport.objectToRefValue(entry.getKey(), entry.getValue()));
            }
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
        RefValue refValue = refValueSupport.objectToRefValue(key, value);
        context.put(key, refValue);
    }

    public void putAll(Map<String, Object> entries) {
        if (entries != null) {
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Retrieves a value from the context by key.
     * RefValue is automatically materialized to Object for backward compatibility.
     *
     * @param key context key
     * @return the value associated with the key, or null if not present
     */
    public Object get(String key) {
        String resolvedKey = aliases.getOrDefault(key, key);
        RefValue refValue = context.get(resolvedKey);
        return refValueSupport.refValueToObject(resolvedKey, refValue);
    }
    
    /**
     * Retrieves the RefValue directly without materialization.
     * Use this for streaming access to large payloads without loading into memory.
     * 
     * @param key context key
     * @return the RefValue associated with the key, or null if not present
     */
    public RefValue getRef(String key) {
        String resolvedKey = aliases.getOrDefault(key, key);
        return context.get(resolvedKey);
    }
    
    /**
     * Get the raw internal context map (for legacy compatibility).
     * 
     * @return materialized map of context values
     * @deprecated Use get() or getRef() for individual values
     */
    @Deprecated
    public Map<String, Object> getContext() {
        Map<String, Object> materialized = new HashMap<>();
        for (Map.Entry<String, RefValue> entry : context.entrySet()) {
            materialized.put(entry.getKey(), refValueSupport.refValueToObject(entry.getKey(), entry.getValue()));
        }
        return materialized;
    }

    public boolean containsKey(String key) {
        return context.containsKey(key);
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
            if (isConsumersEmpty(currentOutputKey)) {
                log.debug("Skipping storage of '{}' as it has no registered consumers", currentOutputKey);
                continue;
            }

            // Store the value as it has consumers
            RefValue refValue = refValueSupport.objectToRefValue(currentOutputKey, value);
            context.put(currentOutputKey, refValue);
            log.debug("Stored context-guided output '{}' with value type '{}' for remaining consumers: {}",
                    currentOutputKey, value != null ? value.getClass().getSimpleName() : "null", getConsumerCount(currentOutputKey));
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
        String resolvedKey = resolveAlias(key);
        if (isInLoop()) {
            return getAndMarkConsumedInLoop(nodeKey, resolvedKey);
        }
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

        RefValue refValue = context.get(key);
        Object value = refValueSupport.refValueToObject(key, refValue);

        // Remove the current node from the consumers list for this key
        removeConsumer(key, nodeKey);

        // Perform garbage collection - remove the key if no more consumers
        performGarbageCollection(key);

        return value;
    }

    /**
     * Gets a value from the context during a loop. Consumer cleanup is deferred
     * until the loop ends to allow multiple iterations to access the same value.
     *
     * @param nodeKey The consuming node
     * @param key     The key to access
     * @return The value, or null if not found
     */
    private Object getAndMarkConsumedInLoop(String nodeKey, String key) {
        if (!context.containsKey(key)) {
            return null;
        }

        RefValue refValue = context.get(key);
        Object value = refValueSupport.refValueToObject(key, refValue);
        
        String activeLoop = getActiveLoop();
        if (activeLoop != null) {
            pendingLoopCleanup
                    .computeIfAbsent(activeLoop, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(key, k -> new HashSet<>())
                    .add(nodeKey);
        }
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
     * Remove a specific consumer from a key's consumer list
     */
    private void removeConsumer(String key, String nodeKey) {
        AtomicInteger remainingConsumers = consumers.get(key);
        if (remainingConsumers == null) {
            return;
        }

        int updated = remainingConsumers.decrementAndGet();
        log.debug("Removed consumer '{}' from key '{}', remaining consumers: {}", nodeKey, key, Math.max(updated, 0));

        if (updated <= 0) {
            consumers.remove(key);
            log.debug("No more consumers for key '{}', removed from consumers map", key);
        }
    }

    /**
     * Perform garbage collection by removing context entries that have no consumers
     */
    private void performGarbageCollection(String key) {
        AtomicInteger remainingConsumers = consumers.get(key);

        if (remainingConsumers == null || remainingConsumers.get() <= 0) {
            RefValue removedValue = context.remove(key);
            if (removedValue != null) {
                log.debug("Garbage collected key '{}' from context", key);
                refValueSupport.releaseRefValue(key, removedValue);
            }
            if (remainingConsumers != null) {
                consumers.remove(key, remainingConsumers);
            } else {
                consumers.remove(key);
            }
        }
    }

    /**
     * Check if a key has any remaining consumers
     */
    public boolean isConsumersEmpty(String key) {
        AtomicInteger keyConsumers = consumers.get(key);
        return keyConsumers == null || keyConsumers.get() <= 0;
    }

    /**
     * Get the remaining consumer count for a specific key
     */
    public int getConsumerCount(String key) {
        AtomicInteger keyConsumers = consumers.get(key);
        return keyConsumers != null ? Math.max(keyConsumers.get(), 0) : 0;
    }

    /**
     * Manual garbage collection for all keys without consumers
     */
    public void garbageCollect() {
        List<String> keysToRemove = new ArrayList<>();

        for (String key : context.keySet()) {
            if (isConsumersEmpty(key)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            context.remove(key);
            consumers.remove(key);
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
        pendingLoopCleanup.values().forEach(loopMap -> loopMap.remove(key));
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
                Set<String> nodesConsumed = entry.getValue();
                int consumersToRemove = nodesConsumed != null ? nodesConsumed.size() : 0;

                if (consumersToRemove == 0) {
                    continue;
                }

                AtomicInteger keyConsumers = consumers.get(key);
                if (keyConsumers != null) {
                    int updated = keyConsumers.updateAndGet(current -> Math.max(current - consumersToRemove, 0));
                    if (updated == 0) {
                        consumers.remove(key, keyConsumers);
                    }
                }

                boolean hadValue = context.containsKey(key);
                performGarbageCollection(key);
                if (hadValue && !context.containsKey(key)) {
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                log.debug("Loop '{}' cleanup removed {} context entries", loopNodeKey, cleanedCount);
            }
        }

        log.debug("Ended loop context for node: {}", loopNodeKey);
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
