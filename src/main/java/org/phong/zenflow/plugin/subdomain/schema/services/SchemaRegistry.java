package org.phong.zenflow.plugin.subdomain.schema.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaException;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaMissingException;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemaRegistry {

    private static final String BUILTIN_PATH = "/builtin_schemas/";

    private final PluginNodeSchemaProvider pluginProvider;

    private final Map<String, JSONObject> builtinCache = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> pluginCache = new ConcurrentHashMap<>();

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
        return builtinCache.computeIfAbsent(name, this::loadBuiltinSchemaFromFile);
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
        String cacheKey = identifier.toCacheKey();
        return pluginCache.computeIfAbsent(cacheKey, k -> {
            Map<String, Object> schema = pluginProvider.getSchemaJson(k);
            if (schema.isEmpty()) {
                throw new NodeSchemaMissingException("No schema found for plugin node: " + k, List.of(k));
            }
            return new JSONObject(schema);
        });
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
                .filter(id -> !pluginCache.containsKey(id.toCacheKey()))
                .toList();

        // Fetch and cache missing schemas
        putNewSchemasForUncachedIdentifiers(uncachedIdentifiers);

        // Fail if any requested identifier is still missing
        List<String> missingIds = identifiers.stream()
                .map(PluginNodeIdentifier::toCacheKey)
                .filter(key -> !pluginCache.containsKey(key))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NodeSchemaMissingException("Schemas missing for plugin node identifiers", missingIds);
        }

        // All schemas are guaranteed to be present
        return identifiers.stream()
                .collect(Collectors.toMap(
                        PluginNodeIdentifier::toCacheKey,
                        id -> pluginCache.get(id.toCacheKey())
                ));
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
