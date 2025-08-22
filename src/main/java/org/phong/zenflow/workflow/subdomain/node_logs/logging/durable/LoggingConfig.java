package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(LoggingProperties.class)
public class LoggingConfig {

    @Bean
    public SharedThreadPoolManager sharedThreadPoolManager(LoggingProperties properties) {
        return new SharedThreadPoolManager(properties);
    }

    @Bean
    public LoggingMetrics loggingMetrics(MeterRegistry meterRegistry) {
        return new LoggingMetrics(meterRegistry);
    }

    @Bean
    public CircuitBreaker circuitBreaker(LoggingProperties properties, LoggingMetrics metrics) {
        return new CircuitBreaker(properties.getPersistence().getCircuitBreaker(), metrics);
    }

    @Bean
    public PersistenceService persistenceService(DataSource dataSource) {
        return new JdbcPersistenceService(dataSource);
    }

    @Bean
    public KafkaPublisher kafkaPublisher() {
        // TODO: Implement Kafka publisher when ready
        return null;
    }

    @Bean
    public GlobalLogCollector globalLogCollector(
            PersistenceService persistenceService,
            KafkaPublisher kafkaPublisher,
            LoggingProperties properties,
            CircuitBreaker circuitBreaker,
            LoggingMetrics metrics) {

        return new GlobalLogCollector(
                persistenceService,
                kafkaPublisher,
                properties.getPersistence(),
                circuitBreaker,
                metrics,
                properties.getRouter().getWorkers()
        );
    }

    @Bean
    public WorkflowBufferManager workflowBufferManager(
            GlobalLogCollector collector,
            LoggingProperties properties,
            SharedThreadPoolManager threadPoolManager) {

        return new WorkflowBufferManager(
                collector,
                properties.getBuffer(),
                threadPoolManager
        );
    }

    @Bean
    public LogRouter logRouter(
            WebSocketNotifier webSocketNotifier,
            WorkflowBufferManager bufferManager,
            LoggingProperties properties) {

        // Initialize LogRouter using the static method since getInstance() doesn't exist
        LogRouter.init(
                webSocketNotifier,
                bufferManager,
                null, // TODO: Implement health check callback
                properties.getRouter().getWorkers()
        );

        // Return a placeholder or null since LogRouter.init is static
        // This bean definition ensures proper initialization order
        return null;
    }

    // Legacy static method kept for backward compatibility
    @Deprecated
    public static void wire(DataSource ds, WebSocketNotifier ws) {
        // This method is deprecated - use Spring configuration instead
        throw new UnsupportedOperationException(
                "Legacy configuration method deprecated. Use Spring configuration instead."
        );
    }
}
