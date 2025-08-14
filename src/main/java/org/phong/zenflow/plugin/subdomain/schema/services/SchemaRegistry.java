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

    public SchemaRegistry(
            PluginNodeSchemaProvider pluginProvider,
            @Value("${zenflow.schema.cache-ttl-seconds:3600}") long cacheTtlSeconds) {
        this.pluginProvider = pluginProvider;
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
     *   <li>Plugin: <code>&#60;pluginKey&#62;:&#60;nodeKey&#62;</code> (e.g., <code>123e4567-e89b-12d3-a456-426614174001:1</code>)</li>
     * </ul>
     * This unified naming convention allows easy differentiation and retrieval of schemas.
     *
     * @param templateString the schema identifier, either built-in name or plugin identifier
     * @return JSONObject containing the schema
     */
    public JSONObject getSchemaByTemplateString(String templateString) {
        // Check if the schema is a built-in one
        if (templateString.startsWith("builtin:")) {
            return getBuiltinSchema(templateString.substring(8));
        }

        // Otherwise, treat it as a plugin schema
        try {
            PluginNodeIdentifier pni = PluginNodeIdentifier.fromString(templateString);
            return getPluginSchema(pni);
        } catch (IllegalArgumentException e) {
            throw new NodeSchemaException("Invalid schema identifier format. Expected 'builtin:name' or 'pluginKey:nodeKey'.", e);
        }
    }

    public Map<String, JSONObject> getSchemaMapByTemplateStrings(Set<String> templateStrings) {
        Map<String, JSONObject> result = new HashMap<>();

        List<String> builtinNames = templateStrings.stream()
                .filter(name -> name.startsWith("builtin:"))
                .map(name -> name.substring(8))
                .toList();
        Set<PluginNodeIdentifier> pluginNodeIdentifiers = templateStrings.stream()
                .filter(name -> !name.startsWith("builtin:"))
                .map(name -> {
                    try {
                        return PluginNodeIdentifier.fromString(name);
                    } catch (IllegalArgumentException e) {
                        throw new NodeSchemaException("Invalid plugin node identifier format: " + name, e);
                    }
                }).collect(Collectors.toSet());

        if (!builtinNames.isEmpty()) {
            Map<String, JSONObject> builtinSchemas = getBuiltinSchemas(builtinNames);
            for (String name : builtinNames) {
                result.put("builtin:" + name, builtinSchemas.get(name));
            }
        }

        if (!pluginNodeIdentifiers.isEmpty()) {
            Map<String, JSONObject> pluginSchemas = getPluginSchemasByIdentifiers(pluginNodeIdentifiers);
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
     * @param identifier The plugin node identifier
     * @return JSONObject containing the schema
     */
    private JSONObject getPluginSchema(PluginNodeIdentifier identifier) {
        String key = identifier.toCacheKey();
        JSONObject cached = pluginCache.getIfPresent(key);
        if (cached != null) {
            log.debug("Plugin schema cache hit for {}", key);
            return cached;
        }
        log.debug("Plugin schema cache miss for {}", key);
        Map<String, Object> schema = pluginProvider.getSchemaJson(identifier);
        if (schema.isEmpty()) {
            throw new NodeSchemaMissingException("No schema found for plugin node: " + key, List.of(key));
        }
        JSONObject json = new JSONObject(schema);
        pluginCache.put(key, json);
        return json;
    }

    /**
     * Efficiently retrieve multiple plugin schemas by identifiers in a single batch operation.
     * Fails if any identifier has no corresponding schema.
     *
     * @param identifiers Set of plugin node identifiers
     * @return Map of identifier (as String) -> schema JSONObject
     */
    private Map<String, JSONObject> getPluginSchemasByIdentifiers(Set<PluginNodeIdentifier> identifiers) {

        // Filter out already cached schemas
        List<PluginNodeIdentifier> uncachedIdentifiers = identifiers.stream()
                .filter(id -> pluginCache.getIfPresent(id.toCacheKey()) == null)
                .toList();

        // Fetch and cache missing schemas
        putNewSchemasForUncachedIdentifiers(uncachedIdentifiers);

        // Fail if any requested identifier is still missing
        List<String> missingIds = identifiers.stream()
                .map(PluginNodeIdentifier::toCacheKey)
                .filter(key -> pluginCache.getIfPresent(key) == null)
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NodeSchemaMissingException("Schemas missing for plugin node identifiers", missingIds);
        }

        return identifiers.stream()
                .collect(
                        HashMap::new,
                        (m, id) -> {
                            String key = id.toCacheKey();
                            JSONObject val = pluginCache.getIfPresent(key);
                            if (val != null) {
                                m.put(key, val);
                            }
                        },
                        Map::putAll
                );
    }

    private void putNewSchemasForUncachedIdentifiers(List<PluginNodeIdentifier> uncachedIdentifiers) {
        if (!uncachedIdentifiers.isEmpty()) {
            Map<String, Map<String, Object>> schemas = pluginProvider.getAllSchemasByIdentifiers(uncachedIdentifiers);

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
     * @param identifiers Set of plugin node identifiers
     * @return Map of PluginNodeIdentifier -> schema JSONObject
     */
    public Map<PluginNodeIdentifier, JSONObject> getPluginSchemasByIdentifiersAsIdentifier(Set<PluginNodeIdentifier> identifiers) {
        Map<String, JSONObject> stringKeyMap = getPluginSchemasByIdentifiers(identifiers);
        return stringKeyMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> PluginNodeIdentifier.fromString(entry.getKey()),
                        Map.Entry::getValue
                ));
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
     * Invalidate a plugin schema entry from cache.
     *
     * @param identifier the plugin node identifier
     */
    public void invalidatePluginSchema(PluginNodeIdentifier identifier) {
        pluginCache.invalidate(identifier.toCacheKey());
    }

    /**
     * Invalidate a schema entry using the template string format used in {@link #getSchemaByTemplateString}.
     *
     * @param templateString schema identifier, either built-in or plugin format
     */
    public void invalidateByTemplateString(String templateString) {
        if (templateString.startsWith("builtin:")) {
            invalidateBuiltinSchema(templateString.substring(8));
            return;
        }
        try {
            PluginNodeIdentifier pni = PluginNodeIdentifier.fromString(templateString);
            invalidatePluginSchema(pni);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid schema identifier format for invalidate: {}", templateString, e);
        }
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
