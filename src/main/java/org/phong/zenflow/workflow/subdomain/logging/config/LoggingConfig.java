package org.phong.zenflow.workflow.subdomain.logging.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.metrics.LoggingMetrics;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher.KafkaImpl;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher.KafkaPublisher;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher.WebSocketNotifier;
import org.phong.zenflow.workflow.subdomain.logging.router.LogRouter;
import org.phong.zenflow.workflow.subdomain.logging.util.CircuitBreaker;
import org.phong.zenflow.workflow.subdomain.logging.util.SharedThreadPoolManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.phong.zenflow.workflow.subdomain.logging.infrastructure.persistence.JdbcPersistenceService;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.persistence.PersistenceService;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.collector.GlobalLogCollector;
import org.phong.zenflow.workflow.subdomain.logging.infrastructure.buffer.WorkflowBufferManager;
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
        return new KafkaImpl();
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
}
