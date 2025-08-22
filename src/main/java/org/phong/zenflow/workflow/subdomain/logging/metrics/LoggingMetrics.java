package org.phong.zenflow.workflow.subdomain.logging.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Timer.Sample;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class LoggingMetrics {

    private final MeterRegistry meterRegistry;

    // Counters
    private final Counter logsProcessed;
    private final Counter persistenceFailures;
    private final Counter persistenceSuccesses;
    private final Counter bufferOverflows;
    private final Counter circuitBreakerTrips;

    // Gauges
    private final AtomicLong activeWorkflows = new AtomicLong(0);
    private final AtomicLong totalQueueDepth = new AtomicLong(0);
    private final AtomicLong memoryUsage = new AtomicLong(0);

    // Timers
    private final Timer persistenceLatency;
    private final Timer bufferFlushTime;
    private final Timer batchProcessingTime;

    // Distribution summaries for tracking batch sizes and queue depths
    private final DistributionSummary batchSizes;
    private final DistributionSummary queueDepths;

    public LoggingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Initialize counters
        this.logsProcessed = Counter.builder("zenflow.logging.logs.processed")
            .description("Total number of log entries processed")
            .register(meterRegistry);

        this.persistenceFailures = Counter.builder("zenflow.logging.persistence.failures")
            .description("Number of persistence failures")
            .register(meterRegistry);

        this.persistenceSuccesses = Counter.builder("zenflow.logging.persistence.successes")
            .description("Number of successful persistence operations")
            .register(meterRegistry);

        this.bufferOverflows = Counter.builder("zenflow.logging.buffer.overflows")
            .description("Number of buffer overflow events")
            .register(meterRegistry);

        this.circuitBreakerTrips = Counter.builder("zenflow.logging.circuit.breaker.trips")
            .description("Number of circuit breaker trips")
            .register(meterRegistry);

        // Initialize gauges with proper API
        Gauge.builder("zenflow.logging.workflows.active", this, LoggingMetrics::getActiveWorkflows)
            .description("Number of active workflows with logging buffers")
            .register(meterRegistry);

        Gauge.builder("zenflow.logging.queue.depth.total", this, LoggingMetrics::getTotalQueueDepth)
            .description("Total queue depth across all workflow buffers")
            .register(meterRegistry);

        Gauge.builder("zenflow.logging.memory.usage.bytes", this, LoggingMetrics::getMemoryUsage)
            .description("Estimated memory usage of logging system in bytes")
            .register(meterRegistry);

        // Initialize timers
        this.persistenceLatency = Timer.builder("zenflow.logging.persistence.latency")
            .description("Latency of persistence operations")
            .register(meterRegistry);

        this.bufferFlushTime = Timer.builder("zenflow.logging.buffer.flush.time")
            .description("Time taken to flush buffer batches")
            .register(meterRegistry);

        this.batchProcessingTime = Timer.builder("zenflow.logging.batch.processing.time")
            .description("Time taken to process batches")
            .register(meterRegistry);

        // Initialize distribution summaries
        this.batchSizes = DistributionSummary.builder("zenflow.logging.batch.size")
            .description("Distribution of batch sizes")
            .register(meterRegistry);

        this.queueDepths = DistributionSummary.builder("zenflow.logging.queue.depth")
            .description("Distribution of queue depths per workflow")
            .register(meterRegistry);
    }

    // Counter methods
    public void incrementLogsProcessed(long count) {
        logsProcessed.increment(count);
    }

    public void incrementPersistenceFailures() {
        persistenceFailures.increment();
    }

    public void incrementPersistenceSuccesses() {
        persistenceSuccesses.increment();
    }

    public void incrementBufferOverflows() {
        bufferOverflows.increment();
    }

    public void incrementCircuitBreakerTrips() {
        circuitBreakerTrips.increment();
    }

    // Gauge update methods
    public void updateActiveWorkflows(long count) {
        activeWorkflows.set(count);
    }

    public void updateTotalQueueDepth(long depth) {
        totalQueueDepth.set(depth);
    }

    public void updateMemoryUsage(long bytes) {
        memoryUsage.set(bytes);
    }

    // Timer methods
    public Sample startPersistenceTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPersistenceLatency(Sample sample) {
        sample.stop(persistenceLatency);
    }

    public Sample startBufferFlushTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordBufferFlushTime(Sample sample) {
        sample.stop(bufferFlushTime);
    }

    public Sample startBatchProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordBatchProcessingTime(Sample sample) {
        sample.stop(batchProcessingTime);
    }

    // Distribution methods
    public void recordBatchSize(int size) {
        batchSizes.record(size);
    }

    public void recordQueueDepth(int depth) {
        queueDepths.record(depth);
    }

    // Getter methods for gauges
    private double getActiveWorkflows() {
        return activeWorkflows.doubleValue();
    }

    private double getTotalQueueDepth() {
        return totalQueueDepth.doubleValue();
    }

    private double getMemoryUsage() {
        return memoryUsage.doubleValue();
    }

    // Utility method to get current metrics snapshot
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            logsProcessed.count(),
            persistenceFailures.count(),
            persistenceSuccesses.count(),
            bufferOverflows.count(),
            circuitBreakerTrips.count(),
            activeWorkflows.get(),
            totalQueueDepth.get(),
            memoryUsage.get(),
            persistenceLatency.mean(TimeUnit.MILLISECONDS),
            bufferFlushTime.mean(TimeUnit.MILLISECONDS),
            batchProcessingTime.mean(TimeUnit.MILLISECONDS)
        );
    }

    public record MetricsSnapshot(
        double logsProcessed,
        double persistenceFailures,
        double persistenceSuccesses,
        double bufferOverflows,
        double circuitBreakerTrips,
        long activeWorkflows,
        long totalQueueDepth,
        long memoryUsage,
        double avgPersistenceLatency,
        double avgBufferFlushTime,
        double avgBatchProcessingTime
    ) {}
}
