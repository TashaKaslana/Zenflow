package org.phong.zenflow.workflow.subdomain.logging.persistence;

import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

import java.util.List;
import java.util.UUID;

public interface PersistenceService {
    void saveBatch(UUID runId, List<LogEntry> entries) throws Exception;
}
