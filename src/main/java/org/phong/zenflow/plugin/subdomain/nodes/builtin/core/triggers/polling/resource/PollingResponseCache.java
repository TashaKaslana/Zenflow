package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.resource;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for storing last polling responses for change detection.
 * Managed by {@link PollingResponseCacheManager} using the BaseNodeResourceManager pattern.
 */
public class PollingResponseCache {
    private final ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    public int size() {
        return cache.size();
    }
}
