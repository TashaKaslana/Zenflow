package org.phong.zenflow.setting.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.setting.infrastructure.persistence.entity.UserSetting}
 */
public record UserSettingDto(@NotNull UUID id, @NotNull OffsetDateTime createdAt, @NotNull OffsetDateTime updatedAt,
                            @NotNull UUID userId, Map<String, Object> settings) implements Serializable {
}
