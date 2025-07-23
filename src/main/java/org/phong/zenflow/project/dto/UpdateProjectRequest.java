package org.phong.zenflow.project.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * DTO for {@link org.phong.zenflow.project.infrastructure.persistence.entity.Project}
 */
public record UpdateProjectRequest(@NotNull String name, String description,
                                   OffsetDateTime deletedAt) implements Serializable {
}