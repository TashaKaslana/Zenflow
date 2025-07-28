package org.phong.zenflow.plugin.subdomain.schema.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaException;
import org.phong.zenflow.plugin.subdomain.schema.exception.NodeSchemaMissingException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
     *   <li>Plugin: <code>&#60;nodeId&#62;</code> (e.g., <code>123e4567-e89b-12d3-a456-426614174001</code>)</li>
     * </ul>
     * This unified naming convention allows easy differentiation and retrieval of schemas.
     *
     * @param templateString the schema identifier, either built-in name or plugin nodeId
     * @return JSONObject containing the schema
     */
    public JSONObject getSchemaByTemplateString(String templateString) {
        // Check if the schema is a built-in one
        if (templateString.startsWith("builtin:")) {
            return getBuiltinSchema(templateString.substring(8));
        }

        // Otherwise, treat it as a plugin schema by nodeId
        try {
            UUID nodeId = UUID.fromString(templateString);
            return getPluginSchema(nodeId);
        } catch (IllegalArgumentException e) {
            throw new NodeSchemaException("Invalid schema identifier format. Expected 'builtin:name' or valid UUID for nodeId.", e);
        }
    }

    public Map<String, JSONObject> getSchemaMapByTemplateStrings(Set<String> templateStrings) {
        Map<String, JSONObject> result = new HashMap<>();

        List<String> builtinNames = templateStrings.stream()
                .filter(name -> name.startsWith("builtin:"))
                .map(name -> name.substring(8))
                .toList();
        Set<UUID> pluginNodeIds = templateStrings.stream()
                .filter(name -> !name.startsWith("builtin:"))
                .map(name -> {
                    try {
                        return UUID.fromString(name);
                    } catch (IllegalArgumentException e) {
                        throw new NodeSchemaException("Invalid nodeId format: " + name, e);
                    }
                }).collect(Collectors.toSet());

        if (!builtinNames.isEmpty()) {
            Map<String, JSONObject> builtinSchemas = getBuiltinSchemas(builtinNames);
            result.putAll(builtinSchemas);
        }

        if (!pluginNodeIds.isEmpty()) {
            Map<String, JSONObject> pluginSchemas = getPluginSchemasByNodeIds(pluginNodeIds);
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
     * Get plugin node schema using nodeId only
     * @param nodeId UUID of the node
     * @return JSONObject containing the schema
     */
    private JSONObject getPluginSchema(UUID nodeId) {
        String cacheKey = nodeId.toString();
        return pluginCache.computeIfAbsent(cacheKey, k -> {
            Map<String, Object> schema = pluginProvider.getSchemaJson(nodeId);
            if (schema.isEmpty()) {
                throw new NodeSchemaMissingException("No schema found for node id: " + nodeId, List.of(nodeId));
            }
            return new JSONObject(schema);
        });
    }

    /**
     * Efficiently retrieve multiple plugin schemas by nodeIds in a single batch operation.
     * Fails if any nodeId has no corresponding schema.
     *
     * @param nodeIds Set of node UUIDs
     * @return Map of nodeId (as String) -> schema JSONObject
     */
    private Map<String, JSONObject> getPluginSchemasByNodeIds(Set<UUID> nodeIds) {
        // Filter out already cached schemas
        List<UUID> uncachedNodeIds = nodeIds.stream()
                .filter(nodeId -> !pluginCache.containsKey(nodeId.toString()))
                .toList();

        // Fetch and cache missing schemas
        putNewSchemasForUnCachedNode(uncachedNodeIds);

        // Fail if any requested nodeId is still missing
        List<UUID> missingIds = nodeIds.stream()
                .filter(nodeId -> !pluginCache.containsKey(nodeId.toString()))
                .toList();

        if (!missingIds.isEmpty()) {
            throw new NodeSchemaMissingException("Schemas missing for plugin nodeIds", missingIds);
        }

        // All schemas are guaranteed to be present
        return nodeIds.stream()
                .collect(Collectors.toMap(
                        UUID::toString,
                        nodeId -> pluginCache.get(nodeId.toString())
                ));
    }

    private void putNewSchemasForUnCachedNode(List<UUID> uncachedNodeIds) {
        if (!uncachedNodeIds.isEmpty()) {
            Map<UUID, Map<String, Object>> schemas = pluginProvider.getAllSchemasByNodeIds(uncachedNodeIds);

            schemas.forEach((uuid, schemaMap) -> {
                if (schemaMap == null) {
                    throw new NodeSchemaException("Null schema for plugin node ID: " + uuid);
                }
                pluginCache.put(uuid.toString(), new JSONObject(schemaMap));
            });
        }
    }

    /**
     * Convenience method to get schemas by UUID nodeIds and return with UUID keys
     * @param nodeIds List of node UUIDs
     * @return Map of UUID -> schema JSONObject
     */
    public Map<UUID, JSONObject> getPluginSchemasByNodeIdsAsUUID(Set<UUID> nodeIds) {
        Map<String, JSONObject> stringKeyMap = getPluginSchemasByNodeIds(nodeIds);
        return stringKeyMap.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> UUID.fromString(entry.getKey()),
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
