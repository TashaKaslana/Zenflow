package org.phong.zenflow.plugin.subdomain.schema.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.core.utils.LoadSchemaHelper;
import org.phong.zenflow.plugin.services.PluginService;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaException;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaMissingException;
import org.phong.zenflow.plugin.subdomain.schema.registry.SchemaIndexRegistry;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SchemaRegistry {

    private static final String BUILTIN_PATH = "/builtin_schemas/";

    private final PluginNodeSchemaProvider pluginProvider;
    private final PluginService pluginService;
    private final SchemaIndexRegistry schemaIndexRegistry;

    private final Cache<String, JSONObject> builtinCache;
    private final Cache<String, JSONObject> pluginCache;
    private final Cache<String, JSONObject> pluginSchemaCache;

    // Performance optimization: use file-based loading by default
    private final boolean useFileBasedLoading;

    public SchemaRegistry(
            PluginNodeSchemaProvider pluginProvider,
            PluginService pluginService,
            SchemaIndexRegistry schemaIndexRegistry,
            @Value("${zenflow.schema.cache-ttl-seconds:3600}") long cacheTtlSeconds,
            @Value("${zenflow.schema.use-file-based-loading:true}") boolean useFileBasedLoading) {
        this.pluginProvider = pluginProvider;
        this.pluginService = pluginService;
        this.schemaIndexRegistry = schemaIndexRegistry;
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
        this.pluginSchemaCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
    }

    /**
     * Retrieves a schema by template string, supporting built-in, plugin, and plugin node schemas.
     * <p>
     * Template string formats:
     * <ul>
     *   <li>Built-in: <code>builtin:&lt;name&gt;</code> (e.g., <code>builtin:http-trigger</code>)</li>
     *   <li>Plugin: <code>plugin:&lt;id&gt;</code> (e.g., <code>plugin:123e4567-e89b-12d3-a456-426614174000</code>)</li>
     *   <li>Plugin Node: <code>&lt;nodeId&gt;</code> (UUID string)</li>
     * </ul>
     * This unified naming convention allows easy differentiation and retrieval of schemas.
     *
     * @param templateString the schema identifier, either built-in name, plugin ID, or plugin node UUID
     * @return JSONObject containing the schema
     */
    public JSONObject getSchemaByTemplateString(String templateString) {
        // Check if the schema is a built-in one
        if (templateString.startsWith("builtin:")) {
            return getBuiltinSchema(templateString.substring(8));
        }

        // Check if the schema is a plugin-level schema
        if (templateString.startsWith("plugin:")) {
            return getPluginLevelSchema(templateString.substring(7));
        }

        // Otherwise, treat it as a plugin node schema with UUID
        return getPluginSchema(templateString);
    }

    public Map<String, JSONObject> getSchemaMapByTemplateStrings(Set<String> templateStrings) {
        Map<String, JSONObject> result = new HashMap<>();

        List<String> builtinNames = templateStrings.stream()
                .filter(name -> name.startsWith("builtin:"))
                .map(name -> name.substring(8))
                .toList();

        List<String> pluginIds = templateStrings.stream()
                .filter(name -> name.startsWith("plugin:"))
                .map(name -> name.substring(7))
                .toList();

        Set<String> pluginNodeIds = templateStrings.stream()
                .filter(name -> !name.startsWith("builtin:") && !name.startsWith("plugin:"))
                .collect(Collectors.toSet());

        if (!builtinNames.isEmpty()) {
            Map<String, JSONObject> builtinSchemas = getBuiltinSchemas(builtinNames);
            for (String name : builtinNames) {
                result.put("builtin:" + name, builtinSchemas.get(name));
            }
        }

        if (!pluginIds.isEmpty()) {
            for (String pluginId : pluginIds) {
                result.put("plugin:" + pluginId, getPluginLevelSchema(pluginId));
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
     * Get plugin node schema using PluginNodeIdentifier.
     * It first attempts to load from the file-based index, and falls back to the database if not found.
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

        Map<String, Object> schema = new HashMap<>();

        // Try loading from file-based index first
        SchemaIndexRegistry.SchemaLocation location = schemaIndexRegistry.getSchemaLocation(id);
        if (location != null) {
            try {
                schema = LoadSchemaHelper.loadSchema(location.clazz(), location.schemaPath(), "schema.json");
                log.debug("Loaded schema from file index for {}", id);
            } catch (Exception e) {
                log.warn("Failed to load schema from file index for {}: {}. Falling back to database.", id, e.getMessage());
            }
        }

        // Fallback to database if not found in index or failed to load
        if (schema.isEmpty()) {
            log.debug("Schema for {} not found in file index or failed to load, falling back to database.", id);
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
     * Get plugin-level schema by plugin ID
     * @param id The plugin ID (as string UUID)
     * @return JSONObject containing the plugin schema
     */
    private JSONObject getPluginLevelSchema(String id) {
        JSONObject cached = pluginSchemaCache.getIfPresent(id);
        if (cached != null) {
            log.debug("Plugin-level schema cache hit for {}", id);
            return cached;
        }
        log.debug("Plugin-level schema cache miss for {}", id);

        Map<String, Object> schema = new HashMap<>();

        // 1. Try loading from file-based index first
        SchemaIndexRegistry.SchemaLocation location = schemaIndexRegistry.getSchemaLocation(id);
        if (location != null) {
            try {
                schema = LoadSchemaHelper.loadSchema(location.clazz(), location.schemaPath(), "schema.json");
                log.debug("Loaded plugin-level schema from file index for {}", id);
            } catch (Exception e) {
                log.warn("Failed to load plugin-level schema from file index for {}: {}. Falling back to database.", id, e.getMessage());
            }
        }

        // 2. Fallback to database if not found in index or failed to load
        if (schema.isEmpty()) {
            log.debug("Plugin-level schema for {} not found in file index, falling back to database.", id);
            try {
                UUID pluginId = UUID.fromString(id);
                schema = pluginService.findPluginById(pluginId).getPluginSchema();
            } catch (IllegalArgumentException e) {
                throw new NodeSchemaException("Invalid UUID format for plugin ID: " + id, e);
            }
        }

        if (schema == null || schema.isEmpty()) {
            throw new NodeSchemaMissingException("No schema found for plugin: " + id, List.of(id));
        }

        JSONObject json = new JSONObject(schema);
        pluginSchemaCache.put(id, json);
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
                            JSONObject val = pluginCache.getIfPresent(id);
                            if (val != null) {
                                m.put(id, val);
                            }
                        },
                        Map::putAll
                );
    }

    private void putNewSchemasForUncachedIdentifiers(Set<String> uncachedIdentifiers) {
        if (uncachedIdentifiers.isEmpty()) {
            return;
        }

        Set<String> identifiersForDbFetch = new HashSet<>();

        // 1. Try to load from file index
        for (String id : uncachedIdentifiers) {
            SchemaIndexRegistry.SchemaLocation location = schemaIndexRegistry.getSchemaLocation(id);
            if (location != null) {
                try {
                    Map<String, Object> schema = LoadSchemaHelper.loadSchema(location.clazz(), location.schemaPath(), "schema.json");
                    if (!schema.isEmpty()) {
                        pluginCache.put(id, new JSONObject(schema));
                    } else {
                        // Schema file is empty, fallback to DB
                        identifiersForDbFetch.add(id);
                    }
                } catch (Exception e) {
                    log.warn("Failed to load schema from file index for {}: {}. Will try database.", id, e.getMessage());
                    identifiersForDbFetch.add(id);
                }
            } else {
                // Not in index, must fetch from DB
                identifiersForDbFetch.add(id);
            }
        }

        // 2. Fallback to database for remaining identifiers
        if (!identifiersForDbFetch.isEmpty()) {
            log.debug("Fetching {} schemas from database as fallback.", identifiersForDbFetch.size());
            Map<String, Map<String, Object>> schemas = pluginProvider.getAllSchemasByIdentifiers(identifiersForDbFetch);
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
        SchemaIndexRegistry.SchemaLocation location = schemaIndexRegistry.getSchemaLocation(nodeId);
        if (location != null) {
            try {
                Map<String, Object> schema = LoadSchemaHelper.loadSchema(
                        location.clazz(), location.schemaPath(), "schema.json"
                );
                if (schema.isEmpty()) {
                    throw new NodeSchemaMissingException("Empty schema file for plugin node: " + nodeId, List.of(nodeId));
                }
                return new JSONObject(schema);
            } catch (Exception e) {
                throw new NodeSchemaException("Failed to load schema from file index for " + nodeId, e);
            }
        } else {
            throw new NodeSchemaMissingException("No schema location found in index for plugin node: " + nodeId, List.of(nodeId));
        }
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
     * Invalidate a plugin-level schema entry from cache using plugin ID.
     *
     * @param pluginId the plugin UUID
     */
    public void invalidatePluginLevelSchema(String pluginId) {
        pluginSchemaCache.invalidate(pluginId);
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
     * @param templateString schema identifier, either built-in, plugin-level, or plugin node UUID
     */
    public void invalidateByTemplateString(String templateString) {
        if (templateString.startsWith("builtin:")) {
            invalidateBuiltinSchema(templateString.substring(8));
            return;
        }
        if (templateString.startsWith("plugin:")) {
            invalidatePluginLevelSchema(templateString.substring(7));
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

    public CacheStats pluginSchemaCacheStats() {
        return pluginSchemaCache.stats();
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