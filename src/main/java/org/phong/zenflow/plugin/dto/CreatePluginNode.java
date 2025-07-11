package org.phong.zenflow.plugin.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.infrastructure.persistence.entity.PluginNode}
 */
public record CreatePluginNode(UUID pluginId, @NotNull String name, @NotNull String type, String pluginNodeVersion,
                               Map<String, Object> configSchema, @NotNull String executorType,
                               String entrypoint) implements Serializable {
}