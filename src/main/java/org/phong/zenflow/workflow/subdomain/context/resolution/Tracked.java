package org.phong.zenflow.workflow.subdomain.context.resolution;

import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wrapper that tracks cost and live reference count for cached resources.
 * References are queried live from RuntimeContext to ensure accurate consumer tracking.
 */
record Tracked<T>(
        T resource,
        RuntimeContext runtimeContext,  // Live reference to RuntimeContext
        String normalizedKey,           // Key to query consumer count
        String originalKey,             // Alternative key to query consumer count
        AtomicLong emaCostNanos,        // exponential moving average of compute time
        AtomicLong lastUsedNanos        // diagnostics/observability
) {
    static final double ALPHA = 0.2;

    /**
     * Get live reference count from RuntimeContext.
     * Returns the maximum consumer count between normalized and original keys.
     */
    AtomicInteger refs() {
        if (runtimeContext == null) {
            return new AtomicInteger(0);
        }
        int count = Math.max(
            normalizedKey != null ? runtimeContext.getConsumerCount(normalizedKey) : 0,
            originalKey != null ? runtimeContext.getConsumerCount(originalKey) : 0
        );
        return new AtomicInteger(count);
    }
    
    void recordCost(long nanos) {
        long prev = emaCostNanos.get();
        long next = (prev == 0L) ? nanos : (long) (ALPHA * nanos + (1 - ALPHA) * prev);
        emaCostNanos.set(next);
        lastUsedNanos.set(System.nanoTime());
    }
}
