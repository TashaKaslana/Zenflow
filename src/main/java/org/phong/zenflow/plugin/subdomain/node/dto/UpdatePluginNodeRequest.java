package org.phong.zenflow.plugin.subdomain.node.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode}
 */
public record UpdatePluginNodeRequest(@NotNull String name, @NotNull String type, String pluginNodeVersion,
                                      Map<String, Object> configSchema, @NotNull String executorType,
                                      String entrypoint) implements Serializable {
}