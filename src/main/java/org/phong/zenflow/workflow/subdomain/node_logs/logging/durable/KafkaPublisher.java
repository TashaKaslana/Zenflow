package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;// logging/collector/KafkaPublisher.java
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;

import java.util.List;

public interface KafkaPublisher {
    // Implement with your Kafka client; partition key = runId to preserve order per run
    void publish(List<LogEntry> entries);
}
