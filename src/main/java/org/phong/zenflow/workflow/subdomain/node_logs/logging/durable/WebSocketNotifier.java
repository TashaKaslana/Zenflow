package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;

import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;

public interface WebSocketNotifier {
    void onLog(LogEntry entry);
}
