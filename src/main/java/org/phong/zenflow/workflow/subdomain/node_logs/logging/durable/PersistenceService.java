package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;// logging/collector/PersistenceService.java
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;

import java.util.List;
import java.util.UUID;

public interface PersistenceService {
    void saveBatch(UUID runId, List<LogEntry> entries) throws Exception;
}
