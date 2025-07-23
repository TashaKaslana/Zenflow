package org.phong.zenflow.secret.dto;

import org.phong.zenflow.secret.enums.SecretScope;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for updating {@link org.phong.zenflow.secret.infrastructure.persistence.entity.Secret}
 */
public record UpdateSecretRequest(String groupName, String key, String value,
                                  String description, List<String> tags, SecretScope scope,
                                  UUID projectId, UUID workflowId, Boolean isActive) implements Serializable {
}
