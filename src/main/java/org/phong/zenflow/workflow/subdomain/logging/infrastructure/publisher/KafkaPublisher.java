package org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher;// logging/collector/KafkaPublisher.java
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

import java.util.List;

public interface KafkaPublisher {
    // Implement with your Kafka client; partition key = runId to preserve order per run
    void publish(List<LogEntry> entries);
}
