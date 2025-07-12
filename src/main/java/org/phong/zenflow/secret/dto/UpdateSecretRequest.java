package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;
import org.phong.zenflow.secret.enums.SecretScope;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for updating {@link org.phong.zenflow.secret.infrastructure.persistence.entity.Secret}
 */
public record UpdateSecretRequest(@NotNull String groupName, @NotNull String key, @NotNull String value,
                                  String description, List<String> tags, @NotNull SecretScope scope,
                                  UUID projectId, UUID workflowId, @NotNull Boolean isActive) implements Serializable {
}
