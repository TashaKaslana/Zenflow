package org.phong.zenflow.project.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.project.infrastructure.persistence.entity.Project}
 */
public record CreateProjectRequest(UUID userId, @NotNull String name, String description) implements Serializable {
}