package org.phong.zenflow.plugin.subdomain.resource;

import lombok.Getter;

import java.util.UUID;

/**
 * Wrapper around a pooled resource that automatically unregisters usage
 * when closed.
 *
 * @param <T> type of the underlying resource
 */
public class ScopedNodeResource<T> implements AutoCloseable {
    private final NodeResourcePool<T, ?> pool;
    private final String resourceKey;
    private final UUID nodeId;
    @Getter
    private final T resource;

    public ScopedNodeResource(NodeResourcePool<T, ?> pool, String resourceKey, UUID nodeId, T resource) {
        this.pool = pool;
        this.resourceKey = resourceKey;
        this.nodeId = nodeId;
        this.resource = resource;
    }

    @Override
    public void close() {
        pool.unregisterNodeUsage(resourceKey);
    }
}
