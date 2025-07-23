package org.phong.zenflow.workflow.subdomain.context;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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

    public void initialize(Map<String, Object> initialContext,
                           Map<String, List<String>> initialConsumers) {
        if (initialConsumers != null) {
            consumers.putAll(initialConsumers);
        }
        if (initialContext != null) {
            context.putAll(initialContext);
        }
    }

    public void put(String key, Object value) {
        context.put(key, value);
    }

    public Object get(String key) {
        return context.get(key);
    }

    public Object getAndClean(String nodeKey, String key) {
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
