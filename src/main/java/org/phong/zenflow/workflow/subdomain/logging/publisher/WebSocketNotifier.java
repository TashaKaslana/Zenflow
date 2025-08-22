package org.phong.zenflow.workflow.subdomain.logging.publisher;

import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

public interface WebSocketNotifier {
    void onLog(LogEntry entry);
}
