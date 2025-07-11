package org.phong.zenflow.plugin.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin}
 */
public record PluginDto(@NotNull UUID id, @NotNull OffsetDateTime createdAt, @NotNull OffsetDateTime updatedAt,
                        UUID publisherId, @NotNull String name, @NotNull String version, String registryUrl,
                        @NotNull Boolean verified) implements Serializable {
}