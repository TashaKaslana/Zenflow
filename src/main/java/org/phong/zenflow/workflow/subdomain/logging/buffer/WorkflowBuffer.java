package org.phong.zenflow.workflow.subdomain.logging.buffer;
import org.phong.zenflow.workflow.subdomain.logging.core.LogLevel;
import org.phong.zenflow.workflow.subdomain.logging.collector.GlobalLogCollector;
import org.phong.zenflow.workflow.subdomain.logging.config.LoggingProperties;
import org.phong.zenflow.workflow.subdomain.logging.util.SharedThreadPoolManager;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class WorkflowBuffer {
    private final UUID runId;
    private final GlobalLogCollector collector;
    private final LoggingProperties.BufferConfig config;
    private final SharedThreadPoolManager threadPoolManager;

    // FIFO for batch, lock-free for producers
    private final ConcurrentLinkedQueue<LogEntry> queue = new ConcurrentLinkedQueue<>();
    // Small ring buffer for reconnect/recent view
    private final ArrayDeque<LogEntry> ring;
    private final int ringCap;

    // Shared scheduler instead of per-workflow scheduler
    private final ScheduledFuture<?> ticker;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    // Adaptive batching metrics
    private final AtomicLong lastFlushTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong totalEntriesProcessed = new AtomicLong(0);
    private final AtomicInteger currentBatchSize;
    private volatile long lastActivityTime = System.currentTimeMillis();

    WorkflowBuffer(UUID runId, GlobalLogCollector collector, LoggingProperties.BufferConfig config,
                   SharedThreadPoolManager threadPoolManager) {
        this.runId = runId;
        this.collector = collector;
        this.config = config;
        this.threadPoolManager = threadPoolManager;
        this.ringCap = Math.max(1, config.getRingBufferSize());
        this.ring = new ArrayDeque<>(ringCap);
        this.currentBatchSize = new AtomicInteger(config.getDefaultBatchSize());

        // Use shared scheduler instead of creating per-workflow scheduler
        this.ticker = threadPoolManager.getSharedScheduler().scheduleAtFixedRate(
            this::flushIfAny,
            config.getMaxDelayMs(),
            config.getMaxDelayMs(),
            TimeUnit.MILLISECONDS
        );

        threadPoolManager.incrementActiveWorkflows();
    }

    void append(LogEntry e) {
        if (closed.get()) return;

        queue.offer(e);
        addToRing(e);
        lastActivityTime = System.currentTimeMillis();

        // Adaptive batching logic
        int queueSize = queue.size();
        boolean shouldFlushImmediate = false;

        // Priority-aware batching - ERROR logs get immediate processing
        if (e.getLevel() == LogLevel.ERROR) {
            shouldFlushImmediate = true;
        }
        // Dynamic batch size based on queue depth and system load
        else if (config.isAdaptiveBatching()) {
            updateAdaptiveBatchSize(queueSize);
            shouldFlushImmediate = queueSize >= currentBatchSize.get();
        } else {
            shouldFlushImmediate = queueSize >= config.getDefaultBatchSize();
        }

        if (shouldFlushImmediate) {
            // Use batch processor service to avoid blocking the caller
            threadPoolManager.getBatchProcessorService().execute(this::flushIfAny);
        }
    }

    synchronized void flushIfAny() {
        if (queue.isEmpty()) return;

        long flushStart = System.currentTimeMillis();
        List<LogEntry> batch = new ArrayList<>(Math.min(queue.size(), getCurrentEffectiveBatchSize()));
        LogEntry x;

        while ((x = queue.poll()) != null) {
            batch.add(x);
            if (batch.size() >= getCurrentEffectiveBatchSize() && !queue.isEmpty()) {
                collector.accept(runId, batch);
                totalEntriesProcessed.addAndGet(batch.size());
                batch = new ArrayList<>(Math.min(queue.size(), getCurrentEffectiveBatchSize()));
            }
        }

        if (!batch.isEmpty()) {
            collector.accept(runId, batch);
            totalEntriesProcessed.addAndGet(batch.size());
        }

        lastFlushTime.set(flushStart);
    }

    List<LogEntry> recent(int limit) {
        synchronized (ring) {
            return ring.stream().skip(Math.max(0, ring.size() - limit)).toList();
        }
    }

    void closeAndFlush() {
        if (closed.compareAndSet(false, true)) {
            try {
                ticker.cancel(false);
            } catch (Exception ignore) {}

            flushIfAny();
            threadPoolManager.decrementActiveWorkflows();
        }
    }

    // Check if buffer has been idle for cleanup
    boolean isIdleForCleanup() {
        return System.currentTimeMillis() - lastActivityTime > config.getCleanupIdleAfterMs();
    }

    // Memory pressure callback
    void handleMemoryPressure() {
        // Force flush under memory pressure
        if (!queue.isEmpty()) {
            threadPoolManager.getBatchProcessorService().execute(this::flushIfAny);
        }
    }

    private void addToRing(LogEntry e) {
        synchronized (ring) {
            if (ring.size() == ringCap) ring.removeFirst();
            ring.addLast(e);
        }
    }

    private void updateAdaptiveBatchSize(int queueSize) {
        // Adaptive batching based on queue depth and system load
        int systemLoad = threadPoolManager.getActiveWorkflowCount();

        if (queueSize > config.getDefaultBatchSize() * 2 || systemLoad > 10) {
            // High load - increase batch size to improve throughput
            currentBatchSize.set(Math.min(config.getMaxBatchSize(), currentBatchSize.get() + 10));
        } else if (queueSize < config.getDefaultBatchSize() / 2 && systemLoad < 5) {
            // Low load - decrease batch size for better latency
            currentBatchSize.set(Math.max(config.getMinBatchSize(), currentBatchSize.get() - 5));
        }
    }

    private int getCurrentEffectiveBatchSize() {
        return config.isAdaptiveBatching() ? currentBatchSize.get() : config.getDefaultBatchSize();
    }

    // Metrics for monitoring
    public long getTotalEntriesProcessed() {
        return totalEntriesProcessed.get();
    }

    public int getCurrentQueueSize() {
        return queue.size();
    }

    public long getLastFlushTime() {
        return lastFlushTime.get();
    }
}
