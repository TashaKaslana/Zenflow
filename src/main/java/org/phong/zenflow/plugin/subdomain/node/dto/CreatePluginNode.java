package org.phong.zenflow.plugin.subdomain.node.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode}
 */
public record CreatePluginNode(UUID pluginId, @NotNull String name, @NotNull String type, String pluginNodeVersion,
                               Map<String, Object> configSchema, @NotNull String executorType,
                               String entrypoint, String description, List<String> tags, String icon, String key) implements Serializable {
}