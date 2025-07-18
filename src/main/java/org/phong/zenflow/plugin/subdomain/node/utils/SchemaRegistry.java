package org.phong.zenflow.plugin.subdomain.node.utils;

import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.phong.zenflow.plugin.subdomain.node.interfaces.PluginNodeSchemaProvider;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchemaRegistry {

    private static final String BUILTIN_PATH = "/builtin_schemas/";

    private final PluginNodeSchemaProvider pluginProvider;

    private final Map<String, JSONObject> builtinCache = new ConcurrentHashMap<>();
    private final Map<String, JSONObject> pluginCache = new ConcurrentHashMap<>();

    // Built-in node schema: key = "http-trigger"
    public JSONObject getBuiltinSchema(String name) {
        return builtinCache.computeIfAbsent(name, this::loadBuiltinSchemaFromFile);
    }

    /**
     * Get plugin node schema using UUID for both plugin and node identifiers
     * @param pluginId UUID of the plugin
     * @param nodeId UUID of the node
     * @return JSONObject containing the schema
     */
    public JSONObject getPluginSchema(UUID pluginId, UUID nodeId) {
        String cacheKey = pluginId.toString() + ":" + nodeId.toString();
        return pluginCache.computeIfAbsent(cacheKey, k -> {
            String schemaJson = pluginProvider.getSchemaJson(pluginId, nodeId);
            if (schemaJson == null) {
                throw new RuntimeException("No schema found for plugin node: " + pluginId + " with node id: " + nodeId);
            }
            return new JSONObject(schemaJson);
        });
    }

    private JSONObject loadBuiltinSchemaFromFile(String name) {
        String path = BUILTIN_PATH + name + ".json";
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Schema file not found: " + path);
            }
            String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return new JSONObject(content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load schema: " + path, e);
        }
    }
}
