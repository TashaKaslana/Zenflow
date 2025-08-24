package org.phong.zenflow.plugin.subdomain.execution.interfaces;

import org.phong.zenflow.workflow.subdomain.logging.core.LogContext;

/**
 * Marker interface for executors that run externally and need log context injection.
 */
public interface ExternalPluginExecutor {
    void setLogContext(LogContext context);
}

