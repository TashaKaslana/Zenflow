package org.phong.zenflow.plugin.subdomain.execution.dto;

import java.util.UUID;

/**
 * Metadata about the current execution used for runtime operations.
 */
public record RuntimeMetadata(UUID workflowRunId) {
}
