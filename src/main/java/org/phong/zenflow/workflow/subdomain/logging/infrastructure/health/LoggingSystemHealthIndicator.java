package org.phong.zenflow.workflow.subdomain.logging.infrastructure.health;

import org.phong.zenflow.workflow.subdomain.logging.infrastructure.buffer.WorkflowBufferManager;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.collector.GlobalLogCollector;
import org.phong.zenflow.workflow.subdomain.logging.config.LoggingProperties;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.metrics.LoggingMetrics;
import org.phong.zenflow.workflow.subdomain.logging.util.SharedThreadPoolManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("loggingSystemHealth")
public class LoggingSystemHealthIndicator implements HealthIndicator {

    private final GlobalLogCollector globalLogCollector;
    private final WorkflowBufferManager bufferManager;
    private final SharedThreadPoolManager threadPoolManager;
    private final LoggingMetrics metrics;
    private final LoggingProperties properties;

    public LoggingSystemHealthIndicator(GlobalLogCollector globalLogCollector,
                                       WorkflowBufferManager bufferManager,
                                       SharedThreadPoolManager threadPoolManager,
                                       LoggingMetrics metrics,
                                       LoggingProperties properties) {
        this.globalLogCollector = globalLogCollector;
        this.bufferManager = bufferManager;
        this.threadPoolManager = threadPoolManager;
        this.metrics = metrics;
        this.properties = properties;
    }

    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();

        try {
            // Check queue health
            int queueSize = globalLogCollector.getQueueSize();
            double queueUtilization = (double) queueSize / properties.getHealthConfig().getQueueCapacity();

            if (queueUtilization > 0.9) {
                builder.down().withDetail("reason", "Queue utilization critical: " +
                    String.format("%.1f%%", queueUtilization * 100));
            } else if (queueUtilization > 0.7) {
                builder.status("WARNING").withDetail("reason", "Queue utilization high: " +
                    String.format("%.1f%%", queueUtilization * 100));
            } else {
                builder.up();
            }

            // Add detailed status information
            Map<String, Object> bufferMetrics = bufferManager.getMetrics();
            LoggingMetrics.MetricsSnapshot snapshot = metrics.getSnapshot();

            builder.withDetail("queue", Map.of(
                "size", queueSize,
                "utilization", String.format("%.1f%%", queueUtilization * 100)
            ))
            .withDetail("circuitBreaker", Map.of(
                "state", globalLogCollector.getCircuitBreakerState(),
                "trips", snapshot.circuitBreakerTrips()
            ))
            .withDetail("buffers", Map.of(
                "active", bufferMetrics.get("activeBuffers"),
                "totalQueueSize", bufferMetrics.get("totalQueueSize"),
                "processedEntries", bufferMetrics.get("totalProcessedEntries")
            ))
            .withDetail("threadPool", Map.of(
                "activeWorkflows", threadPoolManager.getActiveWorkflowCount()
            ))
            .withDetail("persistence", Map.of(
                "successes", snapshot.persistenceSuccesses(),
                "failures", snapshot.persistenceFailures(),
                "avgLatency", String.format("%.2fms", snapshot.avgPersistenceLatency())
            ))
            .withDetail("performance", Map.of(
                "avgBufferFlushTime", String.format("%.2fms", snapshot.avgBufferFlushTime()),
                "avgBatchProcessingTime", String.format("%.2fms", snapshot.avgBatchProcessingTime()),
                "bufferOverflows", snapshot.bufferOverflows()
            ));

        } catch (Exception e) {
            builder.down(e).withDetail("error", "Failed to collect health metrics");
        }

        return builder.build();
    }
}
