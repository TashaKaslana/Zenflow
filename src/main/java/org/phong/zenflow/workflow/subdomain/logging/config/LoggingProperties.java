package org.phong.zenflow.workflow.subdomain.logging.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Setter
@Getter
@ConfigurationProperties(prefix = "zenflow.logging.durable")
public class LoggingProperties {

    // Getters and setters
    @NestedConfigurationProperty
    private RouterConfig router = new RouterConfig();

    @NestedConfigurationProperty
    private BufferConfig buffer = new BufferConfig();

    @NestedConfigurationProperty
    private PersistenceConfig persistence = new PersistenceConfig();

    @NestedConfigurationProperty
    private ThreadPoolConfig threadPool = new ThreadPoolConfig();

    @NestedConfigurationProperty
    private HealthConfig healthConfig = new HealthConfig();

    @Setter
    @Getter
    public static class RouterConfig {
        private int queueCapacity = 100000;
        private int workers = 2;
        private BackpressureConfig backpressure = new BackpressureConfig();

        @Setter
        @Getter
        public static class BackpressureConfig {
            private String strategy = "DROP_DEBUG_FIRST";
            private double threshold = 0.8;
        }
    }

    @Setter
    @Getter
    public static class BufferConfig {
        private int defaultBatchSize = 100;
        private long maxDelayMs = 2000;
        private int ringBufferSize = 200;
        private long cleanupIdleAfterMs = 300000; // 5 minutes
        private boolean adaptiveBatching = true;
        private int minBatchSize = 10;
        private int maxBatchSize = 500;
    }

    @Setter
    @Getter
    public static class PersistenceConfig {
        private long batchTimeoutMs = 5000;
        private int retryAttempts = 3;
        private long retryBackoffMs = 1000;
        private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

        @Setter
        @Getter
        public static class CircuitBreakerConfig {
            private int failureThreshold = 10;
            private long recoveryTimeMs = 30000;
        }
    }

    @Setter
    @Getter
    public static class ThreadPoolConfig {
        private int corePoolSize = 2;
        private int maximumPoolSize = 10;
        private long keepAliveTimeMs = 60000;
        private int queueCapacity = 1000;
    }

    @Data
    public static class HealthConfig {
        private int queueCapacity = 10000;
        private boolean enabled = true;
        private long intervalMs = 60000;
        private int maxUnhealthyRuns = 3; // Fail after 3 consecutive unhealthy checks
    }
}
