package org.phong.zenflow.workflow.subdomain.context.resolution;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

record Tracked<T>(T resource,
                  AtomicInteger refs,
                  AtomicLong emaCostNanos,
                  AtomicLong lastUsedNanos) {
    static final double ALPHA = 0.2;

    void recordCost(long nanos) {
        long prev = emaCostNanos.get();
        long next = (prev == 0L) ? nanos : (long) (ALPHA * nanos + (1 - ALPHA) * prev);
        emaCostNanos.set(next);
        lastUsedNanos.set(System.nanoTime());
    }
}