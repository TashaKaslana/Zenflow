package org.phong.zenflow.project.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.project.infrastructure.persistence.entity.Project}
 */
public record ProjectDto(@NotNull UUID id, @NotNull OffsetDateTime createdAt, @NotNull OffsetDateTime updatedAt,
                         UUID userId, @NotNull String name, String description, UUID updatedBy,
                         OffsetDateTime deletedAt) implements Serializable {
}