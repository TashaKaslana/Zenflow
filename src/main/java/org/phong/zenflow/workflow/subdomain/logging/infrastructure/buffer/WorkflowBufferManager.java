package org.phong.zenflow.workflow.subdomain.logging.infrastructure.buffer;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.collector.GlobalLogCollector;
import org.phong.zenflow.workflow.subdomain.logging.config.LoggingProperties;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;
import org.phong.zenflow.workflow.subdomain.logging.util.SharedThreadPoolManager;

import java.util.*;
import java.util.concurrent.*;

public class WorkflowBufferManager {
    private final ConcurrentMap<UUID, WorkflowBuffer> buffers = new ConcurrentHashMap<>();
    private final GlobalLogCollector collector;
    private final LoggingProperties.BufferConfig config;
    private final SharedThreadPoolManager threadPoolManager;
    private final ScheduledFuture<?> cleanupTask;

    public WorkflowBufferManager(GlobalLogCollector collector, LoggingProperties.BufferConfig config,
                                SharedThreadPoolManager threadPoolManager) {
        this.collector = collector;
        this.config = config;
        this.threadPoolManager = threadPoolManager;

        // Schedule periodic cleanup of idle buffers
        this.cleanupTask = threadPoolManager.getSharedScheduler().scheduleAtFixedRate(
            this::cleanupIdleBuffers,
            config.getCleanupIdleAfterMs(),
            config.getCleanupIdleAfterMs() / 2, // Check twice as frequently as timeout
            TimeUnit.MILLISECONDS
        );
    }

    public void startRun(UUID runId) {
        buffers.computeIfAbsent(runId, id ->
            new WorkflowBuffer(id, collector, config, threadPoolManager)
        );
    }

    public void endRun(UUID runId) {
        WorkflowBuffer buf = buffers.remove(runId);
        if (buf != null) buf.closeAndFlush();
    }

    public void enqueue(LogEntry e) {
        // Ensure buffer exists (idempotent)
        startRun(e.getWorkflowRunId());
        buffers.get(e.getWorkflowRunId()).append(e);
    }

    public List<LogEntry> recent(UUID runId, int limit) {
        WorkflowBuffer buf = buffers.get(runId);
        return buf == null ? List.of() : buf.recent(limit);
    }

    // Handle system memory pressure by flushing all buffers
    public void handleMemoryPressure() {
        buffers.values().parallelStream().forEach(WorkflowBuffer::handleMemoryPressure);
    }

    // Get metrics for monitoring
    public Map<String, Object> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeBuffers", buffers.size());
        metrics.put("totalQueueSize", buffers.values().stream()
            .mapToInt(WorkflowBuffer::getCurrentQueueSize)
            .sum());
        metrics.put("totalProcessedEntries", buffers.values().stream()
            .mapToLong(WorkflowBuffer::getTotalEntriesProcessed)
            .sum());
        return metrics;
    }

    private void cleanupIdleBuffers() {
        buffers.entrySet().removeIf(entry -> {
            WorkflowBuffer buffer = entry.getValue();
            if (buffer.isIdleForCleanup()) {
                buffer.closeAndFlush();
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel(false);
        }

        // Close all remaining buffers
        buffers.values().forEach(WorkflowBuffer::closeAndFlush);
        buffers.clear();
    }
}
