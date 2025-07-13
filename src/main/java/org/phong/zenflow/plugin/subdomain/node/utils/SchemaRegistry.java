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

    // Plugin node schema: key = "plugin:pluginName:nodeName"
    public JSONObject getPluginSchema(String plugin, String node) {
        String key = plugin + ":" + node;
        return pluginCache.computeIfAbsent(key, k -> {
            String schemaJson = pluginProvider.getSchemaJson(plugin, node);
            if (schemaJson == null) {
                throw new RuntimeException("No schema found for plugin node: " + plugin + ":" + node);
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
