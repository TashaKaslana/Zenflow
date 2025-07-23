package org.phong.zenflow.plugin.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin}
 */
public record CreatePluginRequest(UUID publisherId, @NotNull String name, @NotNull String version, String registryUrl,
                                  @NotNull Boolean verified, String description, List<String> tags) implements Serializable {
}