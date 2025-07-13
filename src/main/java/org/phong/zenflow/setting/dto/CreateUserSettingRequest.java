package org.phong.zenflow.setting.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for creating {@link org.phong.zenflow.setting.infrastructure.persistence.entity.UserSetting}
 */
public record CreateUserSettingRequest(@NotNull UUID userId, Map<String, Object> settings) implements Serializable {
}
