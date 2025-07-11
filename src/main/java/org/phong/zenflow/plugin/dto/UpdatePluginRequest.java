package org.phong.zenflow.plugin.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin}
 */
public record UpdatePluginRequest(UUID publisherId, String name, String version, String registryUrl,
                                  Boolean verified) implements Serializable {
}