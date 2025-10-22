package org.phong.zenflow.workflow.subdomain.context.resolution;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks resolution cost statistics for profiling and admission decisions.
 * Maintains total cost and count to calculate average resolution time.
 */
class CostStats {
    private final AtomicLong totalCostNanos = new AtomicLong(0L);
    private final AtomicInteger count = new AtomicInteger(0);

    void record(long nanos) {
        totalCostNanos.addAndGet(nanos);
        count.incrementAndGet();
    }

    long getAverageCostNanos() {
        int n = count.get();
        return n == 0 ? 0L : totalCostNanos.get() / n;
    }
}
