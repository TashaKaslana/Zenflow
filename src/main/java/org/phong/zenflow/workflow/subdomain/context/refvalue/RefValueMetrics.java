package org.phong.zenflow.workflow.subdomain.context.refvalue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics tracker for RefValue operations.
 * Tracks: ref counts by type, cleanup latency, storage size, leak warnings.
 * 
 * <p>Metrics are exposed via Micrometer for integration with Prometheus, Grafana, etc.
 */
@Slf4j
@Component
public class RefValueMetrics {
    
    private final MeterRegistry meterRegistry;
    private final RefValueConfig config;
    
    // Counters for creation by type
    private final Counter memoryCreated;
    private final Counter jsonCreated;
    private final Counter fileCreated;
    
    // Counters for release by type
    private final Counter memoryReleased;
    private final Counter jsonReleased;
    private final Counter fileReleased;
    
    // Timers for cleanup latency
    private final Timer fileCleanupTimer;
    
    // Gauges for current counts (using AtomicLong)
    private final AtomicLong memoryCount = new AtomicLong(0);
    private final AtomicLong jsonCount = new AtomicLong(0);
    private final AtomicLong fileCount = new AtomicLong(0);
    
    // Total storage size tracking
    private final AtomicLong memoryBytesTotal = new AtomicLong(0);
    private final AtomicLong jsonBytesTotal = new AtomicLong(0);
    private final AtomicLong fileBytesTotal = new AtomicLong(0);
    
    // Leak detection: track file creation times
    private final ConcurrentMap<String, Long> fileCreationTimes = new ConcurrentHashMap<>();
    
    public RefValueMetrics(MeterRegistry meterRegistry, RefValueConfig config) {
        this.meterRegistry = meterRegistry;
        this.config = config;
        
        // Register counters
        this.memoryCreated = Counter.builder("refvalue.memory.created")
                .description("Number of MemoryRefValue instances created")
                .register(meterRegistry);
        
        this.jsonCreated = Counter.builder("refvalue.json.created")
                .description("Number of JsonRefValue instances created")
                .register(meterRegistry);
        
        this.fileCreated = Counter.builder("refvalue.file.created")
                .description("Number of FileRefValue instances created")
                .register(meterRegistry);
        
        this.memoryReleased = Counter.builder("refvalue.memory.released")
                .description("Number of MemoryRefValue instances released")
                .register(meterRegistry);
        
        this.jsonReleased = Counter.builder("refvalue.json.released")
                .description("Number of JsonRefValue instances released")
                .register(meterRegistry);
        
        this.fileReleased = Counter.builder("refvalue.file.released")
                .description("Number of FileRefValue instances released")
                .register(meterRegistry);
        
        // Register timers
        this.fileCleanupTimer = Timer.builder("refvalue.file.cleanup.duration")
                .description("Time taken to delete file-backed values")
                .register(meterRegistry);
        
        // Register gauges for current counts
        meterRegistry.gauge("refvalue.memory.active", memoryCount);
        meterRegistry.gauge("refvalue.json.active", jsonCount);
        meterRegistry.gauge("refvalue.file.active", fileCount);
        
        // Register gauges for storage size
        meterRegistry.gauge("refvalue.memory.bytes", memoryBytesTotal);
        meterRegistry.gauge("refvalue.json.bytes", jsonBytesTotal);
        meterRegistry.gauge("refvalue.file.bytes", fileBytesTotal);
    }
    
    /**
     * Records creation of a RefValue.
     */
    public void recordCreated(RefValueType type, long sizeBytes, String identifier) {
        if (!config.isMetricsEnabled()) return;
        
        switch (type) {
            case MEMORY -> {
                memoryCreated.increment();
                memoryCount.incrementAndGet();
                memoryBytesTotal.addAndGet(sizeBytes);
            }
            case JSON -> {
                jsonCreated.increment();
                jsonCount.incrementAndGet();
                jsonBytesTotal.addAndGet(sizeBytes);
            }
            case FILE -> {
                fileCreated.increment();
                fileCount.incrementAndGet();
                fileBytesTotal.addAndGet(sizeBytes);
                if (identifier != null) {
                    fileCreationTimes.put(identifier, System.currentTimeMillis());
                }
            }
        }
        
        log.trace("RefValue created: type={}, size={} bytes", type, sizeBytes);
    }
    
    /**
     * Records release of a RefValue.
     */
    public void recordReleased(RefValueType type, long sizeBytes, String identifier) {
        if (!config.isMetricsEnabled()) return;
        
        switch (type) {
            case MEMORY -> {
                memoryReleased.increment();
                memoryCount.decrementAndGet();
                memoryBytesTotal.addAndGet(-sizeBytes);
            }
            case JSON -> {
                jsonReleased.increment();
                jsonCount.decrementAndGet();
                jsonBytesTotal.addAndGet(-sizeBytes);
            }
            case FILE -> {
                fileReleased.increment();
                fileCount.decrementAndGet();
                fileBytesTotal.addAndGet(-sizeBytes);
                if (identifier != null) {
                    checkForLeak(identifier);
                    fileCreationTimes.remove(identifier);
                }
            }
        }
        
        log.trace("RefValue released: type={}, size={} bytes", type, sizeBytes);
    }
    
    /**
     * Records file cleanup duration.
     */
    public void recordCleanupDuration(Runnable cleanupAction) {
        if (!config.isMetricsEnabled()) {
            cleanupAction.run();
            return;
        }
        fileCleanupTimer.record(cleanupAction);
    }
    
    /**
     * Checks if a file has been alive longer than the leak warning threshold.
     */
    private void checkForLeak(String identifier) {
        Long createdAt = fileCreationTimes.get(identifier);
        if (createdAt != null) {
            long ageMs = System.currentTimeMillis() - createdAt;
            if (ageMs > config.getFileLeakWarningMs()) {
                log.warn("Potential file leak detected: {} was alive for {} ms (threshold: {} ms)", 
                        identifier, ageMs, config.getFileLeakWarningMs());
            }
        }
    }
    
    /**
     * Gets current active count for a type.
     */
    public long getActiveCount(RefValueType type) {
        return switch (type) {
            case MEMORY -> memoryCount.get();
            case JSON -> jsonCount.get();
            case FILE -> fileCount.get();
        };
    }
    
    /**
     * Gets current total storage size for a type.
     */
    public long getTotalBytes(RefValueType type) {
        return switch (type) {
            case MEMORY -> memoryBytesTotal.get();
            case JSON -> jsonBytesTotal.get();
            case FILE -> fileBytesTotal.get();
        };
    }
    
    /**
     * Checks for potential leaks (files alive longer than threshold).
     * Should be called periodically by a scheduled task.
     */
    public void checkForLeaks() {
        long now = System.currentTimeMillis();
        long threshold = config.getFileLeakWarningMs();
        
        fileCreationTimes.forEach((identifier, createdAt) -> {
            long ageMs = now - createdAt;
            if (ageMs > threshold) {
                log.warn("Long-lived file detected: {} has been alive for {} ms", identifier, ageMs);
            }
        });
    }
}
