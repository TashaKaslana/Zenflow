package org.phong.zenflow.workflow.subdomain.logging.infrastructure.collector;
import io.micrometer.core.instrument.Timer;
import org.phong.zenflow.workflow.subdomain.logging.core.LogLevel;
import org.phong.zenflow.workflow.subdomain.logging.config.LoggingProperties;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.metrics.LoggingMetrics;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.persistence.PersistenceService;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher.KafkaPublisher;
import org.phong.zenflow.workflow.subdomain.logging.util.CircuitBreaker;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GlobalLogCollector {
    private final BlockingQueue<Batch> queue;
    private final PersistenceService persistence;
    private final KafkaPublisher kafka;
    private final LoggingProperties.PersistenceConfig config;
    private final CircuitBreaker circuitBreaker;
    private final LoggingMetrics metrics;
    private final ExecutorService workerPool;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public GlobalLogCollector(PersistenceService persistence, KafkaPublisher kafka,
                            LoggingProperties.PersistenceConfig config, CircuitBreaker circuitBreaker,
                            LoggingMetrics metrics, int workers) {
        this.persistence = persistence;
        this.kafka = kafka;
        this.config = config;
        this.circuitBreaker = circuitBreaker;
        this.metrics = metrics;

        // Use configurable queue capacity with monitoring
        int capacity = 10_000; // Could make this configurable
        this.queue = new ArrayBlockingQueue<>(capacity);

        this.workerPool = Executors.newFixedThreadPool(workers, r -> {
            Thread t = new Thread(r, "log-collector");
            t.setDaemon(true);
            return t;
        });

        // Start worker threads
        for (int i = 0; i < workers; i++) {
            workerPool.submit(this::processBatches);
        }
    }

    public void accept(UUID runId, List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) return;

        Batch batch = new Batch(runId, entries);

        if (!queue.offer(batch)) {
            // Backpressure handling with metrics
            metrics.incrementBufferOverflows();
            handleBackpressure(batch);
        }

        // Update queue depth metrics
        metrics.recordQueueDepth(queue.size());
    }

    private void processBatches() {
        while (!shutdown.get() || !queue.isEmpty()) {
            try {
                Batch batch = queue.poll(1, TimeUnit.SECONDS);
                if (batch != null) {
                    processBatchWithRetry(batch);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processBatchWithRetry(Batch batch) {
        Timer.Sample sample = metrics.startBatchProcessingTimer();

        try {
            // Try to persist with circuit breaker protection
            circuitBreaker.executeVoid(() -> {
                Timer.Sample persistenceTimer = metrics.startPersistenceTimer();
                try {
                    persistence.saveBatch(batch.runId, batch.entries);
                    metrics.incrementPersistenceSuccesses();
                    metrics.incrementLogsProcessed(batch.entries.size());
                    metrics.recordBatchSize(batch.entries.size());
                } catch (Exception e) {
                    // Re-throw as RuntimeException so circuit breaker can handle it
                    throw new RuntimeException("Persistence failed", e);
                } finally {
                    metrics.recordPersistenceLatency(persistenceTimer);
                }
            });

            // Forward to Kafka if available (optional, don't fail the whole batch)
            if (kafka != null) {
                try {
                    kafka.publish(batch.entries);
                } catch (Exception kafkaEx) {
                    // Log Kafka failure but don't retry - persistence is more critical
                    System.err.println("Kafka publish failed: " + kafkaEx.getMessage());
                }
            }

        } catch (CircuitBreaker.CircuitBreakerException e) {
            // Circuit breaker is open, drop the batch or queue for later
            handleCircuitBreakerOpen(batch);
        } catch (Exception ex) {
            // Retry with backoff
            retryBatch(batch, ex, 1);
        } finally {
            metrics.recordBatchProcessingTime(sample);
        }
    }

    private void retryBatch(Batch batch, Exception lastException, int attempt) {
        if (attempt > config.getRetryAttempts()) {
            metrics.incrementPersistenceFailures();
            System.err.println("Failed to persist batch after " + config.getRetryAttempts() +
                             " attempts: " + lastException.getMessage());
            return;
        }

        try {
            // Exponential backoff
            long backoffMs = config.getRetryBackoffMs() * (1L << (attempt - 1));
            Thread.sleep(Math.min(backoffMs, 30000)); // Cap at 30 seconds

            // Retry with circuit breaker
            circuitBreaker.executeVoid(() -> {
                Timer.Sample timer = metrics.startPersistenceTimer();
                try {
                    persistence.saveBatch(batch.runId, batch.entries);
                    metrics.incrementPersistenceSuccesses();
                    metrics.incrementLogsProcessed(batch.entries.size());
                } catch (Exception e) {
                    // Re-throw as RuntimeException so circuit breaker can handle it
                    throw new RuntimeException("Persistence retry failed", e);
                } finally {
                    metrics.recordPersistenceLatency(timer);
                }
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (Exception ex) {
            retryBatch(batch, ex, attempt + 1);
        }
    }

    private void handleBackpressure(Batch batch) {
        // Priority-based backpressure: drop DEBUG batches first, preserve ERROR/WARN
        List<LogEntry> criticalEntries = batch.entries.stream()
            .filter(e -> e.getLevel() != LogLevel.DEBUG && e.getLevel() != LogLevel.INFO)
            .toList();

        if (!criticalEntries.isEmpty()) {
            // Try to queue only critical entries
            Batch criticalBatch = new Batch(batch.runId, criticalEntries);
            if (!queue.offer(criticalBatch)) {
                // Still can't queue - force synchronous processing for ERROR level
                List<LogEntry> errorEntries = criticalEntries.stream()
                    .filter(e -> e.getLevel() == LogLevel.ERROR)
                    .toList();
                if (!errorEntries.isEmpty()) {
                    processBatchWithRetry(new Batch(batch.runId, errorEntries));
                }
            }
        }
    }

    private void handleCircuitBreakerOpen(Batch batch) {
        // When circuit breaker is open, we could:
        // 1. Drop the batch (current approach)
        // 2. Queue to a dead letter queue
        // 3. Write to a local file for later processing

        // For now, just count the failure and drop non-critical logs
        metrics.incrementPersistenceFailures();

        // Keep only ERROR level logs and try to queue them for later
        List<LogEntry> errorEntries = batch.entries.stream()
            .filter(e -> e.getLevel() == LogLevel.ERROR)
            .toList();

        if (!errorEntries.isEmpty()) {
            queue.offer(new Batch(batch.runId, errorEntries));
        }
    }

    public void shutdown() {
        shutdown.set(true);
        workerPool.shutdown();

        try {
            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            workerPool.shutdownNow();
        }
    }

    // Metrics access
    public int getQueueSize() {
        return queue.size();
    }

    public CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    private record Batch(UUID runId, List<LogEntry> entries) {}
}
