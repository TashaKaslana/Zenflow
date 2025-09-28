package org.phong.zenflow.workflow.subdomain.context;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RuntimeContextManager {
    private final Cache<@NonNull String, RuntimeContext> cache = Caffeine.newBuilder().build();

    /**
     * Retrieves the {@link RuntimeContext} associated with the given key from the cache.
     * If no context exists for the key, a new {@link RuntimeContext} is created, stored, and returned.
     *
     * @param key the cache key
     * @return the existing or newly created {@link RuntimeContext}
     */
    public RuntimeContext getOrCreate(String key) {
        return cache.get(key, k -> new RuntimeContext());
    }

    public void invalidate(String key) {
        cache.invalidate(key);
    }

    public void invalidateAll() {
        cache.invalidateAll();
    }

    public RuntimeContext assign(String key, RuntimeContext context) {
        cache.put(key, context);
        return context;
    }

    public RuntimeContext remove(String key) {
        return cache.asMap().remove(key);
    }


    /**
     * Retrieves a value from the {@link RuntimeContext} associated with the given key.
     * Returns the value stored in the context, or null if not present.
     */
    public Object get(String key) {
        RuntimeContext context = cache.getIfPresent(key);
        return context != null ? context.get(key) : null;
    }
}
