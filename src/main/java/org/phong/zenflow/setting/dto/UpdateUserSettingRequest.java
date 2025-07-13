package org.phong.zenflow.setting.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for updating {@link org.phong.zenflow.setting.infrastructure.persistence.entity.UserSetting}
 */
public record UpdateUserSettingRequest(Map<String, Object> settings) implements Serializable {
}
