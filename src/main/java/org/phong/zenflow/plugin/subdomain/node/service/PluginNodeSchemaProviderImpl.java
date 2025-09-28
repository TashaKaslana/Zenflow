package org.phong.zenflow.plugin.subdomain.node.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.phong.zenflow.plugin.subdomain.node.exception.ValidateNodeSchemaException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections.PluginNodeSchema;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeSpecifications;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class PluginNodeSchemaProviderImpl implements PluginNodeSchemaProvider {

    private final PluginNodeRepository pluginNodeRepository;
    private final SchemaIndexRegistry schemaIndexRegistry;

    // Cache for file-based schemas to avoid repeated file I/O
    private final Map<String, Map<String, Object>> fileSchemaCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getSchemaJson(String nodeId) {
        Specification<PluginNode> spec = PluginNodeSpecifications.withIds(List.of(nodeId));
        return pluginNodeRepository
                .findAll(spec)
                .stream()
                .findFirst()
                .map(PluginNode::getConfigSchema)
                .orElseThrow(() -> new ValidateNodeSchemaException(
                        "Node schema not found for node ID: " + nodeId
                ));
    }

    @Override
    public Map<String, Map<String, Object>> getAllSchemasByIdentifiers(Set<String> nodeIds) {
        Set<UUID> uuidSet = nodeIds.stream()
                .map(UUID::fromString)
                .collect(Collectors.toSet());

        return pluginNodeRepository.findAllSchemasByNodeIds(uuidSet).stream()
                .collect(Collectors.toMap(
                        schema -> schema.getId().toString(),
                        PluginNodeSchema::getConfigSchema,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public Map<String, Object> getSchemaJsonFromFile(String nodeId) {
        // Check cache first
        Map<String, Object> cached = fileSchemaCache.get(nodeId);
        if (cached != null) {
            log.debug("Schema file cache hit for node ID {}", nodeId);
            return cached;
        }

        log.debug("Schema file cache miss for node ID {}, loading from file", nodeId);
        Map<String, Object> schema = loadSchemaFromFile(nodeId);
        fileSchemaCache.put(nodeId, schema);
        return schema;
    }

    @Override
    public Map<String, Map<String, Object>> getAllSchemasByIdentifiersFromFile(Set<String> nodeIds) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (String nodeId : nodeIds) {
            try {
                Map<String, Object> schema = getSchemaJsonFromFile(nodeId);
                result.put(nodeId, schema);
            } catch (Exception e) {
                log.warn("Failed to load schema from file for node ID {}: {}", nodeId, e.getMessage());
                // Continue loading other schemas even if one fails
            }
        }

        return result;
    }

    private Map<String, Object> loadSchemaFromFile(String nodeId) {
        SchemaIndexRegistry.SchemaLocation location = schemaIndexRegistry.getSchemaLocation(nodeId);
        if (location == null) {
            throw new ValidateNodeSchemaException("Schema location not indexed for node ID: " + nodeId);
        }

        return LoadSchemaHelper.loadSchema(location.clazz(), location.schemaPath(), "schema.json");
    }

    /**
     * Invalidate the file schema cache for a specific node ID
     */
    public void invalidateFileSchemaCache(String nodeId) {
        fileSchemaCache.remove(nodeId);
        log.debug("Invalidated file schema cache for node ID {}", nodeId);
    }

    /**
     * Clear all file schema cache
     */
    public void clearFileSchemaCache() {
        fileSchemaCache.clear();
        log.debug("Cleared all file schema cache");
    }

    /**
     * Get schema index statistics for monitoring
     */
    public int getIndexedSchemaCount() {
        return schemaIndexRegistry.getSchemaIndexSize();
    }

    /**
     * Check if a schema is available via file-based loading
     */
    public boolean hasFileBasedSchema(String nodeId) {
        return schemaIndexRegistry.hasSchemaLocation(nodeId);
    }
}
