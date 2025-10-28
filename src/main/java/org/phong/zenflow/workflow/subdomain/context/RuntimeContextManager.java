package org.phong.zenflow.workflow.subdomain.context;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RuntimeContextManager {
    private final Cache<@NonNull String, RuntimeContext> cache = Caffeine.newBuilder()
            .removalListener((key, value, cause) -> {
                // Clean up RefValue resources when cache entry is evicted
                if (value instanceof RuntimeContext context) {
                    log.debug("Cleaning up RuntimeContext for key: {} (cause: {})", key, cause);
                    context.clear();
                }
            })
            .build();

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
        RuntimeContext context = cache.asMap().get(key);
        if (context != null) {
            // Clean up all RefValue resources before invalidating
            context.clear();
        }
        cache.invalidate(key);
        // Force synchronous cleanup of removal listener
        cache.cleanUp();
    }

    public void invalidateAll() {
        // Clean up all contexts before invalidating
        cache.asMap().values().forEach(RuntimeContext::clear);
        cache.invalidateAll();
        // Force synchronous cleanup of removal listener
        cache.cleanUp();
    }

    public RuntimeContext assign(String key, RuntimeContext context) {
        cache.put(key, context);
        return context;
    }

    public RuntimeContext remove(String key) {
        RuntimeContext context = cache.asMap().get(key);
        if (context != null) {
            // Clean up all RefValue resources before removing from cache
            context.clear();
        }
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
