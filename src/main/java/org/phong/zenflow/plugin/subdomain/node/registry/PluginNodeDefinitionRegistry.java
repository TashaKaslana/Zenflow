package org.phong.zenflow.plugin.subdomain.node.registry;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of plugin node metadata and configuration schemas discovered
 * from external JARs.
 */
@Component
public class PluginNodeDefinitionRegistry {

    private final Map<PluginNodeIdentifier, Map<String, Object>> schemas = new ConcurrentHashMap<>();

    public void upsert(PluginNodeIdentifier identifier, Map<String, Object> schema) {
        schemas.put(identifier, schema);
    }

    public Optional<Map<String, Object>> getSchema(PluginNodeIdentifier identifier) {
        return Optional.ofNullable(schemas.get(identifier));
    }

    public Map<PluginNodeIdentifier, Map<String, Object>> getAll() {
        return Collections.unmodifiableMap(schemas);
    }
}
