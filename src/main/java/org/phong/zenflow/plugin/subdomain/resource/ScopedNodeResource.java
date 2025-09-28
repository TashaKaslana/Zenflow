package org.phong.zenflow.plugin.subdomain.resource;

import lombok.Getter;

/**
 * Wrapper around a pooled resource that automatically unregisters usage
 * when closed.
 *
 * @param <T> type of the underlying resource
 */
public class ScopedNodeResource<T> implements AutoCloseable {
    private final NodeResourcePool<T, ?> pool;
    private final String resourceKey;
    @Getter
    private final T resource;

    public ScopedNodeResource(NodeResourcePool<T, ?> pool, String resourceKey, T resource) {
        this.pool = pool;
        this.resourceKey = resourceKey;
        this.resource = resource;
    }

    @Override
    public void close() {
        pool.unregisterNodeUsage(resourceKey);
    }
}
