package org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher;

import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaImpl implements KafkaPublisher {
    @Override
    public void publish(List<LogEntry> entries) {

    }
}
