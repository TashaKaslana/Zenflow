package org.phong.zenflow.plugin.subdomain.node.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * DTO for {@link org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode}
 */
public record UpdatePluginNodeRequest(String name, String type, String pluginNodeVersion,
                                      Map<String, Object> configSchema, String executorType,
                                      String entrypoint, String description, List<String> tags, String icon,
                                      String key) implements Serializable {
}