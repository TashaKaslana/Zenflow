package org.phong.zenflow.workflow.subdomain.node_logs.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO for {@link org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity.NodeLog}
 */
public record UpdateNodeLogRequest(@NotNull String nodeKey, @NotNull String status, String error,
                                   Integer attempts, Map<String, Object> output, OffsetDateTime endedAt,
                                   Map<String, Object> logs) implements Serializable {
}