package org.phong.zenflow.plugin.subdomain.node.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.node.exception.ValidateNodeSchemaException;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeRepository;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository.PluginNodeSpecifications;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNodeSchemaIndex;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNodeSynchronizer;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class PluginNodeSchemaProviderImpl implements PluginNodeSchemaProvider {

    private final PluginNodeRepository pluginNodeRepository;
    private final PluginNodeSynchronizer synchronizer;
    private final PluginNodeSchemaIndex schemaIndex;

    // Cache for file-based schemas to avoid repeated file I/O
    private final Map<String, Map<String, Object>> fileSchemaCache = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> getSchemaJson(PluginNodeIdentifier identifier) {
        Specification<PluginNode> spec = PluginNodeSpecifications.withIdentifiers(List.of(identifier));
        return pluginNodeRepository
                .findAll(spec)
                .stream()
                .findFirst()
                .map(PluginNode::getConfigSchema)
                .orElseThrow(() -> new ValidateNodeSchemaException(
                        "Node schema not found for identifier: " + identifier
                ));
    }

    @Override
    public Map<String, Map<String, Object>> getAllSchemasByIdentifiers(List<PluginNodeIdentifier> identifiers) {
        Specification<PluginNode> spec = PluginNodeSpecifications.withIdentifiers(identifiers);
        return pluginNodeRepository.findAll(spec).stream()
                .filter(node -> node.getConfigSchema() != null)
                .collect(Collectors.toMap(
                        this::getCacheKey,
                        PluginNode::getConfigSchema
                ));
    }

    @Override
    public Map<String, Object> getSchemaJsonFromFile(PluginNodeIdentifier identifier) {
        String cacheKey = identifier.toCacheKey();

        // Check cache first
        Map<String, Object> cached = fileSchemaCache.get(cacheKey);
        if (cached != null) {
            log.debug("Schema file cache hit for {}", cacheKey);
            return cached;
        }

        log.debug("Schema file cache miss for {}, loading from file", cacheKey);
        Map<String, Object> schema = loadSchemaFromFile(identifier);
        fileSchemaCache.put(cacheKey, schema);
        return schema;
    }

    @Override
    public Map<String, Map<String, Object>> getAllSchemasByIdentifiersFromFile(List<PluginNodeIdentifier> identifiers) {
        Map<String, Map<String, Object>> result = new HashMap<>();

        for (PluginNodeIdentifier identifier : identifiers) {
            try {
                Map<String, Object> schema = getSchemaJsonFromFile(identifier);
                result.put(identifier.toCacheKey(), schema);
            } catch (Exception e) {
                log.warn("Failed to load schema from file for {}: {}", identifier, e.getMessage());
                // Continue loading other schemas even if one fails
            }
        }

        return result;
    }

    private Map<String, Object> loadSchemaFromFile(PluginNodeIdentifier identifier) {
        PluginNodeSchemaIndex.SchemaLocation location = schemaIndex.getSchemaLocation(identifier);
        if (location == null) {
            throw new ValidateNodeSchemaException("Schema location not indexed for identifier: " + identifier);
        }

        return synchronizer.loadSchema(location.clazz(), location.schemaPath());
    }

    private String getCacheKey(PluginNode entity) {
        return entity.getPlugin().getName() + ":" + entity.getKey() + ":" + entity.getPluginNodeVersion();
    }

    /**
     * Invalidate the file schema cache for a specific identifier
     */
    public void invalidateFileSchemaCache(PluginNodeIdentifier identifier) {
        fileSchemaCache.remove(identifier.toCacheKey());
        log.debug("Invalidated file schema cache for {}", identifier.toCacheKey());
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
        return schemaIndex.getIndexSize();
    }

    /**
     * Check if a schema is available via file-based loading
     */
    public boolean hasFileBasedSchema(PluginNodeIdentifier identifier) {
        return schemaIndex.hasSchemaLocation(identifier);
    }
}
