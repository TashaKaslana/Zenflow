package org.phong.zenflow.workflow.subdomain.context;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

@Component
public class RuntimeContextManager {
    private final Cache<String, RuntimeContext> cache = Caffeine.newBuilder().build();

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

    public Object get(String key) {
        RuntimeContext context = cache.getIfPresent(key);
        return context != null ? context.get(key) : null;
    }
}
