package org.phong.zenflow.workflow.subdomain.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SystemStateManager {
    private final Map<String, Object> systemState = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> systemConsumers = new ConcurrentHashMap<>();

    /**
     * Store system state without consumer tracking (for persistent state like loops)
     */
    public void putSystemState(String key, Object value) {
        systemState.put(key, value);
        log.debug("Stored system state '{}' with value type '{}'", key, 
                 value != null ? value.getClass().getSimpleName() : "null");
    }

    /**
     * Get system state without affecting its lifecycle
     */
    public Object getSystemState(String key) {
        return systemState.get(key);
    }

    /**
     * Remove system state (used when loops/conditions complete)
     */
    public void removeSystemState(String key) {
        Object removed = systemState.remove(key);
        systemConsumers.remove(key);
        if (removed != null) {
            log.debug("Removed system state '{}'", key);
        }
    }

    /**
     * Store system state with consumer tracking (for cleanup when specific nodes complete)
     */
    public void putSystemStateWithConsumer(String key, Object value, String consumerNodeId) {
        systemState.put(key, value);
        systemConsumers.computeIfAbsent(key, k -> new HashSet<>()).add(consumerNodeId);
        log.debug("Stored system state '{}' with consumer '{}'", key, consumerNodeId);
    }

    /**
     * Get system state and mark as consumed, triggering cleanup if no more consumers
     */
    public Object getSystemStateAndMarkConsumed(String key, String consumerNodeId) {
        Object value = systemState.get(key);
        if (value != null) {
            Set<String> keyConsumers = systemConsumers.get(key);
            if (keyConsumers != null) {
                keyConsumers.remove(consumerNodeId);
                if (keyConsumers.isEmpty()) {
                    systemState.remove(key);
                    systemConsumers.remove(key);
                    log.debug("System state '{}' consumed by last consumer '{}', removed", key, consumerNodeId);
                }
            }
        }
        return value;
    }

    /**
     * Check if system state exists for a key
     */
    public boolean hasSystemState(String key) {
        return systemState.containsKey(key);
    }

    /**
     * Get all system state keys (for debugging/monitoring)
     */
    public Set<String> getSystemStateKeys() {
        return new HashSet<>(systemState.keySet());
    }

    /**
     * Clear all system state (useful for cleanup when workflow completes/fails)
     */
    public void clear() {
        systemState.clear();
        systemConsumers.clear();
        log.debug("SystemStateManager cleared");
    }

    /**
     * Get current system state size for monitoring
     */
    public int getSystemStateSize() {
        return systemState.size();
    }
}
