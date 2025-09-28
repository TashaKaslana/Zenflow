package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.phong.zenflow.secret.enums.SecretScope;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for {@link Secret}
 */
@Data
@AllArgsConstructor
public final class SecretDto implements Serializable {
    private final @NotNull UUID id;
    private final @NotNull OffsetDateTime createdAt;
    private final @NotNull OffsetDateTime updatedAt;
    private final UUID userId;
    private final UUID projectId;
    private final UUID workflowId;
    private final @NotNull String key;
    private @NotNull String value;
    private final String description;
    private final List<String> tags;
    private final Integer version;
    private final Boolean isActive;
    private final @NotNull SecretScope scope;
    private final UUID updatedBy;
    private final OffsetDateTime deletedAt;
}
