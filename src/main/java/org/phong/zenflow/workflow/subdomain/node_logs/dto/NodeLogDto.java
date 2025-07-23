package org.phong.zenflow.workflow.subdomain.node_logs.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog}
 */
public record NodeLogDto(@NotNull UUID id, UUID workflowRunId, @NotNull String nodeKey, @NotNull String status,
                         String error, Integer attempts, Map<String, Object> output,
                         @NotNull OffsetDateTime startedAt, OffsetDateTime endedAt,
                         List<LogEntry> logs) implements Serializable {
}