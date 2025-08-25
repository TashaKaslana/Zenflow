package org.phong.zenflow.plugin.subdomain.schema.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaException;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaMissingException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SchemaRegistry {

    private static final String BUILTIN_PATH = "/builtin_schemas/";

    private final PluginNodeSchemaProvider pluginProvider;

    private final Cache<String, JSONObject> builtinCache;
    private final Cache<String, JSONObject> pluginCache;

    // Performance optimization: use file-based loading by default
    private final boolean useFileBasedLoading;

    public SchemaRegistry(
            PluginNodeSchemaProvider pluginProvider,
            @Value("${zenflow.schema.cache-ttl-seconds:3600}") long cacheTtlSeconds,
            @Value("${zenflow.schema.use-file-based-loading:true}") boolean useFileBasedLoading) {
        this.pluginProvider = pluginProvider;
        this.useFileBasedLoading = useFileBasedLoading;
        Duration ttl = Duration.ofSeconds(cacheTtlSeconds);
        this.builtinCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
        this.pluginCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
    }

    /**
     * Retrieves a schema by template string, supporting both built-in and plugin schemas.
     * <p>
     * Template string formats:
     * <ul>
     *   <li>Built-in: <code>builtin:&#60;name&#62;</code> (e.g., <code>builtin:http-trigger</code>)</li>
     *   <li>Plugin: <code>&#60;nodeId&#62;</code> (UUID string)</li>
     * </ul>
     * This unified naming convention allows easy differentiation and retrieval of schemas.
     *
     * @param templateString the schema identifier, either built-in name or plugin node UUID
     * @return JSONObject containing the schema
     */
    public JSONObject getSchemaByTemplateString(String templateString) {
        // Check if the schema is a built-in one
        if (templateString.startsWith("builtin:")) {
            return getBuiltinSchema(templateString.substring(8));
        }

        // Otherwise, treat it as a plugin schema with UUID
        return getPluginSchema(templateString);
    }

    public Map<String, JSONObject> getSchemaMapByTemplateStrings(Set<String> templateStrings) {
        Map<String, JSONObject> result = new HashMap<>();

        List<String> builtinNames = templateStrings.stream()
                .filter(name -> name.startsWith("builtin:"))
                .map(name -> name.substring(8))
                .toList();

        Set<String> pluginNodeIds = templateStrings.stream()
                .filter(name -> !name.startsWith("builtin:"))
                .collect(Collectors.toSet());

        if (!builtinNames.isEmpty()) {
            Map<String, JSONObject> builtinSchemas = getBuiltinSchemas(builtinNames);
            for (String name : builtinNames) {
                result.put("builtin:" + name, builtinSchemas.get(name));
            }
        }

        if (!pluginNodeIds.isEmpty()) {
            Map<String, JSONObject> pluginSchemas = getPluginSchemasByIds(pluginNodeIds);
            result.putAll(pluginSchemas);
        }

        return result;
    }

    // Built-in node schema: key = "http-trigger"
    public JSONObject getBuiltinSchema(String name) {
        JSONObject cached = builtinCache.getIfPresent(name);
        if (cached != null) {
            log.debug("Builtin schema cache hit for {}", name);
            return cached;
        }
        log.debug("Builtin schema cache miss for {}", name);
        JSONObject schema = loadBuiltinSchemaFromFile(name);
        builtinCache.put(name, schema);
        return schema;
    }

    private Map<String, JSONObject> getBuiltinSchemas(List<String> names) {
        return names.stream()
                .collect(Collectors.toMap(
                        name -> name,
                        this::getBuiltinSchema
                ));
    }

    /**
     * Get plugin node schema using PluginNodeIdentifier
     * Uses file-based loading for better performance when enabled
     * @param id The plugin node id
     * @return JSONObject containing the schema
     */
    private JSONObject getPluginSchema(String id) {
        JSONObject cached = pluginCache.getIfPresent(id);
        if (cached != null) {
            log.debug("Plugin schema cache hit for {}", id);
            return cached;
        }
        log.debug("Plugin schema cache miss for {}", id);

        Map<String, Object> schema;
        if (useFileBasedLoading) {
            // Use direct file access for better performance
            schema = pluginProvider.getSchemaJsonFromFile(id);
            log.debug("Loaded schema from file for {}", id);
        } else {
            // Fallback to database access
            schema = pluginProvider.getSchemaJson(id);
            log.debug("Loaded schema from database for {}", id);
        }

        if (schema.isEmpty()) {
            throw new NodeSchemaMissingException("No schema found for plugin node: " + id, List.of(id));
        }
        JSONObject json = new JSONObject(schema);
        pluginCache.put(id, json);
        return json;
    }

    /**
     * Efficiently retrieve multiple plugin schemas by identifiers in a single batch operation.
     * Uses file-based loading for better performance when enabled.
     * Fails if any identifier has no corresponding schema.
     *
     * @param identifiers Set of plugin node identifiers
     * @return Map of identifier (as String) -> schema JSONObject
     */
    private Map<String, JSONObject> getPluginSchemasByIds(Set<String> identifiers) {

        // Filter out already cached schemas
        Set<String> uncachedIdentifiers = identifiers.stream()
                .filter(id -> pluginCache.getIfPresent(id) == null)
                .collect(Collectors.toSet());

        // Fetch and cache missing schemas
        putNewSchemasForUncachedIdentifiers(uncachedIdentifiers);

        // Fail if any requested identifier is still missing
        List<String> missingIds = identifiers.stream()
                .filter(key -> pluginCache.getIfPresent(key) == null)
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NodeSchemaMissingException("Schemas missing for plugin node identifiers", missingIds);
        }

        return identifiers.stream()
                .collect(
                        HashMap::new,
                        (m, id) -> {
                            String key = id;
                            JSONObject val = pluginCache.getIfPresent(key);
                            if (val != null) {
                                m.put(key, val);
                            }
                        },
                        Map::putAll
                );
    }

    private void putNewSchemasForUncachedIdentifiers(Set<String> uncachedIdentifiers) {
        if (!uncachedIdentifiers.isEmpty()) {
            Map<String, Map<String, Object>> schemas;

            if (useFileBasedLoading) {
                // Use direct file access for better performance
                schemas = pluginProvider.getAllSchemasByIdentifiersFromFile(uncachedIdentifiers);
                log.debug("Loaded {} schemas from file", schemas.size());
            } else {
                // Fallback to database access
                schemas = pluginProvider.getAllSchemasByIdentifiers(uncachedIdentifiers);
                log.debug("Loaded {} schemas from database", schemas.size());
            }

            schemas.forEach((key, schemaMap) -> {
                if (schemaMap == null) {
                    throw new NodeSchemaException("Null schema for plugin node identifier: " + key);
                }
                pluginCache.put(key, new JSONObject(schemaMap));
            });
        }
    }

    /**
     * Convenience method to get schemas by PluginNodeIdentifier and return with PluginNodeIdentifier keys
     * @deprecated Use getPluginSchemasByIds instead - this method is for backward compatibility only
     * @param identifiers Set of plugin node identifiers
     * @return Map of PluginNodeIdentifier -> schema JSONObject
     */
    @Deprecated
    public Map<PluginNodeIdentifier, JSONObject> getPluginSchemasByIdentifiersAsIdentifier(Set<PluginNodeIdentifier> identifiers) {
        Set<String> nodeIds = identifiers.stream()
                .map(identifier -> identifier.getNodeId() != null ? identifier.getNodeId().toString() : identifier.toCacheKey())
                .collect(Collectors.toSet());

        Map<String, JSONObject> stringKeyMap = getPluginSchemasByIds(nodeIds);
        return identifiers.stream()
                .collect(Collectors.toMap(
                        identifier -> identifier,
                        identifier -> {
                            String key = identifier.getNodeId() != null ? identifier.getNodeId().toString() : identifier.toCacheKey();
                            return stringKeyMap.get(key);
                        }
                ));
    }

    /**
     * Force database-based schema loading for specific use cases (e.g., frontend API)
     * @param nodeId The plugin node UUID
     * @return JSONObject containing the schema from database
     */
    public JSONObject getPluginSchemaFromDatabase(String nodeId) {
        Map<String, Object> schema = pluginProvider.getSchemaJson(nodeId);
        if (schema.isEmpty()) {
            throw new NodeSchemaMissingException("No schema found in database for plugin node: " + nodeId, List.of(nodeId));
        }
        return new JSONObject(schema);
    }

    /**
     * Force file-based schema loading for specific use cases
     * @param nodeId The plugin node UUID
     * @return JSONObject containing the schema from file
     */
    public JSONObject getPluginSchemaFromFile(String nodeId) {
        Map<String, Object> schema = pluginProvider.getSchemaJsonFromFile(nodeId);
        if (schema.isEmpty()) {
            throw new NodeSchemaMissingException("No schema found in file for plugin node: " + nodeId, List.of(nodeId));
        }
        return new JSONObject(schema);
    }

    /**
     * Invalidate a built-in schema entry from cache.
     *
     * @param name the built-in schema name
     */
    public void invalidateBuiltinSchema(String name) {
        builtinCache.invalidate(name);
    }

    /**
     * Invalidate a plugin schema entry from cache using node ID.
     *
     * @param nodeId the plugin node UUID
     */
    public void invalidatePluginSchema(String nodeId) {
        pluginCache.invalidate(nodeId);
    }

    /**
     * Invalidate a plugin schema entry from cache.
     *
     * @deprecated Use invalidatePluginSchema(String nodeId) instead
     * @param identifier the plugin node identifier
     */
    @Deprecated
    public void invalidatePluginSchema(PluginNodeIdentifier identifier) {
        String key = identifier.getNodeId() != null ? identifier.getNodeId().toString() : identifier.toCacheKey();
        pluginCache.invalidate(key);
    }

    /**
     * Invalidate a schema entry using the template string format used in {@link #getSchemaByTemplateString}.
     *
     * @param templateString schema identifier, either built-in or plugin UUID
     */
    public void invalidateByTemplateString(String templateString) {
        if (templateString.startsWith("builtin:")) {
            invalidateBuiltinSchema(templateString.substring(8));
            return;
        }
        // Treat as plugin node UUID
        invalidatePluginSchema(templateString);
    }

    /**
     * Expose cache statistics for debugging and metrics.
     */
    public CacheStats builtinCacheStats() {
        return builtinCache.stats();
    }

    public CacheStats pluginCacheStats() {
        return pluginCache.stats();
    }

    private JSONObject loadBuiltinSchemaFromFile(String name) {
        String path = BUILTIN_PATH + name + ".json";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new NodeSchemaException("Schema file not found: " + path);
            }
            String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return new JSONObject(content);
        } catch (Exception e) {
            throw new NodeSchemaException("Failed to load schema: " + path, e);
        }
    }
}
