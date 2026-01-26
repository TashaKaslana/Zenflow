package org.phong.zenflow.workflow.subdomain.node_definition.compound;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Metadata that a compound node descriptor supplies for each child it materializes.
 * The materializer adds the child under a predictable key and then hands off
 * execution/validation to the existing node pipeline, so this object only carries
 * the minimal information needed to build the child definition.
 */
public record CompoundChildDescriptor(String childAlias, PluginNodeIdentifier pluginNodeIdentifier,
                                      Map<String, Object> configOverrides, List<String> profileKeys) {
    public CompoundChildDescriptor(String childAlias,
                                   PluginNodeIdentifier pluginNodeIdentifier,
                                   Map<String, Object> configOverrides,
                                   List<String> profileKeys) {
        this.childAlias = childAlias;
        this.pluginNodeIdentifier = pluginNodeIdentifier;
        this.configOverrides = configOverrides != null ? Map.copyOf(configOverrides) : Collections.emptyMap();
        this.profileKeys = profileKeys == null ? List.of() : List.copyOf(profileKeys);
    }
}
