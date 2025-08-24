package org.phong.zenflow.workflow.subdomain.logging.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;
import org.phong.zenflow.workflow.subdomain.logging.router.LogRouter;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener that catches LogEntry events published by NodeLogPublisher
 * and routes them to the LogRouter for processing through the logging infrastructure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LogEntryEventListener {

    /**
     * Listens for LogEntry events and routes them through the LogRouter.
     * This is asynchronous using the global applicationTaskExecutor to avoid blocking the publisher.
     */
    @EventListener
    @Async
    public void handleLogEntry(LogEntry logEntry) {
        try {
            log.trace("Received LogEntry event for workflow run: {}, node: {}, level: {}",
                     logEntry.getWorkflowRunId(), logEntry.getNodeKey(), logEntry.getLevel());

            // Route the log entry through the LogRouter infrastructure
            LogRouter.dispatch(logEntry);
        } catch (Exception e) {
            log.error("Failed to process LogEntry event: {}", e.getMessage(), e);
            // Don't rethrow - we don't want to break the publisher
        }
    }
}
