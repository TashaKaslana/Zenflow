package org.phong.zenflow.workflow.subdomain.logging.publisher;

import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;
import org.springframework.stereotype.Component;

@Component
public class WebsocketNotifierImpl implements WebSocketNotifier {

    @Override
    public void onLog(LogEntry entry) {
        // Implementation for handling log entries via WebSocket
        // This could involve sending the log entry to connected WebSocket clients
        // For example, using a WebSocket session manager to broadcast the entry
    }
}
