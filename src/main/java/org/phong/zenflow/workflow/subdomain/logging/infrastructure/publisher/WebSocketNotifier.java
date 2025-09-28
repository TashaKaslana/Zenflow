package org.phong.zenflow.workflow.subdomain.logging.infrastructure.publisher;

import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

public interface WebSocketNotifier {
    void onLog(LogEntry entry);
}
