package org.phong.zenflow.plugin.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin}
 */
public record UpdatePluginRequest(UUID publisherId, String name, String version, String registryUrl,
                                  Boolean verified, String description, List<String> tags, String icon, String key,
                                  String organization, Map<String, Object> pluginSchema) implements Serializable {
}