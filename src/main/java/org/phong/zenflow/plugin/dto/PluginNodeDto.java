package org.phong.zenflow.plugin.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.infrastructure.persistence.entity.PluginNode}
 */
public record PluginNodeDto(@NotNull UUID id, @NotNull OffsetDateTime createdAt, @NotNull OffsetDateTime updatedAt,
                            UUID pluginId, @NotNull String name, @NotNull String type, String pluginNodeVersion,
                            Map<String, Object> configSchema, @NotNull String executorType,
                            String entrypoint) implements Serializable {
}